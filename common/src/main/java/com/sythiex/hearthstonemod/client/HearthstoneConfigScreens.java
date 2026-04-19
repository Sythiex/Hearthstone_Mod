package com.sythiex.hearthstonemod.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * Shared factory for opening the Hearthstone config screen from loader
 * integrations.
 */
public final class HearthstoneConfigScreens
{
    private HearthstoneConfigScreens()
    {}

    public static Screen create(Screen parentScreen)
    {
        return new HearthstoneConfigScreen(parentScreen);
    }
}
