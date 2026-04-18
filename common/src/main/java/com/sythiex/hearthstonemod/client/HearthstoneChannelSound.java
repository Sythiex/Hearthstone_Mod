package com.sythiex.hearthstonemod.client;

import java.util.Objects;

import com.sythiex.hearthstonemod.HearthstoneCommon;
import com.sythiex.hearthstonemod.content.HearthstoneContentIds;

import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class HearthstoneChannelSound extends EntityBoundSoundInstance {
    private final int entityId;
    private int time;

    public HearthstoneChannelSound(Entity entity) {
        super(Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.get(HearthstoneContentIds.HEARTHSTONE_CHANNEL_SOUND)), SoundSource.PLAYERS, 1.0F, 1.0F, entity, 0L);
        this.entityId = entity.getId();
        this.looping = true;
        this.delay = 0;
    }

    @Override
    public void tick() {
        super.tick();
        this.time++;
        if (!HearthstoneClientCasting.isEntityCasting(this.entityId) || this.time >= HearthstoneCommon.config().channelTime()) {
            this.stop();
        }
    }
}
