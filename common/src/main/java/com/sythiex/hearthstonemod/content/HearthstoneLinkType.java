package com.sythiex.hearthstonemod.content;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum HearthstoneLinkType implements StringRepresentable {
    BED("bed"),
    RESPAWN_ANCHOR("respawn_anchor");

    public static final Codec<HearthstoneLinkType> CODEC = StringRepresentable.fromEnum(HearthstoneLinkType::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, HearthstoneLinkType> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeUtf(value.getSerializedName()),
            buffer -> fromSerializedName(buffer.readUtf())
    );

    private final String serializedName;

    HearthstoneLinkType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public static HearthstoneLinkType fromSerializedName(String serializedName) {
        for (HearthstoneLinkType value : values()) {
            if (value.serializedName.equals(serializedName)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown Hearthstone link type: " + serializedName);
    }
}
