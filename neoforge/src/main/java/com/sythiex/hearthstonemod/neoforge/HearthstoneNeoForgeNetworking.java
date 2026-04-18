package com.sythiex.hearthstonemod.neoforge;

import com.sythiex.hearthstonemod.network.HearthstoneNetworkBridge;
import com.sythiex.hearthstonemod.network.SyncHearthstoneCastingPayload;
import com.sythiex.hearthstonemod.client.HearthstoneClientCasting;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class HearthstoneNeoForgeNetworking {
    private HearthstoneNeoForgeNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncHearthstoneCastingPayload.TYPE, SyncHearthstoneCastingPayload.STREAM_CODEC, HearthstoneNeoForgeNetworking::handleClientbound);
    }

    private static void handleClientbound(SyncHearthstoneCastingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> HearthstoneClientCasting.handleSync(payload));
    }

    public static HearthstoneNetworkBridge createBridge() {
        return (player, casting) -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SyncHearthstoneCastingPayload(player.getId(), casting));
    }
}
