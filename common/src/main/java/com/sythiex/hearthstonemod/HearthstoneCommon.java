package com.sythiex.hearthstonemod;

import java.io.IOException;
import java.nio.file.Path;

import com.sythiex.hearthstonemod.config.HearthstoneConfigData;
import com.sythiex.hearthstonemod.config.HearthstoneConfigStore;
import com.sythiex.hearthstonemod.network.HearthstoneNetworkBridge;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationBridge;

import net.minecraft.world.level.Level;

/**
 * Shared bootstrap state for configuration, networking, and client hooks.
 */
public final class HearthstoneCommon
{
    private static HearthstoneConfigStore configStore;
    private static HearthstoneNetworkBridge networkBridge = HearthstoneNetworkBridge.noop();
    private static HearthstoneClientBridge clientBridge = HearthstoneClientBridge.noop();
    private static HearthstoneDestinationBridge destinationBridge = HearthstoneDestinationBridge.DEFAULT;

    private HearthstoneCommon()
    {}

    /**
     * Initializes the shared config store and installs the active network bridge.
     */
    public static synchronized void bootstrap(Path configDirectory, HearthstoneNetworkBridge bridge)
    {
        if (configStore == null)
        {
            try
            {
                configStore = HearthstoneConfigStore.load(configDirectory);
            } catch (IOException exception)
            {
                throw new IllegalStateException("Unable to initialize Hearthstone config store", exception);
            }
        }

        networkBridge = bridge == null ? HearthstoneNetworkBridge.noop() : bridge;
    }

    public static synchronized HearthstoneConfigStore configStore()
    {
        if (configStore == null)
        {
            throw new IllegalStateException("HearthstoneCommon.bootstrap must be called before accessing the config store.");
        }

        return configStore;
    }

    public static HearthstoneConfigData config()
    {
        return configStore().get();
    }

    /**
     * Returns the loader bridge used to broadcast cast-state updates.
     */
    public static synchronized HearthstoneNetworkBridge networkBridge()
    {
        return networkBridge;
    }

    /**
     * Installs the client bridge after client bootstrap completes.
     */
    public static synchronized void setClientBridge(HearthstoneClientBridge bridge)
    {
        clientBridge = bridge == null ? HearthstoneClientBridge.noop() : bridge;
    }

    public static synchronized HearthstoneClientBridge clientBridge()
    {
        return clientBridge;
    }

    /**
     * Installs an optional loader-specific destination provider at common setup.
     */
    public static void setDestinationBridge(HearthstoneDestinationBridge bridge)
    {
        destinationBridge = bridge == null ? HearthstoneDestinationBridge.DEFAULT : bridge;
    }

    public static HearthstoneDestinationBridge destinationBridge()
    {
        return destinationBridge;
    }

    /**
     * Shared convenience check used by item logic and config UI.
     */
    public static boolean isDimensionBlacklisted(Level level)
    {
        return config().dimensionBlacklist().contains(level.dimension().location().toString());
    }
}
