package com.sythiex.hearthstonemod.content;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Typed per-stack state for the Hearthstone item.
 */
public record HearthstoneItemData(Optional<HearthstoneHomeLink> homeLink, long lastUsedGameTime)
{
    public static final HearthstoneItemData DEFAULT = new HearthstoneItemData(Optional.empty(), -1L);
    public static final Codec<HearthstoneItemData> CODEC = RecordCodecBuilder
            .create(instance -> instance
                    .group(HearthstoneHomeLink.CODEC.optionalFieldOf("home").forGetter(HearthstoneItemData::homeLink),
                            Codec.LONG.optionalFieldOf("last_used_game_time", -1L).forGetter(HearthstoneItemData::lastUsedGameTime))
                    .apply(instance, HearthstoneItemData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HearthstoneItemData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(HearthstoneHomeLink.STREAM_CODEC), HearthstoneItemData::homeLink, ByteBufCodecs.VAR_LONG, HearthstoneItemData::lastUsedGameTime,
            HearthstoneItemData::new);

    /**
     * Returns a copy with a new home link while preserving cooldown state.
     */
    public HearthstoneItemData withHomeLink(HearthstoneHomeLink link)
    {
        return new HearthstoneItemData(Optional.of(link), this.lastUsedGameTime);
    }

    /**
     * Returns a copy with the link removed.
     */
    public HearthstoneItemData clearHomeLink()
    {
        return new HearthstoneItemData(Optional.empty(), this.lastUsedGameTime);
    }

    /**
     * Returns a copy with the latest successful-use timestamp.
     */
    public HearthstoneItemData withLastUsedGameTime(long gameTime)
    {
        return new HearthstoneItemData(this.homeLink, gameTime);
    }
}
