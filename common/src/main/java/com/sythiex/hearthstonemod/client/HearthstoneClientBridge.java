package com.sythiex.hearthstonemod.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface HearthstoneClientBridge {
    Level clientLevel();

    boolean isLocalStackCasting(ItemStack stack);

    int getLocalCastTime(ItemStack stack);

    static HearthstoneClientBridge noop() {
        return new HearthstoneClientBridge() {
            @Override
            public Level clientLevel() {
                return null;
            }

            @Override
            public boolean isLocalStackCasting(ItemStack stack) {
                return false;
            }

            @Override
            public int getLocalCastTime(ItemStack stack) {
                return 0;
            }
        };
    }
}
