package com.sythiex.hearthstonemod.fabric;

import com.sythiex.hearthstonemod.content.HearthstoneContentFactory;
import com.sythiex.hearthstonemod.content.HearthstoneContentIds;
import com.sythiex.hearthstonemod.content.HearthstoneDataComponents;
import com.sythiex.hearthstonemod.content.HearthstoneItemData;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvent;

/**
 * Fabric-side registrations for items, sounds, and data components.
 */
public final class HearthstoneFabricRegistries
{
    public static final DataComponentType<HearthstoneItemData> HEARTHSTONE_ITEM_DATA = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            HearthstoneContentIds.HEARTHSTONE_ITEM_DATA, HearthstoneDataComponents.createType());
    public static final Item HEARTHSTONE = Registry.register(BuiltInRegistries.ITEM, HearthstoneContentIds.HEARTHSTONE, HearthstoneContentFactory.createHearthstoneItem());
    public static final SoundEvent HEARTHSTONE_CAST = Registry.register(BuiltInRegistries.SOUND_EVENT, HearthstoneContentIds.HEARTHSTONE_CAST_SOUND,
            HearthstoneContentFactory.createVariableRangeSound(HearthstoneContentIds.HEARTHSTONE_CAST_SOUND));
    public static final SoundEvent HEARTHSTONE_IMPACT = Registry.register(BuiltInRegistries.SOUND_EVENT, HearthstoneContentIds.HEARTHSTONE_IMPACT_SOUND,
            HearthstoneContentFactory.createVariableRangeSound(HearthstoneContentIds.HEARTHSTONE_IMPACT_SOUND));
    public static final SoundEvent HEARTHSTONE_CHANNEL = Registry.register(BuiltInRegistries.SOUND_EVENT, HearthstoneContentIds.HEARTHSTONE_CHANNEL_SOUND,
            HearthstoneContentFactory.createVariableRangeSound(HearthstoneContentIds.HEARTHSTONE_CHANNEL_SOUND));

    private HearthstoneFabricRegistries()
    {}

    /**
     * Registers creative tab placement after the static registry entries are
     * created.
     */
    public static void register()
    {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(HEARTHSTONE));
    }
}
