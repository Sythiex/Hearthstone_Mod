package com.sythiex.hearthstonemod;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Loader-supplied access point for client-only state needed by shared code.
 */
public interface HearthstoneClientBridge
{
    Level clientLevel();

    boolean isLocalStackCasting(ItemStack stack);

    int getLocalCastTime(ItemStack stack);

    /**
     * Fallback bridge used before client bootstrap or on dedicated servers.
     */
    static HearthstoneClientBridge noop()
    {
        return new HearthstoneClientBridge()
        {
            @Override
            public Level clientLevel()
            {
                return null;
            }

            @Override
            public boolean isLocalStackCasting(ItemStack stack)
            {
                return false;
            }

            @Override
            public int getLocalCastTime(ItemStack stack)
            {
                return 0;
            }
        };
    }
}
