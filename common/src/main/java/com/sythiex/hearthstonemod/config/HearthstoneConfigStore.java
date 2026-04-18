package com.sythiex.hearthstonemod.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HearthstoneConfigStore {
    private final Path path;
    private HearthstoneConfigData currentData;

    private HearthstoneConfigStore(Path path, HearthstoneConfigData currentData) {
        this.path = path;
        this.currentData = currentData;
    }

    public static HearthstoneConfigStore load(Path configDirectory) throws IOException {
        Files.createDirectories(configDirectory);

        Path configPath = configDirectory.resolve(HearthstoneConfigData.FILE_NAME);
        HearthstoneConfigData loadedData = Files.exists(configPath) ? read(configPath) : HearthstoneConfigData.defaults();
        HearthstoneConfigStore store = new HearthstoneConfigStore(configPath, loadedData.sanitize());
        store.persist();
        return store;
    }

    public synchronized HearthstoneConfigData get() {
        return currentData;
    }

    public synchronized void save(HearthstoneConfigData updatedData) throws IOException {
        currentData = updatedData.sanitize();
        persist();
    }

    public Path path() {
        return path;
    }

    private static HearthstoneConfigData read(Path configPath) throws IOException {
        Map<String, String> rawValues = new LinkedHashMap<>();

        for (String line : Files.readAllLines(configPath, StandardCharsets.UTF_8)) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            int separatorIndex = trimmedLine.indexOf('=');
            if (separatorIndex < 0) {
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

        return new HearthstoneConfigData(
                channelTime,
                cooldown,
                allowCrossDimensionTravel,
                allowRespawnAnchorLinking,
                consumeRespawnAnchorCharge,
                dimensionBlacklist
        );
    }

    private static int parseInt(String rawValue, int fallback) {
        if (rawValue == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String rawValue, boolean fallback) {
        if (rawValue == null) {
            return fallback;
        }

        if ("true".equalsIgnoreCase(rawValue)) {
            return true;
        }

        if ("false".equalsIgnoreCase(rawValue)) {
            return false;
        }

        return fallback;
    }

    private static List<String> parseStringArray(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        String trimmed = rawValue.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of();
        }

        String contents = trimmed.substring(1, trimmed.length() - 1).trim();
        if (contents.isEmpty()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < contents.length(); i++) {
            char currentChar = contents.charAt(i);

            if (escaping) {
                currentValue.append(currentChar);
                escaping = false;
                continue;
            }

            if (currentChar == '\\') {
                escaping = true;
                continue;
            }

            if (currentChar == '"') {
                if (inString) {
                    values.add(currentValue.toString());
                    currentValue.setLength(0);
                }

                inString = !inString;
                continue;
            }

            if (inString) {
                currentValue.append(currentChar);
            }
        }

        return List.copyOf(values);
    }

    private synchronized void persist() throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("# Hearthstone shared gameplay settings").append(System.lineSeparator());
        builder.append("# This file is used by both the NeoForge and Fabric builds.").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append(HearthstoneConfigData.CHANNEL_TIME_KEY).append(" = ").append(currentData.channelTime()).append(System.lineSeparator());
        builder.append(HearthstoneConfigData.COOLDOWN_KEY).append(" = ").append(currentData.cooldown()).append(System.lineSeparator());
        builder.append(HearthstoneConfigData.ALLOW_CROSS_DIMENSION_TRAVEL_KEY).append(" = ").append(currentData.allowCrossDimensionTravel()).append(System.lineSeparator());
        builder.append(HearthstoneConfigData.ALLOW_RESPAWN_ANCHOR_LINKING_KEY).append(" = ").append(currentData.allowRespawnAnchorLinking()).append(System.lineSeparator());
        builder.append(HearthstoneConfigData.CONSUME_RESPAWN_ANCHOR_CHARGE_KEY).append(" = ").append(currentData.consumeRespawnAnchorCharge()).append(System.lineSeparator());
        builder.append(HearthstoneConfigData.DIMENSION_BLACKLIST_KEY).append(" = ").append(serializeStringArray(currentData.dimensionBlacklist())).append(System.lineSeparator());

        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
    }

    private static String serializeStringArray(List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }

            builder.append('"').append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        builder.append(']');
        return builder.toString();
    }
}
