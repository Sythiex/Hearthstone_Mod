package com.sythiex.hearthstonemod.client;

import com.sythiex.hearthstonemod.ModSoundEvents;
import com.sythiex.hearthstonemod.HearthstoneConfig.HearthstoneSettings;

import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
/**
 * Loops the channel sound while the tracked entity is marked as casting on the
 * client.
 */
public class HearthstoneChannelSound extends EntityBoundSoundInstance
{
    private final int entityId;
    private int time = 0;

    /**
     * Creates a looping channel sound bound to the given entity.
     *
     * @param entity casting entity that should emit the sound
     */
    protected HearthstoneChannelSound(Entity entity)
    {
        super(ModSoundEvents.HEARTHSTONE_CHANNEL.get(), SoundSource.PLAYERS, 1.0F, 1.0F, entity, 0L);
        this.entityId = entity.getId();
        this.looping = true;
        this.delay = 0;
    }

    @Override
    public void tick()
    {
        super.tick();
        time++;
        if (!HearthstoneClientCasting.isEntityCasting(entityId) || time >= HearthstoneSettings.channelTime.get())
        {
            this.stop();
        }
    }
}
