package com.sythiex.hearthstonemod.neoforge;

import com.sythiex.hearthstonemod.HearthstoneCasting;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * NeoForge gameplay events that drive cast ticking, interruption, and cleanup.
 */
public final class HearthstoneNeoForgeEvents
{
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            HearthstoneCasting.tick(player);
        }
    }

    @SubscribeEvent
    public void onPlayerDamaged(LivingIncomingDamageEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            HearthstoneCasting.requestStop(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        HearthstoneCasting.clear(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerDied(LivingDeathEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) HearthstoneCasting.clear(player);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        HearthstoneCasting.clearAll();
    }
}
