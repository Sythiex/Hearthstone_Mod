package com.sythiex.hearthstonemod.neoforge.compat.sable;

import com.sythiex.hearthstonemod.HearthstoneCommon;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Loaded only after NeoForge confirms that Sable is installed.
 */
public final class SableCompatibility
{
    private SableCompatibility()
    {}

    public static void install()
    {
        SableDestinationResolver.registerTicketType();
        HearthstoneCommon.setDestinationBridge(new SableDestinationBridge());
        NeoForge.EVENT_BUS.addListener(SableCompatibility::serverStarted);
    }

    /**
     * Discards temporary tickets persisted by an interrupted prior server session.
     */
    private static void serverStarted(ServerStartedEvent event)
    {
        event.getServer().getAllLevels().forEach(SableDestinationResolver::clearStaleTickets);
    }
}
