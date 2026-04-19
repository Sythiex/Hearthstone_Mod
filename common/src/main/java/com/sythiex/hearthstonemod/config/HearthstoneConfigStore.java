package com.sythiex.hearthstonemod.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HearthstoneConfigStore
{
    private final Path path;
    private HearthstoneConfigData currentData;

    private HearthstoneConfigStore(Path path, HearthstoneConfigData currentData)
    {
        this.path = path;
        this.currentData = currentData;
    }

    public static HearthstoneConfigStore load(Path configDirectory) throws IOException
    {
        Files.createDirectories(configDirectory);

        Path configPath = configDirectory.resolve(HearthstoneConfigData.FILE_NAME);
        HearthstoneConfigData loadedData = Files.exists(configPath) ? read(configPath) : HearthstoneConfigData.defaults();
        HearthstoneConfigStore store = new HearthstoneConfigStore(configPath, loadedData.sanitize());
        store.persist();
        return store;
    }

    public synchronized HearthstoneConfigData get()
    {
        return currentData;
    }

    public synchronized void save(HearthstoneConfigData updatedData) throws IOException
    {
        currentData = updatedData.sanitize();
        persist();
    }

    public Path path()
    {
        return path;
    }

    private static HearthstoneConfigData read(Path configPath) throws IOException
    {
        Map<String, String> rawValues = new LinkedHashMap<>();

        for (String line : Files.readAllLines(configPath, StandardCharsets.UTF_8))
        {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#"))
            {
                continue;
            }

            int separatorIndex = trimmedLine.indexOf('=');
            if (separatorIndex < 0)
            {
                continue;
            }

            String key = trimmedLine.substring(0, separatorIndex).trim();
            String value = trimmedLine.substring(separatorIndex + 1).trim();
            rawValues.put(key, value);
        }

        HearthstoneConfigData defaults = HearthstoneConfigData.defaults();

        int channelTime = parseInt(rawValues.get(HearthstoneConfigData.CHANNEL_TIME_KEY), defaults.channelTime());
        int cooldown = parseInt(rawValues.get(HearthstoneConfigData.COOLDOWN_KEY), defaults.cooldown());
        boolean allowCrossDimensionTravel = parseBoolean(rawValues.get(HearthstoneConfigData.ALLOW_CROSS_DIMENSION_TRAVEL_KEY), defaults.allowCrossDimensionTravel());
        boolean allowRespawnAnchorLinking = parseBoolean(rawValues.get(HearthstoneConfigData.ALLOW_RESPAWN_ANCHOR_LINKING_KEY), defaults.allowRespawnAnchorLinking());
        boolean consumeRespawnAnchorCharge = parseBoolean(rawValues.get(HearthstoneConfigData.CONSUME_RESPAWN_ANCHOR_CHARGE_KEY), defaults.consumeRespawnAnchorCharge());
        List<String> dimensionBlacklist = parseStringArray(rawValues.get(HearthstoneConfigData.DIMENSION_BLACKLIST_KEY));

        return new HearthstoneConfigData(channelTime, cooldown, allowCrossDimensionTravel, allowRespawnAnchorLinking, consumeRespawnAnchorCharge, dimensionBlacklist);
    }

    private static int parseInt(String rawValue, int fallback)
    {
        if (rawValue == null)
        {
            return fallback;
        }

        try
        {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception)
        {
            return fallback;
        }
    }

    private static boolean parseBoolean(String rawValue, boolean fallback)
    {
        if (rawValue == null)
        {
            return fallback;
        }

        if ("true".equalsIgnoreCase(rawValue))
        {
            return true;
        }

        if ("false".equalsIgnoreCase(rawValue))
        {
            return false;
        }

        return fallback;
    }

    private static List<String> parseStringArray(String rawValue)
    {
        if (rawValue == null || rawValue.isBlank())
        {
            return List.of();
        }

        String trimmed = rawValue.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]"))
        {
            return List.of();
        }

        String contents = trimmed.substring(1, trimmed.length() - 1).trim();
        if (contents.isEmpty())
        {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < contents.length(); i++)
        {
            char currentChar = contents.charAt(i);

            if (escaping)
            {
                currentValue.append(currentChar);
                escaping = false;
                continue;
            }

            if (currentChar == '\\')
            {
                escaping = true;
                continue;
            }

            if (currentChar == '"')
            {
                if (inString)
                {
                    values.add(currentValue.toString());
                    currentValue.setLength(0);
                }

                inString = !inString;
                continue;
            }

            if (inString)
            {
                currentValue.append(currentChar);
            }
        }

        return List.copyOf(values);
    }

    private synchronized void persist() throws IOException
    {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();

        appendCommentedEntry(builder, "How long it takes for the hearthstone to teleport you. Measured in ticks (20 ticks = 1 second). Default: 200 ticks (10 seconds)",
                HearthstoneConfigData.CHANNEL_TIME_KEY, Integer.toString(currentData.channelTime()), lineSeparator);
        appendCommentedEntry(builder, "How long you must wait between hearthstone uses. Measured in ticks (20 ticks = 1 second). Default: 36000 ticks (30 minutes)",
                HearthstoneConfigData.COOLDOWN_KEY, Integer.toString(currentData.cooldown()), lineSeparator);
        appendCommentedEntry(builder, "Whether the hearthstone can teleport you between dimensions. Default: true", HearthstoneConfigData.ALLOW_CROSS_DIMENSION_TRAVEL_KEY,
                Boolean.toString(currentData.allowCrossDimensionTravel()), lineSeparator);
        appendCommentedEntry(builder, "Whether the hearthstone can link to respawn anchors. Default: true", HearthstoneConfigData.ALLOW_RESPAWN_ANCHOR_LINKING_KEY,
                Boolean.toString(currentData.allowRespawnAnchorLinking()), lineSeparator);
        appendCommentedEntry(builder, "Whether successful hearthstone teleports should consume a charge from linked respawn anchors. Default: false",
                HearthstoneConfigData.CONSUME_RESPAWN_ANCHOR_CHARGE_KEY, Boolean.toString(currentData.consumeRespawnAnchorCharge()), lineSeparator);
        appendCommentedEntry(builder, "A list of dimension ids in which the hearthstone is disabled. Example: [\"minecraft:the_nether\", \"minecraft:the_end\"]",
                HearthstoneConfigData.DIMENSION_BLACKLIST_KEY, serializeStringArray(currentData.dimensionBlacklist()), lineSeparator);
        builder.append(lineSeparator);

        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
    }

    private static void appendCommentedEntry(StringBuilder builder, String comment, String key, String value, String lineSeparator)
    {
        builder.append('\t').append('#').append(comment).append(lineSeparator);
        builder.append('\t').append(key).append(" = ").append(value).append(lineSeparator);
    }

    private static String serializeStringArray(List<String> values)
    {
        if (values.isEmpty())
        {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++)
        {
            if (i > 0)
            {
                builder.append(", ");
            }

            builder.append('"').append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        builder.append(']');
        return builder.toString();
    }
}
