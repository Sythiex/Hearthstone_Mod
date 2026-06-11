package com.sythiex.hearthstonemod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(HearthstoneMod.MODID)
public class HearthstoneMod
{
    public static final String MODID = "hearthstonemod";
    public static final String NAME = "Hearthstone Mod";
    public static final String VERSION = "2.0.0";

    public static Logger logger = LogManager.getLogger(MODID);

    public HearthstoneMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        ModSoundEvents.register(modEventBus);
        HearthstoneNetwork.register();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.sythiex.hearthstonemod.client.HearthstoneClientConfig.register());

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, com.sythiex.hearthstonemod.HearthstoneConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(new HearthstoneEventHandler());
    }
}
