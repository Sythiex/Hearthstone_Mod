package com.sythiex.hearthstonemod.fabric;

import java.util.LinkedHashSet;
import java.util.Set;

import com.sythiex.hearthstonemod.network.HearthstoneNetworkBridge;
import com.sythiex.hearthstonemod.network.SyncHearthstoneCastingPayload;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class HearthstoneFabricNetworking {
    private HearthstoneFabricNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(SyncHearthstoneCastingPayload.TYPE, SyncHearthstoneCastingPayload.STREAM_CODEC);
    }

    public static HearthstoneNetworkBridge createBridge() {
        return (player, casting) -> {
            SyncHearthstoneCastingPayload payload = new SyncHearthstoneCastingPayload(player.getId(), casting);
            Set<net.minecraft.server.level.ServerPlayer> recipients = new LinkedHashSet<>(PlayerLookup.tracking(player));
            recipients.add(player);
            recipients.forEach(serverPlayer -> ServerPlayNetworking.send(serverPlayer, payload));
        };
    }
}
