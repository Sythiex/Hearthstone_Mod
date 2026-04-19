package com.sythiex.hearthstonemod;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared mod identifiers used across common and loader-specific code.
 */
public final class HearthstoneConstants
{
    public static final String MOD_ID = "hearthstonemod";
    public static final String MOD_NAME = "Hearthstone Mod";

    private HearthstoneConstants()
    {}

    /**
     * Builds a namespaced id under the Hearthstone mod namespace.
     */
    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
