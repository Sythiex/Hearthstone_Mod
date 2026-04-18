package com.sythiex.hearthstonemod.fabric;

import com.sythiex.hearthstonemod.client.HearthstoneClientCasting;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class HearthstoneFabricClientEvents {
    private HearthstoneFabricClientEvents() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HearthstoneClientCasting.clear());
    }
}
