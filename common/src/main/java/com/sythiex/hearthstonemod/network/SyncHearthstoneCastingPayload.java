package com.sythiex.hearthstonemod.network;

import com.sythiex.hearthstonemod.HearthstoneConstants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Clientbound payload that tells clients whether an entity is actively
 * channeling.
 */
public record SyncHearthstoneCastingPayload(int entityId, boolean casting) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SyncHearthstoneCastingPayload> TYPE = new CustomPacketPayload.Type<>(HearthstoneConstants.id("sync_casting"));
    public static final StreamCodec<ByteBuf, SyncHearthstoneCastingPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,
            SyncHearthstoneCastingPayload::entityId, ByteBufCodecs.BOOL, SyncHearthstoneCastingPayload::casting, SyncHearthstoneCastingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
