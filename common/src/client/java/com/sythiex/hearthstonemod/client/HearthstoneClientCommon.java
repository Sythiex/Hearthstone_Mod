package com.sythiex.hearthstonemod.client;

import com.sythiex.hearthstonemod.HearthstoneCommon;

public final class HearthstoneClientCommon {
    private static boolean bootstrapped;

    private HearthstoneClientCommon() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        HearthstoneCommon.setClientBridge(new HearthstoneClientBridge() {
            @Override
            public net.minecraft.world.level.Level clientLevel() {
                return HearthstoneClientCasting.getClientLevel();
            }

            @Override
            public boolean isLocalStackCasting(net.minecraft.world.item.ItemStack stack) {
                return HearthstoneClientCasting.isLocalStackCasting(stack);
            }

            @Override
            public int getLocalCastTime(net.minecraft.world.item.ItemStack stack) {
                return HearthstoneClientCasting.getLocalCastTime(stack);
            }
        });
        HearthstoneArmPoses.initialize();
        bootstrapped = true;
    }
}
