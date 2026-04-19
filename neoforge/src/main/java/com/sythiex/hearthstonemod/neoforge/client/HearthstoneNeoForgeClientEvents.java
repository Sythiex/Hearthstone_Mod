package com.sythiex.hearthstonemod.neoforge.client;

import com.sythiex.hearthstonemod.client.HearthstoneClientCasting;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * NeoForge client cleanup hooks for transient Hearthstone state.
 */
public final class HearthstoneNeoForgeClientEvents
{
    @SubscribeEvent
    public void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        HearthstoneClientCasting.clear();
    }
}
