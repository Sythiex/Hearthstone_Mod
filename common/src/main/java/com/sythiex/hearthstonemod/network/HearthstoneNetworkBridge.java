package com.sythiex.hearthstonemod.network;

import net.minecraft.server.level.ServerPlayer;

/**
 * Loader bridge for broadcasting client-facing cast state.
 */
@FunctionalInterface
public interface HearthstoneNetworkBridge
{
    void sendCastingState(ServerPlayer player, boolean casting);

    /**
     * No-op bridge used before loader initialization finishes.
     */
    static HearthstoneNetworkBridge noop()
    {
        return (player, casting) ->
        {
        };
    }
}
