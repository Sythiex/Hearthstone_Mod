package com.sythiex.hearthstonemod.content;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

/**
 * Shared content constructors used by both loader-specific registries.
 */
public final class HearthstoneContentFactory
{
    private HearthstoneContentFactory()
    {}

    /**
     * Base item properties shared by NeoForge and Fabric registrations.
     */
    public static Item.Properties createHearthstoneProperties()
    {
        return new Item.Properties().stacksTo(1);
    }

    public static Item createHearthstoneItem()
    {
        return new ItemHearthstone(createHearthstoneProperties());
    }

    /**
     * Creates sound events that use vanilla's distance-based attenuation.
     */
    public static SoundEvent createVariableRangeSound(net.minecraft.resources.ResourceLocation id)
    {
        return SoundEvent.createVariableRangeEvent(id);
    }
}
