package com.sythiex.hearthstonemod;

import java.util.function.Supplier;

import com.sythiex.hearthstonemod.client.HearthstoneClientCasting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Tells clients when a player begins or ends channeling so visuals and audio
 * can stay in sync with the server.
 */
public class SyncHearthstoneCastingPacket
{
    private final int entityId;
    private final boolean isCasting;

    /**
     * Creates a cast-state sync packet for a single entity.
     *
     * @param entityId  runtime entity id to update on the client
     * @param isCasting {@code true} when the entity started casting, {@code false}
     *                  when it stopped
     */
    public SyncHearthstoneCastingPacket(int entityId, boolean isCasting)
    {
        this.entityId = entityId;
        this.isCasting = isCasting;
    }

    /**
     * Writes this packet's fields to the network buffer.
     *
     * @param packet packet to encode
     * @param buffer destination buffer
     */
    public static void encode(SyncHearthstoneCastingPacket packet, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.isCasting);
    }

    /**
     * Rebuilds a packet from the network buffer.
     *
     * @param buffer source buffer
     * @return decoded packet instance
     */
    public static SyncHearthstoneCastingPacket decode(FriendlyByteBuf buffer)
    {
        return new SyncHearthstoneCastingPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    /**
     * Schedules client-side handling for a cast-state update.
     *
     * @param packet          decoded packet payload
     * @param contextSupplier network context supplier
     */
    public static void handle(SyncHearthstoneCastingPacket packet, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HearthstoneClientCasting.handleSync(packet.entityId, packet.isCasting)));
        context.setPacketHandled(true);
    }
}
