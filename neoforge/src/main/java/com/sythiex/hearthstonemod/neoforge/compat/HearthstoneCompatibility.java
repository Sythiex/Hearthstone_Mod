package com.sythiex.hearthstonemod.neoforge.compat;

import com.sythiex.hearthstonemod.neoforge.compat.sable.SableCompatibility;

import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class HearthstoneCompatibility
{
    private HearthstoneCompatibility()
    {}

    public static void setup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            if (ModList.get().isLoaded("sable")) SableCompatibility.install();
        });
    }
}
