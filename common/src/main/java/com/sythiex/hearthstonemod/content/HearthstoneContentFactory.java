package com.sythiex.hearthstonemod.content;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

public final class HearthstoneContentFactory {
    private HearthstoneContentFactory() {
    }

    public static Item.Properties createHearthstoneProperties() {
        return new Item.Properties().stacksTo(1);
    }

    public static Item createHearthstoneItem() {
        return new ItemHearthstone(createHearthstoneProperties());
    }

    public static SoundEvent createVariableRangeSound(net.minecraft.resources.ResourceLocation id) {
        return SoundEvent.createVariableRangeEvent(id);
    }
}
