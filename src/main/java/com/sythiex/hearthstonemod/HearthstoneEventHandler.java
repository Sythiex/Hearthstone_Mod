package com.sythiex.hearthstonemod;

import com.sythiex.hearthstonemod.client.HearthstoneClientCasting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Bridges Forge events into the Hearthstone cast managers.
 */
public class HearthstoneEventHandler
{
    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
    /**
     * Cancels an active cast when the player takes damage.
     *
     * @param event damage event
     */
    public void onLivingHurtEvent(LivingHurtEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            Player player = (Player) event.getEntity();
            if (!player.level.isClientSide())
            {
                HearthstoneCasting.requestStop(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
    /**
     * Advances server-side casting state once per player tick.
     *
     * @param event player tick event
     */
    public void onPlayerTickEvent(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide() && event.player instanceof ServerPlayer)
        {
            HearthstoneCasting.tick((ServerPlayer) event.player);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
    /**
     * Clears server-side cast state when a player leaves.
     *
     * @param event logout event
     */
    public void onPlayerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event)
    {
        HearthstoneCasting.clear(event.getEntity());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
    /**
     * Clears client-side cast state when disconnecting from a world or server.
     *
     * @param event client logout event
     */
    public void onClientPlayerLoggedOutEvent(ClientPlayerNetworkEvent.LoggingOut event)
    {
        HearthstoneClientCasting.clear();
    }
}
