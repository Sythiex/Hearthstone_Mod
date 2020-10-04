package com.sythiex.hearthstonemod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class HearthstoneConfig
{
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final HearthstoneSettings SETTINGS = new HearthstoneSettings(BUILDER);
	public static final ForgeConfigSpec SPEC = BUILDER.build();
	
	public static class HearthstoneSettings
	{
		public static ConfigValue<Integer> channelTime; // default 200
		public static ConfigValue<Integer> cooldown; // default 36000
		
		HearthstoneSettings(ForgeConfigSpec.Builder builder)
		{
			builder.push("Settings");
			
			channelTime = builder.comment("How long it takes for the hearthstone to teleport you. Measured in ticks (20 ticks = 1 second). Default: 200 ticks (10 seconds)").translation("config.hearthstone.channel_time").define("ChannelTime", 200);
			cooldown = builder.comment("How long you must wait between hearthstone uses. Measured in ticks (20 ticks = 1 second). Default: 36000 ticks (30 minutes)").translation("config.hearthstone.cooldown").define("Cooldown", 36000);
			
			builder.pop();
		}
	}
}