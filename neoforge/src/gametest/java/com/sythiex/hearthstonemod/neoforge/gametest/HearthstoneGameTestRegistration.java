package com.sythiex.hearthstonemod.neoforge.gametest;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Development-only tests, registered explicitly and omitted from distributed jars.
 */
public final class HearthstoneGameTestRegistration
{
    private HearthstoneGameTestRegistration()
    {}

    public static void register(RegisterGameTestsEvent event)
    {
        if (!System.getProperty("hearthstonemod.restartTest", "").isEmpty())
        {
            if (!ModList.get().isLoaded("sable")) throw new IllegalStateException("Restart verification requires -PsableCompat");
            event.register(SableRestartGameTests.class);
            return;
        }
        event.register(HearthstoneDestinationGameTests.class);
        if (ModList.get().isLoaded("sable")) event.register(SableDestinationGameTests.class);
    }
}
