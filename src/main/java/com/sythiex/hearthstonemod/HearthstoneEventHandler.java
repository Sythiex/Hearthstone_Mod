package com.sythiex.hearthstonemod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class HearthstoneEventHandler
{
	// stops casting if the player takes damage
	@SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
	public void onLivingHurtEvent(LivingHurtEvent event)
	{
		if(event.getEntity() instanceof PlayerEntity)
		{
			PlayerEntity player = (PlayerEntity) event.getEntity();
			ItemStack currentItem = player.inventory.getCurrentItem();
			if(currentItem != null)
			{
				if(currentItem.getItem() instanceof ItemHearthstone)
				{
					CompoundNBT tagCompound = currentItem.getTag();
					tagCompound.putBoolean("stopCasting", true);
					currentItem.setTag(tagCompound);
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
	public void onPlaySoundAtEntityEvent(PlaySoundAtEntityEvent event)
	{
		if(event.getSound() == HearthstoneMod.channelSoundEvent)
		{
			event.setCanceled(true);
			Minecraft.getInstance().getSoundHandler().play(new HearthstoneChannelSound(event.getEntity()));
		}
	}
}