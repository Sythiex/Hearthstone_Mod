package com.sythiex.hearthstonemod.fabric;

import com.sythiex.hearthstonemod.HearthstoneCasting;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class HearthstoneFabricEvents {
    private HearthstoneFabricEvents() {
    }

    public static void registerCommon() {
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(HearthstoneCasting::tick)
        );
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayer player) {
                HearthstoneCasting.requestStop(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HearthstoneCasting.clear(handler.player));
    }
}
