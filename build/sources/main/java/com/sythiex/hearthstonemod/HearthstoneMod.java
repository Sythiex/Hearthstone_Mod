package com.sythiex.hearthstonemod;

import org.apache.logging.log4j.Logger;

import com.sythiex.hearthstonemod.proxy.CommonProxy;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.GameData;

@Mod(modid = HearthstoneMod.MODID, name = HearthstoneMod.NAME, version = HearthstoneMod.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class HearthstoneMod
{
	@SidedProxy(clientSide = "com.sythiex.hearthstonemod.proxy.ClientProxy", serverSide = "com.sythiex.hearthstonemod.proxy.CommonProxy")
	public static CommonProxy proxy;
	
	public static final String MODID = "hearthstonemod";
	public static final String NAME = "Hearthstone Mod";
	public static final String VERSION = "0.4.7";
	
	@Instance(MODID)
	public static HearthstoneMod instance;
	
	public static Logger logger;
	
	public static boolean easyRecipe;
	public static int hearthstoneChannelTime;
	public static int hearthstoneCooldown;
	
	public static Item hearthstone;
	
	public static SoundEvent channelSoundEvent;
	public static SoundEvent castSoundEvent;
	public static SoundEvent impactSoundEvent;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
		logger.info("Preinitializing");
		
		logger.info("Loading config");
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		
		easyRecipe = config.getBoolean("Use easy recipe", config.CATEGORY_GENERAL, false, "");
		hearthstoneChannelTime = config.getInt("Channel Time", config.CATEGORY_GENERAL, 200, 0, 1200, "How long you must wait for the hearthstone to teleport you. Measured in ticks (20 ticks = 1 second). Default 10 seconds");
		hearthstoneCooldown = config.getInt("Cooldown", config.CATEGORY_GENERAL, 36000, 0, 1728000, "How long you must wait between hearthstone uses. Measured in ticks (20 ticks = 1 second). Default 30 minutes");
		
		config.save();
		
		logger.info("Registering items");
		HearthstoneMod.hearthstone = new ItemHearthstone();
		ForgeRegistries.ITEMS.register(hearthstone);
		
		logger.info("Registering sound events");
		HearthstoneMod.channelSoundEvent = new SoundEvent(new ResourceLocation("hearthstonemod", "hearthstonechannel")).setRegistryName("hearthstoneChannel");
		HearthstoneMod.castSoundEvent = new SoundEvent(new ResourceLocation("hearthstonemod", "hearthstonecast")).setRegistryName("hearthstoneCast");
		HearthstoneMod.impactSoundEvent = new SoundEvent(new ResourceLocation("hearthstonemod", "hearthstoneimpact")).setRegistryName("hearthstoneImpact");
		ForgeRegistries.SOUND_EVENTS.register(channelSoundEvent);
		ForgeRegistries.SOUND_EVENTS.register(castSoundEvent);
		ForgeRegistries.SOUND_EVENTS.register(impactSoundEvent);
		
		logger.info("Registering event handler");
		MinecraftForge.EVENT_BUS.register(new HearthstoneEventHandler());
		
		proxy.preInit(event);
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		logger.info("Initializing");
		
		logger.info("Registering recipies");
		ResourceLocation noGroup = new ResourceLocation("");
		IRecipe hearthstoneRecipe;
		// @formatter:off
		if(easyRecipe)
		{
			hearthstoneRecipe = new ShapedOreRecipe(noGroup, new ItemStack(hearthstone), new Object[] {
					"SLS",
					"LCL",
					"SLS",
					'S', "stone",
					'L', "gemLapis",
					'C', new ItemStack(Items.COMPASS) });
		}
		else
		{
			hearthstoneRecipe = new ShapedOreRecipe(noGroup, new ItemStack(hearthstone), new Object[] {
					"SDS",
					"DED",
					"SDS",
					'S', "stone",
					'D', "gemDiamond",
					'E', "enderpearl" });
		}
		// @formatter:on
		hearthstoneRecipe.setRegistryName(new ResourceLocation(this.MODID + ":hearthstone_recipe"));
		GameData.register_impl(hearthstoneRecipe);
		
		proxy.init(event);
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		proxy.postInit(event);
	}
}