package com.sythiex.hearthstonemod.content;

import java.util.Optional;

import com.sythiex.hearthstonemod.compat.HearthstoneExternalTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Per-stack home link describing where a Hearthstone should teleport.
 */
public record HearthstoneHomeLink(BlockPos homePos, ResourceLocation dimensionId, HearthstoneLinkType linkType, Optional<HearthstoneExternalTarget> externalTarget)
{
    public static final Codec<HearthstoneHomeLink> CODEC = RecordCodecBuilder
            .create(instance -> instance.group(BlockPos.CODEC.fieldOf("home").forGetter(HearthstoneHomeLink::homePos),
                    ResourceLocation.CODEC.fieldOf("dimension").forGetter(HearthstoneHomeLink::dimensionId),
                    HearthstoneLinkType.CODEC.fieldOf("type").forGetter(HearthstoneHomeLink::linkType),
                    HearthstoneExternalTarget.CODEC.optionalFieldOf("external_target").forGetter(HearthstoneHomeLink::externalTarget)).apply(instance, HearthstoneHomeLink::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HearthstoneHomeLink> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, HearthstoneHomeLink::homePos,
            ResourceLocation.STREAM_CODEC, HearthstoneHomeLink::dimensionId, HearthstoneLinkType.STREAM_CODEC, HearthstoneHomeLink::linkType,
            ByteBufCodecs.optional(HearthstoneExternalTarget.STREAM_CODEC), HearthstoneHomeLink::externalTarget, HearthstoneHomeLink::new);

    /**
     * Creates a legacy-compatible ordinary world link.
     */
    public HearthstoneHomeLink(BlockPos homePos, ResourceLocation dimensionId, HearthstoneLinkType linkType)
    {
        this(homePos, dimensionId, linkType, Optional.empty());
    }

    /**
     * Converts the stored dimension id back into a runtime dimension key.
     */
    public ResourceKey<Level> dimensionKey()
    {
        return ResourceKey.create(Registries.DIMENSION, this.dimensionId);
    }
}
