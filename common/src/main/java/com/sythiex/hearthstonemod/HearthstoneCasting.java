package com.sythiex.hearthstonemod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sythiex.hearthstonemod.content.HearthstoneContentIds;
import com.sythiex.hearthstonemod.content.ItemHearthstone;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HearthstoneCasting {
    private static final double CAST_CANCEL_DISTANCE_SQR = 0.05D * 0.05D;
    private static final Map<UUID, CastState> CASTERS = new HashMap<>();

    private HearthstoneCasting() {
    }

    public static boolean startCasting(ServerPlayer player) {
        ItemStack currentItem = player.getMainHandItem();
        if (!HearthstoneContentIds.isHearthstone(currentItem)) {
            return false;
        }

        UUID playerId = player.getUUID();
        if (CASTERS.containsKey(playerId)) {
            return false;
        }

        CASTERS.put(playerId, new CastState(player.getInventory().selected, currentItem, player.getX(), player.getY(), player.getZ()));
        HearthstoneCommon.networkBridge().sendCastingState(player, true);
        return true;
    }

    public static void requestStop(Player player) {
        CastState state = CASTERS.get(player.getUUID());
        if (state != null) {
            state.stopCasting = true;
        }
    }

    public static void tick(ServerPlayer player) {
        CastState state = CASTERS.get(player.getUUID());
        if (state == null) {
            return;
        }

        ItemStack currentItem = player.getMainHandItem();
        if (shouldCancelCast(player, currentItem, state)) {
            stopCasting(player);
            return;
        }

        state.castTime++;
        if (state.castTime >= HearthstoneCommon.config().channelTime()) {
            stopCasting(player);
            ItemHearthstone.finishCast(player, currentItem);
            return;
        }

        state.prevX = player.getX();
        state.prevY = player.getY();
        state.prevZ = player.getZ();
    }

    public static void clear(Player player) {
        if (CASTERS.remove(player.getUUID()) != null && player instanceof ServerPlayer serverPlayer) {
            HearthstoneCommon.networkBridge().sendCastingState(serverPlayer, false);
        }
    }

    public static void clear(UUID playerId) {
        CASTERS.remove(playerId);
    }

    public static boolean isCasting(UUID playerId) {
        return CASTERS.containsKey(playerId);
    }

    private static boolean shouldCancelCast(ServerPlayer player, ItemStack currentItem, CastState state) {
        return state.stopCasting
                || player.getInventory().selected != state.selectedSlot
                || currentItem != state.hearthstone
                || !HearthstoneContentIds.isHearthstone(currentItem)
                || hasMovedWhileCasting(player, state);
    }

    private static void stopCasting(ServerPlayer player) {
        if (CASTERS.remove(player.getUUID()) != null) {
            HearthstoneCommon.networkBridge().sendCastingState(player, false);
        }
    }

    private static boolean hasMovedWhileCasting(ServerPlayer player, CastState state) {
        double dx = player.getX() - state.prevX;
        double dy = player.getY() - state.prevY;
        double dz = player.getZ() - state.prevZ;
        return dx * dx + dy * dy + dz * dz > CAST_CANCEL_DISTANCE_SQR;
    }

    private static final class CastState {
        private final int selectedSlot;
        private final ItemStack hearthstone;
        private int castTime;
        private boolean stopCasting;
        private double prevX;
        private double prevY;
        private double prevZ;

        private CastState(int selectedSlot, ItemStack hearthstone, double prevX, double prevY, double prevZ) {
            this.selectedSlot = selectedSlot;
            this.hearthstone = hearthstone;
            this.prevX = prevX;
            this.prevY = prevY;
            this.prevZ = prevZ;
        }
    }
}
