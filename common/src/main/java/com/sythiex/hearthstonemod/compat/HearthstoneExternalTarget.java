package com.sythiex.hearthstonemod.compat;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A persistent destination reference that remains readable without its provider.
 */
public record HearthstoneExternalTarget(ResourceLocation providerId, UUID trackingPointId)
{
    public static final Codec<HearthstoneExternalTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("provider").forGetter(HearthstoneExternalTarget::providerId),
            UUIDUtil.CODEC.fieldOf("tracking_point").forGetter(HearthstoneExternalTarget::trackingPointId)).apply(instance, HearthstoneExternalTarget::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HearthstoneExternalTarget> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, HearthstoneExternalTarget::providerId, UUIDUtil.STREAM_CODEC, HearthstoneExternalTarget::trackingPointId,
            HearthstoneExternalTarget::new);
}
