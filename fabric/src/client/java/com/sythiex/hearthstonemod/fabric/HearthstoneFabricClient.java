package com.sythiex.hearthstonemod.fabric;

import com.sythiex.hearthstonemod.client.HearthstoneClientCommon;

import net.fabricmc.api.ClientModInitializer;

public final class HearthstoneFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HearthstoneClientCommon.bootstrap();
        HearthstoneFabricClientEvents.register();
        HearthstoneFabricClientNetworking.register();
    }
}
