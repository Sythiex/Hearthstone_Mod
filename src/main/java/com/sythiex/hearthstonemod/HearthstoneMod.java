package com.sythiex.hearthstonemod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sythiex.hearthstonemod.proxy.ClientProxy;
import com.sythiex.hearthstonemod.proxy.CommonProxy;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(HearthstoneMod.MODID)
public class HearthstoneMod
{
	public static final String MODID = "hearthstonemod";
	public static final String NAME = "Hearthstone Mod";
	public static final String VERSION = "0.5.2";
	
	public static CommonProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
	
	public static Logger logger = LogManager.getLogger(MODID);
	
	public static Item hearthstone;
	
	public static SoundEvent channelSoundEvent;
	public static SoundEvent castSoundEvent;
	public static SoundEvent impactSoundEvent;
	
	public HearthstoneMod()
	{
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, com.sythiex.hearthstonemod.HearthstoneConfig.SPEC);
		
		MinecraftForge.EVENT_BUS.register(new HearthstoneEventHandler());
	}
	
	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents
	{
		@SubscribeEvent
		public static void Items(final RegistryEvent.Register<Item> event)
		{
			HearthstoneMod.hearthstone = new ItemHearthstone();
			event.getRegistry().register(hearthstone);
		}
		
		@SubscribeEvent
		public static void registerSounds(final RegistryEvent.Register<SoundEvent> event)
		{
			HearthstoneMod.castSoundEvent = new SoundEvent(new ResourceLocation(MODID, "hearthstonecast")).setRegistryName("hearthstonecast");
			HearthstoneMod.impactSoundEvent = new SoundEvent(new ResourceLocation(MODID, "hearthstoneimpact")).setRegistryName("hearthstoneimpact");
			HearthstoneMod.channelSoundEvent = new SoundEvent(new ResourceLocation(MODID, "hearthstonechannel")).setRegistryName("hearthstonechannel");
			event.getRegistry().register(castSoundEvent);
			event.getRegistry().register(impactSoundEvent);
			event.getRegistry().register(channelSoundEvent);
		}
	}
}