package com.sythiex.hearthstonemod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sythiex.hearthstonemod.HearthstoneConfig.HearthstoneSettings;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Tracks active Hearthstone casts on the server.
 */
public final class HearthstoneCasting
{
    private static final double CAST_CANCEL_DISTANCE_SQR = 0.05D * 0.05D;
    private static final Map<UUID, CastState> CASTERS = new HashMap<>();

    private HearthstoneCasting()
    {}

    /**
     * Starts channeling for the given player if they are holding a Hearthstone and
     * are not already casting.
     *
     * @param player player attempting to start a cast
     * @return {@code true} if a new cast state was created
     */
    public static boolean startCasting(ServerPlayer player)
    {
        ItemStack currentItem = player.getMainHandItem();
        if (currentItem.getItem() != ModItems.HEARTHSTONE.get())
        {
            return false;
        }

        UUID playerId = player.getUUID();
        if (CASTERS.containsKey(playerId))
        {
            return false;
        }

        CASTERS.put(playerId, new CastState(player.getInventory().selected, currentItem, player.getX(), player.getY(), player.getZ()));
        HearthstoneNetwork.sendCastingState(player, true);
        return true;
    }

    /**
     * Marks an active cast to stop on the next server tick.
     *
     * @param player player whose cast should be cancelled
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
     * Advances one player's cast state for the current server tick.
     *
     * @param player player to update
     */
    public static void tick(ServerPlayer player)
    {
        CastState state = CASTERS.get(player.getUUID());
        if (state == null)
        {
            return;
        }

        ItemStack currentItem = player.getMainHandItem();
        if (shouldCancelCast(player, currentItem, state))
        {
            stopCasting(player, false);
            return;
        }

        state.castTime++;

        if (state.castTime >= HearthstoneSettings.channelTime.get())
        {
            stopCasting(player, false);
            ItemHearthstone.finishCast(player, currentItem);
            return;
        }

        state.prevX = player.getX();
        state.prevY = player.getY();
        state.prevZ = player.getZ();
    }

    /**
     * Removes any active cast state for the given player without showing messages.
     *
     * @param player player whose state should be cleared
     */
    public static void clear(Player player)
    {
        CASTERS.remove(player.getUUID());
    }

    /**
     * Determines whether the current cast should abort.
     *
     * @param player      casting player
     * @param currentItem item currently held in the main hand
     * @param state       tracked cast state for the player
     * @return {@code true} when the cast should be cancelled
     */
    private static boolean shouldCancelCast(ServerPlayer player, ItemStack currentItem, CastState state)
    {
        return state.stopCasting || player.getInventory().selected != state.selectedSlot || currentItem != state.hearthstone || currentItem.getItem() != ModItems.HEARTHSTONE.get()
                || hasMovedWhileCasting(player, state);
    }

    /**
     * Ends an active cast and optionally shows the cancel message.
     *
     * @param player              player whose cast should end
     * @param showCanceledMessage whether to show the action-bar cancel message
     */
    private static void stopCasting(ServerPlayer player, boolean showCanceledMessage)
    {
        if (CASTERS.remove(player.getUUID()) != null)
        {
            HearthstoneNetwork.sendCastingState(player, false);
            if (showCanceledMessage)
            {
                player.displayClientMessage(Component.translatable("item.hearthstone.canceled"), true);
            }
        }
    }

    /**
     * Checks whether the player has moved far enough to cancel the cast.
     *
     * @param player casting player
     * @param state  tracked cast state containing the previous position sample
     * @return {@code true} if the player moved beyond the cancel threshold
     */
    private static boolean hasMovedWhileCasting(ServerPlayer player, CastState state)
    {
        double dx = player.getX() - state.prevX;
        double dy = player.getY() - state.prevY;
        double dz = player.getZ() - state.prevZ;
        return dx * dx + dy * dy + dz * dz > CAST_CANCEL_DISTANCE_SQR;
    }

    private static final class CastState
    {
        private final int selectedSlot;
        private final ItemStack hearthstone;
        private int castTime;
        private boolean stopCasting;
        private double prevX;
        private double prevY;
        private double prevZ;

        private CastState(int selectedSlot, ItemStack hearthstone, double prevX, double prevY, double prevZ)
        {
            this.selectedSlot = selectedSlot;
            this.hearthstone = hearthstone;
            this.prevX = prevX;
            this.prevY = prevY;
            this.prevZ = prevZ;
        }
    }
}
