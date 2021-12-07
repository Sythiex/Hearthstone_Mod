package com.sythiex.hearthstonemod;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
		if(event.getEntity() instanceof Player)
		{
			Player player = (Player) event.getEntity();
			ItemStack currentItem = player.getInventory().getSelected();
			if(currentItem != null)
			{
				if(currentItem.getItem() instanceof ItemHearthstone)
				{
					CompoundTag tagCompound = currentItem.getTag();
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
			Minecraft.getInstance().getSoundManager().play(new HearthstoneChannelSound(event.getEntity()));
		}
	}
}