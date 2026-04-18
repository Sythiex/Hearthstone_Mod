package com.sythiex.hearthstonemod.network;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface HearthstoneNetworkBridge {
    void sendCastingState(ServerPlayer player, boolean casting);

    static HearthstoneNetworkBridge noop() {
        return (player, casting) -> {
        };
    }
}
