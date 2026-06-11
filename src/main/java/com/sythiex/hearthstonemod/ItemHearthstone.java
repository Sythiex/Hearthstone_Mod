package com.sythiex.hearthstonemod;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.sythiex.hearthstonemod.HearthstoneConfig.HearthstoneSettings;
import com.sythiex.hearthstonemod.client.HearthstoneArmPoses;
import com.sythiex.hearthstonemod.client.HearthstoneClientCasting;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Main item implementation for the Hearthstone.
 *
 * Persistent stack data stores the linked home and cooldown timestamp, while
 * channeling state is managed separately by {@link HearthstoneCasting}.
 */
public class ItemHearthstone extends Item
{
    public static final String NAME = "hearthstone";

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

    private static final String TAG_LAST_USED_GAME_TIME = "lastUsedGameTime";
    private static final String TAG_HOME_X = "homeX";
    private static final String TAG_HOME_Y = "homeY";
    private static final String TAG_HOME_Z = "homeZ";
    private static final String TAG_DIMENSION_LOCATION = "dimensionResourceLocation";
    private static final String TAG_LINK_TYPE = "linkType";

    private static final String LINK_TYPE_BED = "bed";
    private static final String LINK_TYPE_RESPAWN_ANCHOR = "respawn_anchor";

    public ItemHearthstone()
    {
        super(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            @Nullable
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack)
            {
                // Only the actively cast main-hand stack should affect posing.
                if (hand != InteractionHand.MAIN_HAND || entityLiving.getMainHandItem() != itemStack)
                {
                    return null;
                }

                return HearthstoneArmPoses.getChannelingPose(entityLiving);
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess)
            {
                return HearthstoneArmPoses.applyFirstPersonChannelingTransform(poseStack, player, arm, itemInHand, partialTick);
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand)
    {
        if (!world.isClientSide && hand == InteractionHand.MAIN_HAND)
        {
            ItemStack itemStack = player.getItemInHand(hand);
            CompoundTag tag = itemStack.getTag();

            if (!player.isCrouching())
            {
                if (HearthstoneConfig.isDimensionBlacklisted(world))
                {
                    player.displayClientMessage(translatable(TEXT_DIMENSION_BLACKLISTED), true);
                } else if (isLinkedHome(tag))
                {
                    if (isRespawnAnchorLinkingDisabled(tag))
                    {
                        player.displayClientMessage(translatable(TEXT_RESPAWN_ANCHOR_DISABLED), true);
                    } else if (isCrossDimensionTravelBlocked(world, tag))
                    {
                        player.displayClientMessage(translatable(TEXT_CROSS_DIMENSION_DISABLED), true);
                    } else if (getCooldown(world, tag) == 0)
                    {
                        if (player instanceof ServerPlayer)
                        {
                            HearthstoneCasting.startCasting((ServerPlayer) player);
                        }
                    } else
                    {
                        player.displayClientMessage(translatable(TEXT_ON_COOLDOWN), true);
                    }
                } else
                {
                    player.displayClientMessage(translatable(TEXT_NO_HOME), true);
                }
            }
        }

        return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player == null)
        {
            return InteractionResult.PASS;
        }

        if (!context.getLevel().isClientSide())
        {
            ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (itemStack.getItem() != ModItems.HEARTHSTONE.get())
            {
                return InteractionResult.FAIL;
            }

            if (player.isCrouching())
            {
                BlockState state = context.getLevel().getBlockState(context.getClickedPos());
                if (state.getBlock().isBed(state, context.getLevel(), context.getClickedPos(), player))
                {
                    if (HearthstoneConfig.isDimensionBlacklisted(context.getLevel()))
                    {
                        player.displayClientMessage(translatable(TEXT_DIMENSION_BLACKLISTED), true);
                        return InteractionResult.SUCCESS;
                    }

                    linkHome(itemStack, context.getClickedPos(), player.level.dimension(), LINK_TYPE_BED);
                    player.displayClientMessage(translatable(TEXT_LINKED), true);
                } else if (isRespawnAnchor(state))
                {
                    if (HearthstoneConfig.isDimensionBlacklisted(context.getLevel()))
                    {
                        player.displayClientMessage(translatable(TEXT_DIMENSION_BLACKLISTED), true);
                        return InteractionResult.SUCCESS;
                    }

                    if (!HearthstoneSettings.allowRespawnAnchorLinking.get())
                    {
                        player.displayClientMessage(translatable(TEXT_RESPAWN_ANCHOR_DISABLED), true);
                        return InteractionResult.SUCCESS;
                    }

                    if (getRespawnAnchorCharge(state) == 0)
                    {
                        player.displayClientMessage(translatable(TEXT_RESPAWN_ANCHOR_NO_CHARGE), true);
                        return InteractionResult.SUCCESS;
                    }

                    linkHome(itemStack, context.getClickedPos(), player.level.dimension(), LINK_TYPE_RESPAWN_ANCHOR);
                    player.displayClientMessage(translatable(TEXT_LINKED), true);
                }
                return InteractionResult.SUCCESS;
            }

            return use(context.getLevel(), player, InteractionHand.MAIN_HAND).getResult();
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isBarVisible(ItemStack itemStack)
    {
        return getCooldown(HearthstoneClientCasting.getClientLevel(), itemStack.getTag()) > 0 || HearthstoneClientCasting.isLocalStackCasting(itemStack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getBarWidth(ItemStack itemStack)
    {
        int cooldown = getCooldown(HearthstoneClientCasting.getClientLevel(), itemStack.getTag());
        if (cooldown > 0)
        {
            return Math.round(13.0F - (float) cooldown * 13.0F / (float) HearthstoneSettings.cooldown.get());
        }

        int castTime = HearthstoneClientCasting.getLocalCastTime(itemStack);
        if (castTime > 0)
        {
            return Math.round(13.0F - ((float) HearthstoneSettings.channelTime.get() - (float) castTime) * 13.0F / (float) HearthstoneSettings.channelTime.get());
        }

        return 0;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getBarColor(ItemStack itemStack)
    {
        if (getCooldown(HearthstoneClientCasting.getClientLevel(), itemStack.getTag()) > 0)
        {
            return 0xFF0000;
        }

        return 0x4BC1E3;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack itemStack, @Nullable Level world, List<Component> tooltip, TooltipFlag flagIn)
    {
        CompoundTag tag = itemStack.getTag();
        if (tag != null)
        {
            if (isLinkedHome(tag))
            {
                tooltip.add(translatable(TEXT_HOME_SET).withStyle(ChatFormatting.GRAY));
            }

            int cooldown = getCooldown(world != null ? world : HearthstoneClientCasting.getClientLevel(), tag);
            if (cooldown != 0)
            {
                // Round up partial seconds so the tooltip matches player expectations.
                cooldown += 19;
                int minutes = cooldown / 1200;
                int seconds = (cooldown / 20) - (minutes * 60);
                tooltip.add(buildCooldownText(minutes, seconds).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isFoil(ItemStack stack)
    {
        return HearthstoneClientCasting.isLocalStackCasting(stack);
    }

    /**
     * Resolves the final teleport after server-side channeling completes.
     *
     * Cross-dimension travel is handled first, then the linked home block is
     * validated so the item can either teleport safely or break the link with a
     * fallback teleport.
     *
     * @param player    player completing the cast
     * @param itemStack Hearthstone stack that finished channeling
     */
    static void finishCast(ServerPlayer player, ItemStack itemStack)
    {
        CompoundTag tag = itemStack.getTag();
        if (tag == null || !isLinkedHome(tag))
        {
            return;
        }

        if (isRespawnAnchorLinkingDisabled(tag))
        {
            player.displayClientMessage(translatable(TEXT_RESPAWN_ANCHOR_DISABLED), true);
            return;
        }

        ServerLevel world = player.getLevel();
        int homeX = tag.getInt(TAG_HOME_X);
        int homeY = tag.getInt(TAG_HOME_Y);
        int homeZ = tag.getInt(TAG_HOME_Z);

        world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSoundEvents.HEARTHSTONE_CAST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        ResourceKey<Level> savedDimensionKey = getWorldResourceKey(tag.getString(TAG_DIMENSION_LOCATION));
        ResourceKey<Level> playerDimensionKey = player.level.dimension();
        if (savedDimensionKey != null && !playerDimensionKey.equals(savedDimensionKey))
        {
            if (!HearthstoneSettings.allowCrossDimensionTravel.get())
            {
                player.displayClientMessage(translatable(TEXT_CROSS_DIMENSION_DISABLED), true);
                return;
            }

            MinecraftServer server = player.level.getServer();
            ServerLevel destinationServerLevel = server == null ? null : server.getLevel(savedDimensionKey);
            if (destinationServerLevel != null)
            {
                player.teleportTo(destinationServerLevel, homeX + 0.5D, homeY + 1.0D, homeZ + 0.5D, player.getYRot(), player.getXRot());
                world = destinationServerLevel;
            }
        }

        BlockPos homePos = new BlockPos(homeX, homeY, homeZ);
        BlockState state = world.getBlockState(homePos);
        if (isLinkedToRespawnAnchor(tag))
        {
            finishRespawnAnchorCast(player, world, tag, homePos, state);
        } else
        {
            finishBedCast(player, world, tag, homePos, state);
        }
    }

    /**
     * Rebuilds a dimension key from the saved NBT string.
     *
     * @param location serialized dimension id from item NBT
     * @return parsed dimension key, or {@code null} if the id is empty or invalid
     */
    private static ResourceKey<Level> getWorldResourceKey(String location)
    {
        if (location.isEmpty())
        {
            return null;
        }

        ResourceLocation dimensionResourceLocation = ResourceLocation.tryParse(location);
        return dimensionResourceLocation == null ? null : ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionResourceLocation);
    }

    /**
     * Returns the stack's persistent data tag, creating it when needed.
     *
     * @param itemStack stack whose tag should be returned
     * @return existing or newly created stack data tag
     */
    private static CompoundTag getOrCreateData(ItemStack itemStack)
    {
        return itemStack.getOrCreateTag();
    }

    /**
     * Stores the linked home location, dimension, and home type on the stack.
     *
     * @param itemStack    stack being linked
     * @param homePos      saved home position
     * @param dimensionKey dimension containing that home
     * @param linkType     saved home type, such as bed or respawn anchor
     */
    private static void linkHome(ItemStack itemStack, BlockPos homePos, ResourceKey<Level> dimensionKey, String linkType)
    {
        CompoundTag tag = getOrCreateData(itemStack);
        tag.putInt(TAG_HOME_X, homePos.getX());
        tag.putInt(TAG_HOME_Y, homePos.getY());
        tag.putInt(TAG_HOME_Z, homePos.getZ());
        tag.putString(TAG_DIMENSION_LOCATION, dimensionKey.location().toString());
        tag.putString(TAG_LINK_TYPE, linkType);
    }

    /**
     * Computes the remaining cooldown for the stack from its last-used timestamp.
     *
     * @param world world whose game time should be used
     * @param tag   stack data tag
     * @return remaining cooldown in ticks, or {@code 0} when the stack is ready
     */
    private static int getCooldown(@Nullable Level world, @Nullable CompoundTag tag)
    {
        if (tag == null)
        {
            return 0;
        }

        if (tag.contains(TAG_LAST_USED_GAME_TIME))
        {
            if (world == null)
            {
                return 0;
            }

            long elapsedTime = world.getGameTime() - tag.getLong(TAG_LAST_USED_GAME_TIME);
            long remainingCooldown = (long) HearthstoneSettings.cooldown.get() - elapsedTime;
            return (int) Math.max(0L, remainingCooldown);
        }

        return 0;
    }

    /**
     * Records the current game time as the stack's last successful use.
     *
     * @param tag   stack data tag to update
     * @param world world supplying the current game time
     */
    private static void setLastUsedGameTime(CompoundTag tag, Level world)
    {
        tag.putLong(TAG_LAST_USED_GAME_TIME, world.getGameTime());
    }

    /**
     * Checks whether the stack currently has a valid linked home.
     *
     * @param tag stack data tag
     * @return {@code true} if the stack has both a saved dimension and link type
     */
    private static boolean isLinkedHome(@Nullable CompoundTag tag)
    {
        return tag != null && !tag.getString(TAG_DIMENSION_LOCATION).isEmpty() && !getLinkType(tag).isEmpty();
    }

    /**
     * Checks whether config forbids travelling from the current world to the saved
     * linked dimension.
     *
     * @param world world where the cast is being attempted
     * @param tag   stack data tag
     * @return {@code true} if cross-dimension travel is disabled and the saved home
     *         is in a different dimension
     */
    private static boolean isCrossDimensionTravelBlocked(Level world, @Nullable CompoundTag tag)
    {
        if (HearthstoneSettings.allowCrossDimensionTravel.get() || !isLinkedHome(tag))
        {
            return false;
        }

        ResourceKey<Level> linkedDimensionKey = getWorldResourceKey(tag.getString(TAG_DIMENSION_LOCATION));
        return linkedDimensionKey != null && !world.dimension().equals(linkedDimensionKey);
    }

    /**
     * Checks whether the stack is linked to a respawn anchor while anchor support
     * is disabled by config.
     *
     * @param tag stack data tag
     * @return {@code true} if the linked home is a respawn anchor and that feature
     *         is disabled
     */
    private static boolean isRespawnAnchorLinkingDisabled(@Nullable CompoundTag tag)
    {
        return isLinkedToRespawnAnchor(tag) && !HearthstoneSettings.allowRespawnAnchorLinking.get();
    }

    /**
     * Checks whether the saved home type is a respawn anchor.
     *
     * @param tag stack data tag
     * @return {@code true} if the linked home type is {@code respawn_anchor}
     */
    private static boolean isLinkedToRespawnAnchor(@Nullable CompoundTag tag)
    {
        return LINK_TYPE_RESPAWN_ANCHOR.equals(getLinkType(tag));
    }

    /**
     * Returns the saved link type string from item NBT.
     *
     * @param tag stack data tag
     * @return saved link type, or an empty string when unset
     */
    private static String getLinkType(@Nullable CompoundTag tag)
    {
        return tag == null ? "" : tag.getString(TAG_LINK_TYPE);
    }

    /**
     * Checks whether a block state is a respawn anchor.
     *
     * @param state block state to inspect
     * @return {@code true} if the block is a respawn anchor
     */
    private static boolean isRespawnAnchor(BlockState state)
    {
        return state.is(Blocks.RESPAWN_ANCHOR);
    }

    /**
     * Reads the current charge level from a respawn anchor block state.
     *
     * @param state respawn anchor block state
     * @return current anchor charge count
     */
    private static int getRespawnAnchorCharge(BlockState state)
    {
        return state.getValue(RespawnAnchorBlock.CHARGE);
    }

    /**
     * Completes a teleport to a linked bed.
     *
     * @param player  player being teleported
     * @param world   destination world
     * @param tag     stack data tag
     * @param homePos linked home position
     * @param state   block state at the linked position
     */
    private static void finishBedCast(ServerPlayer player, ServerLevel world, CompoundTag tag, BlockPos homePos, BlockState state)
    {
        if (state.getBlock().isBed(state, world, homePos, player))
        {
            Optional<Vec3> respawnPosition = state.getRespawnPosition(EntityType.PLAYER, world, homePos, player.getYRot(), player);
            teleportToRespawnPosition(player, respawnPosition, homePos);
            completeTeleport(player, world, tag);
            return;
        }

        breakLinkedHome(player, world, tag, homePos, TEXT_MISSING_BED);
    }

    /**
     * Completes a teleport to a linked respawn anchor and optionally consumes a
     * charge.
     *
     * @param player  player being teleported
     * @param world   destination world
     * @param tag     stack data tag
     * @param homePos linked home position
     * @param state   block state at the linked position
     */
    private static void finishRespawnAnchorCast(ServerPlayer player, ServerLevel world, CompoundTag tag, BlockPos homePos, BlockState state)
    {
        if (!isRespawnAnchor(state))
        {
            breakLinkedHome(player, world, tag, homePos, TEXT_MISSING_RESPAWN_ANCHOR);
            return;
        }

        int charge = getRespawnAnchorCharge(state);
        if (charge == 0)
        {
            breakLinkedHome(player, world, tag, homePos, TEXT_EMPTY_RESPAWN_ANCHOR);
            return;
        }

        Optional<Vec3> respawnPosition = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, world, homePos);
        teleportToRespawnPosition(player, respawnPosition, homePos);
        completeTeleport(player, world, tag);

        if (HearthstoneSettings.consumeRespawnAnchorCharge.get())
        {
            int remainingCharge = charge - 1;
            world.setBlock(homePos, state.setValue(RespawnAnchorBlock.CHARGE, remainingCharge), 3);
            world.playSound(null, homePos, SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (remainingCharge == 0)
            {
                clearLinkedHome(tag);
                player.displayClientMessage(translatable(TEXT_EMPTY_RESPAWN_ANCHOR), true);
            }
        }
    }

    /**
     * Teleports the player to a safe respawn position when one exists, or to the
     * top of the saved home block otherwise.
     *
     * @param player          player being teleported
     * @param respawnPosition vanilla safe stand-up position lookup result
     * @param homePos         saved home position used for fallback
     */
    private static void teleportToRespawnPosition(ServerPlayer player, Optional<Vec3> respawnPosition, BlockPos homePos)
    {
        if (respawnPosition.isPresent())
        {
            Vec3 safePosition = respawnPosition.get();
            player.teleportTo(safePosition.x, safePosition.y, safePosition.z);
        } else
        {
            teleportToHomeTop(player, homePos);
        }
    }

    /**
     * Breaks the current link after a missing or invalid home fallback teleport.
     *
     * @param player     player being teleported
     * @param world      destination world
     * @param tag        stack data tag
     * @param homePos    saved home position
     * @param messageKey translation key for the broken-link message
     */
    private static void breakLinkedHome(ServerPlayer player, ServerLevel world, CompoundTag tag, BlockPos homePos, String messageKey)
    {
        teleportToHomeTop(player, homePos);
        completeTeleport(player, world, tag);
        clearLinkedHome(tag);
        player.displayClientMessage(translatable(messageKey), true);
    }

    /**
     * Teleports the player to the top center of the saved home block.
     *
     * @param player  player being teleported
     * @param homePos saved home position
     */
    private static void teleportToHomeTop(ServerPlayer player, BlockPos homePos)
    {
        player.teleportTo(homePos.getX() + 0.5D, homePos.getY() + 1.0D, homePos.getZ() + 0.5D);
    }

    /**
     * Finalizes a successful teleport by playing the impact sound and starting
     * cooldown tracking.
     *
     * @param player teleported player
     * @param world  world where the teleport completed
     * @param tag    stack data tag
     */
    private static void completeTeleport(ServerPlayer player, ServerLevel world, CompoundTag tag)
    {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSoundEvents.HEARTHSTONE_IMPACT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        setLastUsedGameTime(tag, world);
    }

    /**
     * Clears the active link while keeping the last saved coordinates so
     * broken-link fallbacks can still land at the stored home position.
     *
     * @param tag stack data tag to clear
     */
    private static void clearLinkedHome(CompoundTag tag)
    {
        tag.putString(TAG_DIMENSION_LOCATION, "");
        tag.putString(TAG_LINK_TYPE, "");
    }

    /**
     * Builds the formatted cooldown tooltip line.
     *
     * @param minutes whole minutes remaining
     * @param seconds whole seconds remaining after minutes are removed
     * @return localized cooldown text component
     */
    private static MutableComponent buildCooldownText(int minutes, int seconds)
    {
        return Component.empty().append(translatable(TEXT_COOLDOWN)).append(Component.literal(Integer.toString(minutes))).append(translatable(TEXT_MINUTES))
                .append(Component.literal(Integer.toString(seconds))).append(translatable(TEXT_SECONDS));
    }

    /**
     * Convenience wrapper for creating translatable text components.
     *
     * @param key translation key to resolve
     * @return localized mutable component for the key
     */
    private static MutableComponent translatable(String key)
    {
        return Component.translatable(key);
    }
}
