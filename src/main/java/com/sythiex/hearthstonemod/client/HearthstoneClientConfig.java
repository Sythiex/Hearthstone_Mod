package com.sythiex.hearthstonemod.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Client-only registration for the mod config screen and animation hooks.
 */
public final class HearthstoneClientConfig
{
    private HearthstoneClientConfig()
    {}

    /**
     * Registers client-only extensions used by the mod.
     */
    public static void register()
    {
        HearthstoneArmPoses.initialize();
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory(HearthstoneConfigScreen::new));
    }
}
