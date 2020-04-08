package com.sythiex.hearthstonemod;

import com.sythiex.hearthstonemod.HearthstoneConfig.HearthstoneSettings;

import net.minecraft.client.audio.TickableSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HearthstoneChannelSound extends TickableSound
{
	private ItemStack hearthstone = null;
	private int time = 0;
	
	protected HearthstoneChannelSound(Entity entity)
	{
		super(HearthstoneMod.channelSoundEvent, SoundCategory.PLAYERS);
		this.repeat = true;
		this.repeatDelay = 0;
		this.volume = 1.0F;
		this.pitch = 1.0F;
		this.x = (float) entity.getPosX();
		this.y = (float) entity.getPosY();
		this.z = (float) entity.getPosZ();
		
		if(entity instanceof PlayerEntity)
		{
			PlayerEntity player = (PlayerEntity) entity;
			ItemStack currentItem = player.inventory.getCurrentItem();
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
			this.donePlaying = true;
		}
		else
		{
			CompoundNBT tag = hearthstone.getTag();
			if(!tag.getBoolean("isCasting"))
			{
				this.donePlaying = true;
			}
		}
	}
}