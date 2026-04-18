package com.sythiex.hearthstonemod;

import net.minecraft.resources.ResourceLocation;

public final class HearthstoneConstants {
    public static final String MOD_ID = "hearthstonemod";
    public static final String MOD_NAME = "Hearthstone Mod";

    private HearthstoneConstants() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
