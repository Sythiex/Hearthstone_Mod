package com.sythiex.hearthstonemod.neoforge;

import java.util.function.Supplier;

import com.sythiex.hearthstonemod.HearthstoneConstants;
import com.sythiex.hearthstonemod.content.HearthstoneContentIds;
import com.sythiex.hearthstonemod.content.HearthstoneItemData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge registry declarations for shared Hearthstone content.
 */
public final class HearthstoneNeoForgeRegistries
{
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HearthstoneConstants.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, HearthstoneConstants.MOD_ID);
    private static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
            HearthstoneConstants.MOD_ID);

    public static final Supplier<DataComponentType<HearthstoneItemData>> HEARTHSTONE_ITEM_DATA = DATA_COMPONENT_TYPES.registerComponentType(
            HearthstoneContentIds.HEARTHSTONE_ITEM_DATA.getPath(),
            builder -> builder.persistent(HearthstoneItemData.CODEC).networkSynchronized(HearthstoneItemData.STREAM_CODEC).cacheEncoding());
    public static final Supplier<Item> HEARTHSTONE = ITEMS.register(HearthstoneContentIds.HEARTHSTONE.getPath(), NeoForgeItemHearthstone::new);
    public static final Supplier<SoundEvent> HEARTHSTONE_CAST = SOUND_EVENTS.register(HearthstoneContentIds.HEARTHSTONE_CAST_SOUND.getPath(),
            () -> com.sythiex.hearthstonemod.content.HearthstoneContentFactory.createVariableRangeSound(HearthstoneContentIds.HEARTHSTONE_CAST_SOUND));
    public static final Supplier<SoundEvent> HEARTHSTONE_IMPACT = SOUND_EVENTS.register(HearthstoneContentIds.HEARTHSTONE_IMPACT_SOUND.getPath(),
            () -> com.sythiex.hearthstonemod.content.HearthstoneContentFactory.createVariableRangeSound(HearthstoneContentIds.HEARTHSTONE_IMPACT_SOUND));
    public static final Supplier<SoundEvent> HEARTHSTONE_CHANNEL = SOUND_EVENTS.register(HearthstoneContentIds.HEARTHSTONE_CHANNEL_SOUND.getPath(),
            () -> com.sythiex.hearthstonemod.content.HearthstoneContentFactory.createVariableRangeSound(HearthstoneContentIds.HEARTHSTONE_CHANNEL_SOUND));

    private HearthstoneNeoForgeRegistries()
    {}

    /**
     * Registers the deferred registers on the mod event bus.
     */
    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        DATA_COMPONENT_TYPES.register(modEventBus);
    }

    /**
     * Adds the Hearthstone to the tools tab once the creative contents are being
     * built.
     */
    public static void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event)
    {
        if (CreativeModeTabs.TOOLS_AND_UTILITIES.equals(event.getTabKey()))
        {
            event.accept(HEARTHSTONE.get());
        }
    }
}
