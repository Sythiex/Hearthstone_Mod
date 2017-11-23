package com.sythiex.hearthstonemod.proxy;

import com.sythiex.hearthstonemod.HearthstoneMod;
import com.sythiex.hearthstonemod.ItemHearthstone;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy
{	
	@Override
	public void preInit(FMLPreInitializationEvent event)
	{
		super.preInit(event);
		HearthstoneMod.logger.info("Loading item models");
		ModelLoader.setCustomModelResourceLocation(HearthstoneMod.hearthstone, 0, new ModelResourceLocation(HearthstoneMod.MODID + ":" + ItemHearthstone.name, "inventory"));
	}
}