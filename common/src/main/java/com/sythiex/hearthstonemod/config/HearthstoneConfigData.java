package com.sythiex.hearthstonemod.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Shared Hearthstone gameplay settings loaded from the common TOML file.
 */
public record HearthstoneConfigData(int channelTime, int cooldown, boolean allowCrossDimensionTravel, boolean allowRespawnAnchorLinking, boolean consumeRespawnAnchorCharge,
        List<String> dimensionBlacklist)
{
    public static final String FILE_NAME = "hearthstonemod-common.toml";
    public static final String CHANNEL_TIME_KEY = "ChannelTime";
    public static final String COOLDOWN_KEY = "Cooldown";
    public static final String ALLOW_CROSS_DIMENSION_TRAVEL_KEY = "AllowCrossDimensionTravel";
    public static final String ALLOW_RESPAWN_ANCHOR_LINKING_KEY = "AllowRespawnAnchorLinking";
    public static final String CONSUME_RESPAWN_ANCHOR_CHARGE_KEY = "ConsumeRespawnAnchorCharge";
    public static final String DIMENSION_BLACKLIST_KEY = "DimensionBlacklist";

    /**
     * Default values used when the config file is missing or invalid.
     */
    public static HearthstoneConfigData defaults()
    {
        return new HearthstoneConfigData(200, 36000, true, true, false, List.of());
    }

    /**
     * Clamps numeric values and removes invalid or duplicate dimension ids.
     */
    public HearthstoneConfigData sanitize()
    {
        int sanitizedChannelTime = Math.max(1, channelTime);
        int sanitizedCooldown = Math.max(0, cooldown);
        Set<String> sanitizedDimensions = new LinkedHashSet<>();

        for (String dimensionId : dimensionBlacklist)
        {
            String trimmed = dimensionId.trim();
            if (!trimmed.isEmpty() && isValidDimensionId(trimmed))
            {
                sanitizedDimensions.add(trimmed);
            }
        }

        return new HearthstoneConfigData(sanitizedChannelTime, sanitizedCooldown, allowCrossDimensionTravel, allowRespawnAnchorLinking, consumeRespawnAnchorCharge,
                List.copyOf(sanitizedDimensions));
    }

    /**
     * Returns whether the value can be parsed as a valid resource location.
     */
    public static boolean isValidDimensionId(String value)
    {
        return ResourceLocation.tryParse(value) != null;
    }

    public boolean isDimensionBlacklisted(Level level)
    {
        return dimensionBlacklist.contains(level.dimension().location().toString());
    }

    /**
     * Parses a comma-delimited list as used by the config screen text field.
     */
    public static List<String> parseDimensionList(String rawValue)
    {
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty())
        {
            return List.of();
        }

        List<String> parsedValues = new ArrayList<>();
        for (String part : rawValue.split(","))
        {
            String dimensionId = part.trim();
            if (!dimensionId.isEmpty())
            {
                parsedValues.add(dimensionId);
            }
        }

        return parsedValues;
    }
}
