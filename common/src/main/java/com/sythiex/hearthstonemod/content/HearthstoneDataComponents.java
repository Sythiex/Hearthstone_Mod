package com.sythiex.hearthstonemod.content;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class HearthstoneDataComponents {
    private HearthstoneDataComponents() {
    }

    public static DataComponentType<HearthstoneItemData> createType() {
        return DataComponentType.<HearthstoneItemData>builder()
                .persistent(HearthstoneItemData.CODEC)
                .networkSynchronized(HearthstoneItemData.STREAM_CODEC)
                .cacheEncoding()
                .build();
    }

    @SuppressWarnings("unchecked")
    public static DataComponentType<HearthstoneItemData> hearthstoneData() {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(HearthstoneContentIds.HEARTHSTONE_ITEM_DATA);
        if (type == null) {
            throw new IllegalStateException("Missing Hearthstone item data component registration.");
        }

        return (DataComponentType<HearthstoneItemData>) type;
    }
}
