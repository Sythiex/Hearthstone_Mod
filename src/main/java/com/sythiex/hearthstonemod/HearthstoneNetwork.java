package com.sythiex.hearthstonemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Lightweight networking used to mirror cast start/stop state to clients.
 */
public final class HearthstoneNetwork
{
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(HearthstoneMod.MODID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextPacketId = 0;

    private HearthstoneNetwork()
    {}

    /**
     * Registers the packet types used by the mod's network channel.
     */
    public static void register()
    {
        CHANNEL.registerMessage(nextPacketId++, SyncHearthstoneCastingPacket.class, SyncHearthstoneCastingPacket::encode, SyncHearthstoneCastingPacket::decode, SyncHearthstoneCastingPacket::handle);
    }

    /**
     * Broadcasts a player's cast state to that player and any clients currently
     * tracking them.
     *
     * @param player    player whose cast state changed
     * @param isCasting {@code true} when casting started, {@code false} when it
     *                  ended
     */
    public static void sendCastingState(ServerPlayer player, boolean isCasting)
    {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncHearthstoneCastingPacket(player.getId(), isCasting));
    }
}
