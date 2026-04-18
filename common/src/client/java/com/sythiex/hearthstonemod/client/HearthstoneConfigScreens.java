package com.sythiex.hearthstonemod.client;

import net.minecraft.client.gui.screens.Screen;

public final class HearthstoneConfigScreens {
    private HearthstoneConfigScreens() {
    }

    public static Screen create(Screen parentScreen) {
        return new HearthstoneConfigScreen(parentScreen);
    }
}
