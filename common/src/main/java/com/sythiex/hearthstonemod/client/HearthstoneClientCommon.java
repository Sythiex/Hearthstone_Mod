package com.sythiex.hearthstonemod.client;

import com.sythiex.hearthstonemod.HearthstoneClientBridge;
import com.sythiex.hearthstonemod.HearthstoneCommon;

/**
 * Shared client bootstrap invoked by both Fabric and NeoForge entrypoints.
 */
public final class HearthstoneClientCommon
{
    private static boolean bootstrapped;

    private HearthstoneClientCommon()
    {}

    /**
     * Installs the client bridge and initializes shared client-only helpers once.
     */
    public static void bootstrap()
    {
        if (bootstrapped)
        {
            return;
        }

        HearthstoneCommon.setClientBridge(new HearthstoneClientBridge()
        {
            @Override
            public net.minecraft.world.level.Level clientLevel()
            {
                return HearthstoneClientCasting.getClientLevel();
            }

            @Override
            public boolean isLocalStackCasting(net.minecraft.world.item.ItemStack stack)
            {
                return HearthstoneClientCasting.isLocalStackCasting(stack);
            }

            @Override
            public int getLocalCastTime(net.minecraft.world.item.ItemStack stack)
            {
                return HearthstoneClientCasting.getLocalCastTime(stack);
            }
        });
        HearthstoneArmPoses.initialize();
        bootstrapped = true;
    }
}
