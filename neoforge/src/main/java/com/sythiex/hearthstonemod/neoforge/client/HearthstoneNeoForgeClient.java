package com.sythiex.hearthstonemod.neoforge.client;

import com.sythiex.hearthstonemod.HearthstoneConstants;
import com.sythiex.hearthstonemod.client.HearthstoneClientCommon;
import com.sythiex.hearthstonemod.client.HearthstoneConfigScreens;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge client bootstrap for shared client systems and config-screen wiring.
 */
@Mod(value = HearthstoneConstants.MOD_ID, dist = Dist.CLIENT)
public final class HearthstoneNeoForgeClient
{
    public HearthstoneNeoForgeClient(ModContainer container)
    {
        HearthstoneClientCommon.bootstrap();
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, screen) -> HearthstoneConfigScreens.create(screen));
        NeoForge.EVENT_BUS.register(new HearthstoneNeoForgeClientEvents());
    }
}
