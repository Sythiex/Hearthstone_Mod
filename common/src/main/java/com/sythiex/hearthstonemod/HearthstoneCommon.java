package com.sythiex.hearthstonemod;

import java.io.IOException;
import java.nio.file.Path;

import com.sythiex.hearthstonemod.client.HearthstoneClientBridge;
import com.sythiex.hearthstonemod.config.HearthstoneConfigData;
import com.sythiex.hearthstonemod.config.HearthstoneConfigStore;
import com.sythiex.hearthstonemod.network.HearthstoneNetworkBridge;

import net.minecraft.world.level.Level;

public final class HearthstoneCommon {
    private static HearthstoneConfigStore configStore;
    private static HearthstoneNetworkBridge networkBridge = HearthstoneNetworkBridge.noop();
    private static HearthstoneClientBridge clientBridge = HearthstoneClientBridge.noop();

    private HearthstoneCommon() {
    }

    public static synchronized void bootstrap(Path configDirectory, HearthstoneNetworkBridge bridge) {
        if (configStore == null) {
            try {
                configStore = HearthstoneConfigStore.load(configDirectory);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to initialize Hearthstone config store", exception);
            }
        }

        networkBridge = bridge == null ? HearthstoneNetworkBridge.noop() : bridge;
    }

    public static synchronized HearthstoneConfigStore configStore() {
        if (configStore == null) {
            throw new IllegalStateException("HearthstoneCommon.bootstrap must be called before accessing the config store.");
        }

        return configStore;
    }

    public static HearthstoneConfigData config() {
        return configStore().get();
    }

    public static synchronized HearthstoneNetworkBridge networkBridge() {
        return networkBridge;
    }

    public static synchronized void setClientBridge(HearthstoneClientBridge bridge) {
        clientBridge = bridge == null ? HearthstoneClientBridge.noop() : bridge;
    }

    public static synchronized HearthstoneClientBridge clientBridge() {
        return clientBridge;
    }

    public static boolean isDimensionBlacklisted(Level level) {
        return config().dimensionBlacklist().contains(level.dimension().location().toString());
    }
}
