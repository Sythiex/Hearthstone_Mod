package com.sythiex.hearthstonemod;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

/**
 * Common config spec and helpers used by both gameplay code and the client
 * config screen.
 */
public class HearthstoneConfig
{
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final HearthstoneSettings SETTINGS = new HearthstoneSettings(BUILDER);
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    /**
     * Backing config values for the Hearthstone's gameplay behavior.
     */
    public static class HearthstoneSettings
    {
        public static ConfigValue<Integer> channelTime;
        public static ConfigValue<Integer> cooldown;
        public static ConfigValue<Boolean> allowCrossDimensionTravel;
        public static ConfigValue<Boolean> allowRespawnAnchorLinking;
        public static ConfigValue<Boolean> consumeRespawnAnchorCharge;
        public static ConfigValue<List<? extends String>> dimensionBlacklist;

        HearthstoneSettings(ForgeConfigSpec.Builder builder)
        {
            builder.push("Settings");

            channelTime = builder.comment("How long it takes for the hearthstone to teleport you. Measured in ticks (20 ticks = 1 second). Default: 200 ticks (10 seconds)")
                    .translation("config.hearthstone.channel_time").define("ChannelTime", 200);
            cooldown = builder.comment("How long you must wait between hearthstone uses. Measured in ticks (20 ticks = 1 second). Default: 36000 ticks (30 minutes)")
                    .translation("config.hearthstone.cooldown").define("Cooldown", 36000);
            allowCrossDimensionTravel = builder.comment("Whether the hearthstone can teleport you between dimensions. Default: true").translation("config.hearthstone.allow_cross_dimension_travel")
                    .define("AllowCrossDimensionTravel", true);
            allowRespawnAnchorLinking = builder.comment("Whether the hearthstone can link to respawn anchors. Default: true").translation("config.hearthstone.allow_respawn_anchor_linking")
                    .define("AllowRespawnAnchorLinking", true);
            consumeRespawnAnchorCharge = builder.comment("Whether successful hearthstone teleports should consume a charge from linked respawn anchors. Default: false")
                    .translation("config.hearthstone.consume_respawn_anchor_charge").define("ConsumeRespawnAnchorCharge", false);
            dimensionBlacklist = builder.comment("A list of dimension ids in which the hearthstone is disabled. Example: [\"minecraft:the_nether\", \"minecraft:the_end\"]")
                    .translation("config.hearthstone.dimBlacklist").defineListAllowEmpty("DimensionBlacklist", List.of(), HearthstoneConfig::isValidDimensionId);

            builder.pop();
        }
    }

    /**
     * Checks whether the given world's dimension is disabled by config.
     *
     * @param world world whose dimension should be checked
     * @return {@code true} if the world's dimension id appears in the blacklist
     */
    public static boolean isDimensionBlacklisted(Level world)
    {
        return HearthstoneSettings.dimensionBlacklist.get().contains(world.dimension().location().toString());
    }

    /**
     * Validates a single dimension id entry from the config list.
     *
     * @param value raw config value to validate
     * @return {@code true} when the value is a parseable dimension id
     */
    private static boolean isValidDimensionId(Object value)
    {
        return value instanceof String string && ResourceLocation.tryParse(string) != null;
    }
}
