package com.sythiex.hearthstonemod;

import com.sythiex.hearthstonemod.HearthstoneConfig.HearthstoneSettings;

import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HearthstoneChannelSound extends EntityBoundSoundInstance
{
	private ItemStack hearthstone = null;
	private int time = 0;
	
	protected HearthstoneChannelSound(Entity entity)
	{
		super(HearthstoneMod.channelSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F, entity);
		this.looping = true;
		this.delay = 0;
		/*
		 * this.volume = 1.0F;
		 * this.pitch = 1.0F;
		 * this.x = (float) entity.getPosX();
		 * this.y = (float) entity.getPosY();
		 * this.z = (float) entity.getPosZ();
		 */
		
		if(entity instanceof Player)
		{
			Player player = (Player) entity;
			ItemStack currentItem = player.getInventory().getSelected();
			if(currentItem != null)
			{
				if(currentItem.getItem() instanceof ItemHearthstone)
				{
					this.hearthstone = currentItem;
				}
			}
		}
	}
	
	@Override
	public void tick()
	{
		time++;
		if(hearthstone == null || time >= HearthstoneSettings.channelTime.get())
		{
			this.stop();
		}
		else
		{
			CompoundTag tag = hearthstone.getTag();
			if(!tag.getBoolean("isCasting"))
			{
				this.stop();
			}
		}
	}
}