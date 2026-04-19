package com.sythiex.hearthstonemod.content;

import java.util.List;
import java.util.Optional;

import com.sythiex.hearthstonemod.HearthstoneCasting;
import com.sythiex.hearthstonemod.HearthstoneClientBridge;
import com.sythiex.hearthstonemod.HearthstoneCommon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Main shared item implementation for the Hearthstone.
 *
 * Persistent stack state stores the linked home and cooldown timestamp, while
 * active channeling is managed separately by {@link HearthstoneCasting}.
 */
public class ItemHearthstone extends Item
{
    private static final int COOLDOWN_BAR_COLOR = 0xFF0000;
    private static final int CAST_BAR_COLOR = 0x4BC1E3;

    private static final String TEXT_ON_COOLDOWN = "item.hearthstone.on_cooldown";
    private static final String TEXT_NO_HOME = "item.hearthstone.no_home";
    private static final String TEXT_MISSING_BED = "item.hearthstone.missing_bed";
    private static final String TEXT_MISSING_RESPAWN_ANCHOR = "item.hearthstone.missing_respawn_anchor";
    private static final String TEXT_EMPTY_RESPAWN_ANCHOR = "item.hearthstone.empty_respawn_anchor";
    private static final String TEXT_LINKED = "item.hearthstone.linked";
    private static final String TEXT_DIMENSION_BLACKLISTED = "item.hearthstone.dimension_blacklisted";
    private static final String TEXT_CROSS_DIMENSION_DISABLED = "item.hearthstone.cross_dimension_disabled";
    private static final String TEXT_RESPAWN_ANCHOR_NO_CHARGE = "item.hearthstone.respawn_anchor_no_charge";
    private static final String TEXT_RESPAWN_ANCHOR_DISABLED = "item.hearthstone.respawn_anchor_disabled";
    private static final String TEXT_COOLDOWN = "item.hearthstone.cooldown";
    private static final String TEXT_MINUTES = "item.hearthstone.minutes";
    private static final String TEXT_SECONDS = "item.hearthstone.seconds";
    private static final String TEXT_HOME_SET = "item.hearthstone.home_set";

    public ItemHearthstone(Properties properties)
    {
        super(properties);
    }

    /**
     * Main-hand use starts channeling when the item is linked and not on cooldown.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND)
        {
            return new InteractionResultHolder<>(InteractionResult.PASS, stack);
        }

        if (!level.isClientSide && !player.isCrouching())
        {
            HearthstoneItemData data = getData(stack);
            if (HearthstoneCommon.isDimensionBlacklisted(level))
            {
                player.displayClientMessage(translatable(TEXT_DIMENSION_BLACKLISTED), true);
            } else if (!isLinkedHome(data))
            {
                player.displayClientMessage(translatable(TEXT_NO_HOME), true);
            } else if (isRespawnAnchorLinkingDisabled(data))
            {
                player.displayClientMessage(translatable(TEXT_RESPAWN_ANCHOR_DISABLED), true);
            } else if (isCrossDimensionTravelBlocked(level, data))
            {
                player.displayClientMessage(translatable(TEXT_CROSS_DIMENSION_DISABLED), true);
            } else if (getCooldown(level, data) == 0)
            {
                if (player instanceof ServerPlayer serverPlayer)
                {
                    HearthstoneCasting.startCasting(serverPlayer);
                }
            } else
            {
                player.displayClientMessage(translatable(TEXT_ON_COOLDOWN), true);
            }
        }

        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }

    /**
     * Sneak-using on a bed or respawn anchor links the stack to that block.
     */
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player == null)
        {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (level.isClientSide())
        {
            return InteractionResult.FAIL;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!HearthstoneContentIds.isHearthstone(stack))
        {
            return InteractionResult.FAIL;
        }

        if (player.isCrouching())
        {
            BlockPos clickedPos = context.getClickedPos();
            BlockState state = level.getBlockState(clickedPos);
            if (state.is(BlockTags.BEDS))
            {
                if (HearthstoneCommon.isDimensionBlacklisted(level))
                {
                    player.displayClientMessage(translatable(TEXT_DIMENSION_BLACKLISTED), true);
                    return InteractionResult.SUCCESS;
                }

                linkHome(stack, clickedPos, level.dimension().location(), HearthstoneLinkType.BED);
                player.displayClientMessage(translatable(TEXT_LINKED), true);
            } else if (isRespawnAnchor(state))
            {
                if (HearthstoneCommon.isDimensionBlacklisted(level))
                {
                    player.displayClientMessage(translatable(TEXT_DIMENSION_BLACKLISTED), true);
                    return InteractionResult.SUCCESS;
                }

                if (!HearthstoneCommon.config().allowRespawnAnchorLinking())
                {
                    player.displayClientMessage(translatable(TEXT_RESPAWN_ANCHOR_DISABLED), true);
                    return InteractionResult.SUCCESS;
                }

                if (getRespawnAnchorCharge(state) == 0)
                {
                    player.displayClientMessage(translatable(TEXT_RESPAWN_ANCHOR_NO_CHARGE), true);
                    return InteractionResult.SUCCESS;
                }

                linkHome(stack, clickedPos, level.dimension().location(), HearthstoneLinkType.RESPAWN_ANCHOR);
                player.displayClientMessage(translatable(TEXT_LINKED), true);
            }

            return InteractionResult.SUCCESS;
        }

        return this.use(level, player, InteractionHand.MAIN_HAND).getResult();
    }

    @Override
    public boolean isBarVisible(ItemStack stack)
    {
        HearthstoneClientBridge clientBridge = HearthstoneCommon.clientBridge();
        return getCooldown(clientBridge.clientLevel(), getData(stack)) > 0 || clientBridge.isLocalStackCasting(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack)
    {
        HearthstoneClientBridge clientBridge = HearthstoneCommon.clientBridge();
        int cooldown = getCooldown(clientBridge.clientLevel(), getData(stack));
        if (cooldown > 0)
        {
            int maxCooldown = Math.max(1, HearthstoneCommon.config().cooldown());
            return Math.round(13.0F - (float) cooldown * 13.0F / (float) maxCooldown);
        }

        int castTime = clientBridge.getLocalCastTime(stack);
        if (castTime > 0)
        {
            int channelTime = Math.max(1, HearthstoneCommon.config().channelTime());
            return Math.round(13.0F - ((float) channelTime - (float) castTime) * 13.0F / (float) channelTime);
        }

        return 0;
    }

    @Override
    public int getBarColor(ItemStack stack)
    {
        return getCooldown(HearthstoneCommon.clientBridge().clientLevel(), getData(stack)) > 0 ? COOLDOWN_BAR_COLOR : CAST_BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag)
    {
        HearthstoneItemData data = getData(stack);
        if (isLinkedHome(data))
        {
            tooltip.add(translatable(TEXT_HOME_SET).withStyle(ChatFormatting.GRAY));
        }

        int cooldown = getCooldown(HearthstoneCommon.clientBridge().clientLevel(), data);
        if (cooldown != 0)
        {
            cooldown += 19;
            int minutes = cooldown / 1200;
            int seconds = cooldown / 20 - minutes * 60;
            tooltip.add(buildCooldownText(minutes, seconds).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack)
    {
        return HearthstoneCommon.clientBridge().isLocalStackCasting(stack);
    }

    /**
     * Resolves the stored destination and finishes a completed server-side cast.
     */
    public static void finishCast(ServerPlayer player, ItemStack stack)
    {
        HearthstoneItemData data = getData(stack);
        Optional<HearthstoneHomeLink> homeLink = data.homeLink();
        if (homeLink.isEmpty())
        {
            return;
        }

        if (isRespawnAnchorLinkingDisabled(data))
        {
            player.displayClientMessage(translatable(TEXT_RESPAWN_ANCHOR_DISABLED), true);
            return;
        }

        ServerLevel destinationLevel = player.serverLevel();
        HearthstoneHomeLink link = homeLink.get();

        playPlayerSound(destinationLevel, player, HearthstoneContentIds.HEARTHSTONE_CAST_SOUND);

        if (!player.level().dimension().location().equals(link.dimensionId()))
        {
            // Cross-dimension teleports are resolved before destination safety checks run.
            if (!HearthstoneCommon.config().allowCrossDimensionTravel())
            {
                player.displayClientMessage(translatable(TEXT_CROSS_DIMENSION_DISABLED), true);
                return;
            }

            MinecraftServer server = player.getServer();
            if (server != null)
            {
                ServerLevel linkedLevel = server.getLevel(link.dimensionKey());
                if (linkedLevel != null)
                {
                    BlockPos homePos = link.homePos();
                    player.teleportTo(linkedLevel, homePos.getX() + 0.5D, homePos.getY() + 1.0D, homePos.getZ() + 0.5D, player.getYRot(), player.getXRot());
                    destinationLevel = linkedLevel;
                }
            }
        }

        BlockPos homePos = link.homePos();
        BlockState state = destinationLevel.getBlockState(homePos);
        if (link.linkType() == HearthstoneLinkType.RESPAWN_ANCHOR)
        {
            finishRespawnAnchorCast(player, destinationLevel, stack, homePos, state);
        } else
        {
            finishBedCast(player, destinationLevel, stack, homePos, state);
        }
    }

    private static HearthstoneItemData getData(ItemStack stack)
    {
        HearthstoneItemData data = stack.get(HearthstoneDataComponents.hearthstoneData());
        return data == null ? HearthstoneItemData.DEFAULT : data;
    }

    private static void setData(ItemStack stack, HearthstoneItemData data)
    {
        stack.set(HearthstoneDataComponents.hearthstoneData(), data);
    }

    private static void linkHome(ItemStack stack, BlockPos homePos, ResourceLocation dimensionId, HearthstoneLinkType linkType)
    {
        setData(stack, getData(stack).withHomeLink(new HearthstoneHomeLink(homePos.immutable(), dimensionId, linkType)));
    }

    private static int getCooldown(Level level, HearthstoneItemData data)
    {
        if (level == null || data.lastUsedGameTime() < 0L)
        {
            return 0;
        }

        long elapsedTime = level.getGameTime() - data.lastUsedGameTime();
        long remainingCooldown = (long) HearthstoneCommon.config().cooldown() - elapsedTime;
        return (int) Math.max(0L, remainingCooldown);
    }

    private static boolean isLinkedHome(HearthstoneItemData data)
    {
        return data.homeLink().isPresent();
    }

    private static boolean isCrossDimensionTravelBlocked(Level level, HearthstoneItemData data)
    {
        if (HearthstoneCommon.config().allowCrossDimensionTravel() || data.homeLink().isEmpty())
        {
            return false;
        }

        return !data.homeLink().get().dimensionId().equals(level.dimension().location());
    }

    private static boolean isRespawnAnchorLinkingDisabled(HearthstoneItemData data)
    {
        return data.homeLink().map(HearthstoneHomeLink::linkType).orElse(HearthstoneLinkType.BED) == HearthstoneLinkType.RESPAWN_ANCHOR
                && !HearthstoneCommon.config().allowRespawnAnchorLinking();
    }

    private static boolean isRespawnAnchor(BlockState state)
    {
        return state.is(Blocks.RESPAWN_ANCHOR);
    }

    private static int getRespawnAnchorCharge(BlockState state)
    {
        return state.getValue(RespawnAnchorBlock.CHARGE);
    }

    /**
     * Bed links try the vanilla stand-up position first and fall back to the block
     * top if needed.
     */
    private static void finishBedCast(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos homePos, BlockState state)
    {
        if (state.is(BlockTags.BEDS))
        {
            Optional<Vec3> respawnPosition = BedBlock.findStandUpPosition(EntityType.PLAYER, level, homePos, state.getValue(BedBlock.FACING), player.getYRot());
            teleportToRespawnPosition(player, respawnPosition, homePos);
            completeTeleport(player, level, stack);
            return;
        }

        breakLinkedHome(player, level, stack, homePos, TEXT_MISSING_BED);
    }

    /**
     * Respawn anchor links validate the block, charge, and optional charge
     * consumption.
     */
    private static void finishRespawnAnchorCast(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos homePos, BlockState state)
    {
        if (!isRespawnAnchor(state))
        {
            breakLinkedHome(player, level, stack, homePos, TEXT_MISSING_RESPAWN_ANCHOR);
            return;
        }

        int charge = getRespawnAnchorCharge(state);
        if (charge == 0)
        {
            breakLinkedHome(player, level, stack, homePos, TEXT_EMPTY_RESPAWN_ANCHOR);
            return;
        }

        Optional<Vec3> respawnPosition = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, level, homePos);
        teleportToRespawnPosition(player, respawnPosition, homePos);
        completeTeleport(player, level, stack);

        if (HearthstoneCommon.config().consumeRespawnAnchorCharge())
        {
            int remainingCharge = charge - 1;
            level.setBlock(homePos, state.setValue(RespawnAnchorBlock.CHARGE, remainingCharge), 3);
            level.playSound(null, homePos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (remainingCharge == 0)
            {
                clearLinkedHome(stack);
                player.displayClientMessage(translatable(TEXT_EMPTY_RESPAWN_ANCHOR), true);
            }
        }
    }

    private static void teleportToRespawnPosition(ServerPlayer player, Optional<Vec3> respawnPosition, BlockPos homePos)
    {
        if (respawnPosition.isPresent())
        {
            Vec3 safePosition = respawnPosition.get();
            player.teleportTo(safePosition.x, safePosition.y, safePosition.z);
        } else
        {
            // If the linked block no longer has a valid stand-up spot, use the simple
            // block-top fallback.
            teleportToHomeTop(player, homePos);
        }
    }

    /**
     * Handles broken or depleted links by teleporting once, clearing the link, and
     * warning the player.
     */
    private static void breakLinkedHome(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos homePos, String messageKey)
    {
        teleportToHomeTop(player, homePos);
        completeTeleport(player, level, stack);
        clearLinkedHome(stack);
        player.displayClientMessage(translatable(messageKey), true);
    }

    private static void teleportToHomeTop(ServerPlayer player, BlockPos homePos)
    {
        player.teleportTo(homePos.getX() + 0.5D, homePos.getY() + 1.0D, homePos.getZ() + 0.5D);
    }

    /**
     * Plays the arrival sound and stamps the stack with its new cooldown start
     * time.
     */
    private static void completeTeleport(ServerPlayer player, ServerLevel level, ItemStack stack)
    {
        playPlayerSound(level, player, HearthstoneContentIds.HEARTHSTONE_IMPACT_SOUND);
        setData(stack, getData(stack).withLastUsedGameTime(level.getGameTime()));
    }

    private static void clearLinkedHome(ItemStack stack)
    {
        setData(stack, getData(stack).clearHomeLink());
    }

    private static void playPlayerSound(ServerLevel level, ServerPlayer player, ResourceLocation soundId)
    {
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (soundEvent != null)
        {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static MutableComponent buildCooldownText(int minutes, int seconds)
    {
        return Component.empty().append(translatable(TEXT_COOLDOWN)).append(Component.literal(Integer.toString(minutes))).append(translatable(TEXT_MINUTES))
                .append(Component.literal(Integer.toString(seconds))).append(translatable(TEXT_SECONDS));
    }

    private static MutableComponent translatable(String key)
    {
        return Component.translatable(key);
    }
}
