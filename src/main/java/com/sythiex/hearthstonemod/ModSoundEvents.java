package com.sythiex.hearthstonemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred sound-event registry for the Hearthstone's cast lifecycle.
 */
public final class ModSoundEvents
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HearthstoneMod.MODID);

    public static final RegistryObject<SoundEvent> HEARTHSTONE_CAST = SOUND_EVENTS.register("hearthstonecast", () -> SoundEvent.createVariableRangeEvent(id("hearthstonecast")));
    public static final RegistryObject<SoundEvent> HEARTHSTONE_IMPACT = SOUND_EVENTS.register("hearthstoneimpact", () -> SoundEvent.createVariableRangeEvent(id("hearthstoneimpact")));
    public static final RegistryObject<SoundEvent> HEARTHSTONE_CHANNEL = SOUND_EVENTS.register("hearthstonechannel", () -> SoundEvent.createVariableRangeEvent(id("hearthstonechannel")));

    private ModSoundEvents()
    {}

    /**
     * Registers the mod's sound-event deferred register on the mod event bus.
     *
     * @param eventBus mod event bus
     */
    public static void register(IEventBus eventBus)
    {
        SOUND_EVENTS.register(eventBus);
    }

    /**
     * Creates a namespaced resource location inside this mod's domain.
     *
     * @param path resource path within the mod namespace
     * @return fully qualified resource location
     */
    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(HearthstoneMod.MODID, path);
    }
}
