package com.sythiex.hearthstonemod.fabric;

import com.sythiex.hearthstonemod.client.HearthstoneConfigScreens;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Exposes the shared config screen through Mod Menu on Fabric.
 */
public final class HearthstoneModMenuIntegration implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return HearthstoneConfigScreens::create;
    }
}
