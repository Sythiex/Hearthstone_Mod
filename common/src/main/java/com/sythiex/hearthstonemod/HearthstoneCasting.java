package com.sythiex.hearthstonemod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution.Result;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution.Status;
import com.sythiex.hearthstonemod.content.HearthstoneContentIds;
import com.sythiex.hearthstonemod.content.HearthstoneDataComponents;
import com.sythiex.hearthstonemod.content.HearthstoneHomeLink;
import com.sythiex.hearthstonemod.content.HearthstoneItemData;
import com.sythiex.hearthstonemod.content.ItemHearthstone;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Tracks server-side Hearthstone channeling state for each player.
 */
public final class HearthstoneCasting
{
    private static final double CAST_CANCEL_DISTANCE_SQR = 0.05D * 0.05D;
    private static final Map<UUID, CastState> CASTERS = new HashMap<>();
    private static final Map<UUID, ArrivalLease> ARRIVALS = new HashMap<>();
    private static final int PREPARATION_TIMEOUT = 100;

    private HearthstoneCasting()
    {}

    /**
     * Starts channeling with the player's current main-hand stack.
     */
    public static boolean startCasting(ServerPlayer player)
    {
        ItemStack currentItem = player.getMainHandItem();
        if (!HearthstoneContentIds.isHearthstone(currentItem))
        {
            return false;
        }

        UUID playerId = player.getUUID();
        if (CASTERS.containsKey(playerId))
        {
            return false;
        }

        CASTERS.put(playerId, new CastState(player, currentItem));
        HearthstoneCommon.networkBridge().sendCastingState(player, true);
        return true;
    }

    /**
     * Marks an active cast to be stopped on the next server tick.
     */
    public static void requestStop(Player player)
    {
        CastState state = CASTERS.get(player.getUUID());
        if (state != null)
        {
            state.stopCasting = true;
        }
    }

    /**
     * Advances a player's cast timer and completes or cancels it when needed.
     */
    public static void tick(ServerPlayer player)
    {
        ArrivalLease arrival = ARRIVALS.get(player.getUUID());
        if (arrival != null && (!player.isAlive() || --arrival.ticksRemaining <= 0))
        {
            ARRIVALS.remove(player.getUUID());
            arrival.resolution.close();
        }
        CastState state = CASTERS.get(player.getUUID());
        if (state == null)
        {
            return;
        }

        ItemStack currentItem = player.getMainHandItem();
        if (shouldCancelCast(player, currentItem, state))
        {
            stopCasting(player);
            return;
        }

        state.castTime++;
        if (state.castTime >= HearthstoneCommon.config().channelTime())
        {
            try
            {
                if (state.resolution == null)
                {
                    state.resolution = ItemHearthstone.prepareCast(player, currentItem);
                }
                Result result = state.resolution.poll();
                if (result.status() == Status.UNAVAILABLE)
                {
                    player.displayClientMessage(result.message(), true);
                    stopCasting(player);
                    return;
                }
                if (result.status() == Status.READY)
                {
                    // Stop while source-dimension observers still track the player, but
                    // retain the destination lease until teleport completion or failure.
                    HearthstoneDestinationResolution prepared = state.resolution;
                    state.resolution = null;
                    try
                    {
                        stopCasting(player);
                        if (ItemHearthstone.finishCast(player, currentItem, result.destination()))
                        {
                            ArrivalLease previous = ARRIVALS.put(player.getUUID(), new ArrivalLease(prepared));
                            prepared = null; // Ownership transfers to the arrival lease.
                            if (previous != null) previous.resolution.close();
                        }
                    } finally
                    {
                        if (prepared != null) prepared.close();
                    }
                    return;
                }
                if (++state.preparationTicks >= PREPARATION_TIMEOUT)
                {
                    player.displayClientMessage(Component.translatable("item.hearthstone.destination_timeout"), true);
                    stopCasting(player);
                    return;
                }
            } catch (RuntimeException exception)
            {
                LogUtils.getLogger().error("Failed to prepare Hearthstone destination for {}", player.getUUID(), exception);
                player.displayClientMessage(Component.translatable(HearthstoneDestinationResolution.UNAVAILABLE_MESSAGE), true);
                stopCasting(player);
                return;
            }
        }

        state.prevX = player.getX();
        state.prevY = player.getY();
        state.prevZ = player.getZ();
    }

    /**
     * Removes any active cast and sends a stop packet if the player is still
     * connected.
     */
    public static void clear(Player player)
    {
        boolean wasCasting = CASTERS.containsKey(player.getUUID());
        clear(player.getUUID());
        if (wasCasting && player instanceof ServerPlayer serverPlayer)
        {
            HearthstoneCommon.networkBridge().sendCastingState(serverPlayer, false);
        }
    }

    public static void clear(UUID playerId)
    {
        CastState state = CASTERS.remove(playerId);
        if (state != null && state.resolution != null) state.resolution.close();
        ArrivalLease arrival = ARRIVALS.remove(playerId);
        if (arrival != null) arrival.resolution.close();
    }

    /**
     * Releases leases before levels close, including in integrated-server restarts.
     */
    public static void clearAll()
    {
        CASTERS.values().forEach(state -> { if (state.resolution != null) state.resolution.close(); });
        ARRIVALS.values().forEach(arrival -> arrival.resolution.close());
        CASTERS.clear();
        ARRIVALS.clear();
    }

    public static boolean isCasting(UUID playerId)
    {
        return CASTERS.containsKey(playerId);
    }

    private static boolean shouldCancelCast(ServerPlayer player, ItemStack currentItem, CastState state)
    {
        // Stack identity matters here so switching to another Hearthstone stack also
        // stops the cast.
        return state.stopCasting || !player.isAlive() || player.serverLevel() != state.sourceLevel
                || !Objects.equals(homeLink(currentItem), state.homeLink) || player.getInventory().selected != state.selectedSlot || currentItem != state.hearthstone
                || !HearthstoneContentIds.isHearthstone(currentItem) || hasMovedWhileCasting(player, state);
    }

    private static void stopCasting(ServerPlayer player)
    {
        CastState state = CASTERS.remove(player.getUUID());
        if (state != null)
        {
            if (state.resolution != null) state.resolution.close();
            HearthstoneCommon.networkBridge().sendCastingState(player, false);
        }
    }

    private static boolean hasMovedWhileCasting(ServerPlayer player, CastState state)
    {
        double dx = player.getX() - state.prevX;
        double dy = player.getY() - state.prevY;
        double dz = player.getZ() - state.prevZ;
        return dx * dx + dy * dy + dz * dz > CAST_CANCEL_DISTANCE_SQR;
    }

    /**
     * Snapshot of the stack, slot, and position that must stay stable while
     * channeling.
     */
    private static final class CastState
    {
        private final int selectedSlot;
        private final ItemStack hearthstone;
        private final ServerLevel sourceLevel;
        private final Optional<HearthstoneHomeLink> homeLink;
        private HearthstoneDestinationResolution resolution;
        private int preparationTicks;
        private int castTime;
        private boolean stopCasting;
        private double prevX;
        private double prevY;
        private double prevZ;

        private CastState(ServerPlayer player, ItemStack hearthstone)
        {
            this.selectedSlot = player.getInventory().selected;
            this.hearthstone = hearthstone;
            this.sourceLevel = player.serverLevel();
            this.homeLink = homeLink(hearthstone);
            this.prevX = player.getX();
            this.prevY = player.getY();
            this.prevZ = player.getZ();
        }
    }

    private static Optional<HearthstoneHomeLink> homeLink(ItemStack stack)
    {
        HearthstoneItemData data = stack.get(HearthstoneDataComponents.hearthstoneData());
        return data == null ? Optional.empty() : data.homeLink();
    }

    /**
     * Keeps destination loading support alive while normal player tracking takes over.
     */
    private static final class ArrivalLease
    {
        private final HearthstoneDestinationResolution resolution;
        private int ticksRemaining = 20;

        private ArrivalLease(HearthstoneDestinationResolution resolution)
        {
            this.resolution = resolution;
        }
    }
}
