package com.sythiex.hearthstonemod.fabric;

import com.sythiex.hearthstonemod.client.HearthstoneClientCasting;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Fabric client events used to clear cached cast state on disconnect.
 */
public final class HearthstoneFabricClientEvents
{
    private HearthstoneFabricClientEvents()
    {}

    /**
     * Registers the client disconnect cleanup hook.
     */
    public static void register()
    {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HearthstoneClientCasting.clear());
    }
}
