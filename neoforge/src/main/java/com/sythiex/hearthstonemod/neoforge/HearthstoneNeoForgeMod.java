package com.sythiex.hearthstonemod.neoforge;

import com.sythiex.hearthstonemod.HearthstoneCommon;
import com.sythiex.hearthstonemod.HearthstoneConstants;
import com.sythiex.hearthstonemod.neoforge.compat.HearthstoneCompatibility;
import com.sythiex.hearthstonemod.neoforge.gametest.HearthstoneGameTestRegistration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

@Mod(HearthstoneConstants.MOD_ID)
public final class HearthstoneNeoForgeMod
{
    public HearthstoneNeoForgeMod(IEventBus modEventBus, ModContainer modContainer)
    {
        HearthstoneCommon.bootstrap(FMLPaths.CONFIGDIR.get(), HearthstoneNeoForgeNetworking.createBridge());
        HearthstoneNeoForgeRegistries.register(modEventBus);
        modEventBus.addListener(HearthstoneNeoForgeRegistries::buildCreativeTabContents);
        modEventBus.addListener(HearthstoneNeoForgeNetworking::registerPayloads);
        modEventBus.addListener(HearthstoneCompatibility::setup);
        NeoForge.EVENT_BUS.register(new HearthstoneNeoForgeEvents());
        if (Boolean.getBoolean("hearthstonemod.gametest")) modEventBus.addListener(HearthstoneGameTestRegistration::register);
    }
}
