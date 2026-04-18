package com.sythiex.hearthstonemod.fabric;

import com.sythiex.hearthstonemod.HearthstoneCommon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class HearthstoneFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        HearthstoneCommon.bootstrap(FabricLoader.getInstance().getConfigDir(), HearthstoneFabricNetworking.createBridge());
        HearthstoneFabricRegistries.register();
        HearthstoneFabricEvents.registerCommon();
        HearthstoneFabricNetworking.registerCommon();
    }
}
