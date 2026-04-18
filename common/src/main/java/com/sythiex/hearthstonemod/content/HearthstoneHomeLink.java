package com.sythiex.hearthstonemod.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record HearthstoneHomeLink(BlockPos homePos, ResourceLocation dimensionId, HearthstoneLinkType linkType) {
    public static final Codec<HearthstoneHomeLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("home").forGetter(HearthstoneHomeLink::homePos),
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(HearthstoneHomeLink::dimensionId),
            HearthstoneLinkType.CODEC.fieldOf("type").forGetter(HearthstoneHomeLink::linkType)
    ).apply(instance, HearthstoneHomeLink::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HearthstoneHomeLink> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            HearthstoneHomeLink::homePos,
            ResourceLocation.STREAM_CODEC,
            HearthstoneHomeLink::dimensionId,
            HearthstoneLinkType.STREAM_CODEC,
            HearthstoneHomeLink::linkType,
            HearthstoneHomeLink::new
    );

    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, this.dimensionId);
    }
}
