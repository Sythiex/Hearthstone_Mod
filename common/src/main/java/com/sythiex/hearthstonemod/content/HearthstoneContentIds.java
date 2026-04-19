package com.sythiex.hearthstonemod.content;

import com.sythiex.hearthstonemod.HearthstoneConstants;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared registry ids for content that exists on both loaders.
 */
public final class HearthstoneContentIds
{
    public static final ResourceLocation HEARTHSTONE = HearthstoneConstants.id("hearthstone");
    public static final ResourceKey<Item> HEARTHSTONE_ITEM_KEY = ResourceKey.create(Registries.ITEM, HEARTHSTONE);
    public static final ResourceLocation HEARTHSTONE_ITEM_DATA = HearthstoneConstants.id("hearthstone_data");

    public static final ResourceLocation HEARTHSTONE_CAST_SOUND = HearthstoneConstants.id("hearthstonecast");
    public static final ResourceLocation HEARTHSTONE_IMPACT_SOUND = HearthstoneConstants.id("hearthstoneimpact");
    public static final ResourceLocation HEARTHSTONE_CHANNEL_SOUND = HearthstoneConstants.id("hearthstonechannel");

    private HearthstoneContentIds()
    {}

    /**
     * Checks the runtime registry id so the helper works on both loaders.
     */
    public static boolean isHearthstone(ItemStack stack)
    {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(HEARTHSTONE);
    }
}
