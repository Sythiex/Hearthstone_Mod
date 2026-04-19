package com.sythiex.hearthstonemod.fabric;

import com.sythiex.hearthstonemod.client.HearthstoneClientCasting;
import com.sythiex.hearthstonemod.network.SyncHearthstoneCastingPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Fabric client networking hookup for shared Hearthstone payloads.
 */
public final class HearthstoneFabricClientNetworking
{
    private HearthstoneFabricClientNetworking()
    {}

    /**
     * Registers the client receiver for cast-state sync packets.
     */
    public static void register()
    {
        ClientPlayNetworking.registerGlobalReceiver(SyncHearthstoneCastingPayload.TYPE, (payload, context) -> HearthstoneClientCasting.handleSync(payload));
    }
}
