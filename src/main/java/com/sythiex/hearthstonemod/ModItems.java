package com.sythiex.hearthstonemod;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred item registry for the mod.
 */
public final class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HearthstoneMod.MODID);

    public static final RegistryObject<Item> HEARTHSTONE = ITEMS.register(ItemHearthstone.NAME, ItemHearthstone::new);

    private ModItems()
    {}

    /**
     * Registers the mod's item deferred register on the mod event bus.
     *
     * @param eventBus mod event bus
     */
    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
