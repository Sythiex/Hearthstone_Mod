package com.sythiex.hearthstonemod.client;

import java.util.HashSet;
import java.util.Set;

import com.sythiex.hearthstonemod.HearthstoneConfig.HearthstoneSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
/**
 * Client-side mirror of active cast state.
 *
 * This is used for purely visual feedback such as the cast bar, item foil, arm
 * poses, and looping channel audio.
 */
public final class HearthstoneClientCasting
{
    private static final Set<Integer> CASTING_ENTITY_IDS = new HashSet<>();
    private static long localCastStartTime = -1L;

    private HearthstoneClientCasting()
    {}

    /**
     * Applies a cast-state update received from the server.
     *
     * @param entityId  runtime entity id whose cast state changed
     * @param isCasting {@code true} when the entity started casting, {@code false}
     *                  when it stopped
     */
    public static void handleSync(int entityId, boolean isCasting)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (isCasting)
        {
            boolean startedCasting = CASTING_ENTITY_IDS.add(entityId);
            if (minecraft.player != null && minecraft.player.getId() == entityId && minecraft.level != null)
            {
                localCastStartTime = minecraft.level.getGameTime();
            }

            if (startedCasting && minecraft.level != null)
            {
                Entity entity = minecraft.level.getEntity(entityId);
                if (entity != null)
                {
                    minecraft.getSoundManager().play(new HearthstoneChannelSound(entity));
                }
            }
        } else
        {
            CASTING_ENTITY_IDS.remove(entityId);
            if (minecraft.player != null && minecraft.player.getId() == entityId)
            {
                localCastStartTime = -1L;
            }
        }
    }

    /**
     * Checks whether a given entity is currently marked as casting on the client.
     *
     * @param entityId runtime entity id to query
     * @return {@code true} if the entity is currently marked as casting
     */
    public static boolean isEntityCasting(int entityId)
    {
        return CASTING_ENTITY_IDS.contains(entityId);
    }

    /**
     * Clears all cached client cast state, typically when leaving a world.
     */
    public static void clear()
    {
        CASTING_ENTITY_IDS.clear();
        localCastStartTime = -1L;
    }

    /**
     * Returns the active client level.
     *
     * @return current client level, or {@code null} if no world is loaded
     */
    public static Level getClientLevel()
    {
        return Minecraft.getInstance().level;
    }

    /**
     * Checks whether the local player is actively casting with the given stack in
     * the main hand.
     *
     * @param stack stack to test
     * @return {@code true} if the stack is the local player's actively casting
     *         Hearthstone
     */
    public static boolean isLocalStackCasting(ItemStack stack)
    {
        Minecraft minecraft = Minecraft.getInstance();
        return localCastStartTime >= 0L && minecraft.player != null && isEntityCasting(minecraft.player.getId()) && stack == minecraft.player.getMainHandItem();
    }

    /**
     * Calculates the local cast progress in ticks for HUD rendering.
     *
     * @param stack stack to query
     * @return elapsed cast time clamped to the configured channel duration, or
     *         {@code 0} when the stack is not actively casting
     */
    public static int getLocalCastTime(ItemStack stack)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isLocalStackCasting(stack) || minecraft.level == null)
        {
            return 0;
        }

        long elapsedTime = minecraft.level.getGameTime() - localCastStartTime;
        return (int) Math.min(HearthstoneSettings.channelTime.get(), Math.max(0L, elapsedTime));
    }
}
