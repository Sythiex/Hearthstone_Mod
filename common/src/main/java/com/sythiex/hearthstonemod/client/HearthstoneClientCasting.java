package com.sythiex.hearthstonemod.client;

import java.util.HashSet;
import java.util.Set;

import com.sythiex.hearthstonemod.HearthstoneCommon;

import com.sythiex.hearthstonemod.network.SyncHearthstoneCastingPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Client-side view of which entities are currently channeling a Hearthstone.
 */
public final class HearthstoneClientCasting
{
    private static final Set<Integer> CASTING_ENTITY_IDS = new HashSet<>();
    private static long localCastStartTime = -1L;

    private HearthstoneClientCasting()
    {}

    /**
     * Clears all cached cast state, typically on disconnect.
     */
    public static void clear()
    {
        CASTING_ENTITY_IDS.clear();
        localCastStartTime = -1L;
    }

    /**
     * Applies the latest cast-state packet from the server.
     */
    public static void handleSync(SyncHearthstoneCastingPayload payload)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.casting())
        {
            boolean startedCasting = CASTING_ENTITY_IDS.add(payload.entityId());
            if (minecraft.player != null && minecraft.player.getId() == payload.entityId() && minecraft.level != null)
            {
                localCastStartTime = minecraft.level.getGameTime();
            }

            if (startedCasting && minecraft.level != null)
            {
                Entity entity = minecraft.level.getEntity(payload.entityId());
                if (entity != null)
                {
                    minecraft.getSoundManager().play(new HearthstoneChannelSound(entity));
                }
            }
        } else
        {
            CASTING_ENTITY_IDS.remove(payload.entityId());
            if (minecraft.player != null && minecraft.player.getId() == payload.entityId())
            {
                localCastStartTime = -1L;
            }
        }
    }

    public static boolean isEntityCasting(int entityId)
    {
        return CASTING_ENTITY_IDS.contains(entityId);
    }

    public static Level getClientLevel()
    {
        return Minecraft.getInstance().level;
    }

    /**
     * Returns whether the given stack is the local player's actively channeling
     * stack.
     */
    public static boolean isLocalStackCasting(ItemStack stack)
    {
        Minecraft minecraft = Minecraft.getInstance();
        return localCastStartTime >= 0L && minecraft.player != null && isEntityCasting(minecraft.player.getId()) && stack == minecraft.player.getMainHandItem();
    }

    /**
     * Returns local channel progress in ticks for HUD and hand animation code.
     */
    public static int getLocalCastTime(ItemStack stack)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isLocalStackCasting(stack) || minecraft.level == null)
        {
            return 0;
        }

        long elapsedTime = minecraft.level.getGameTime() - localCastStartTime;
        return (int) Math.min(HearthstoneCommon.config().channelTime(), Math.max(0L, elapsedTime));
    }
}
