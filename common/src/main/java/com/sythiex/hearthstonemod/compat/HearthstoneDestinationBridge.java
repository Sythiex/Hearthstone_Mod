package com.sythiex.hearthstonemod.compat;

import java.util.Objects;

import com.sythiex.hearthstonemod.content.HearthstoneHomeLink;
import com.sythiex.hearthstonemod.content.HearthstoneLinkType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-independent capture and preparation contract. All calls run on the server thread.
 */
public interface HearthstoneDestinationBridge
{
    HearthstoneDestinationBridge DEFAULT = new HearthstoneDestinationBridge()
    {
        @Override
        public Capture capture(ServerLevel level, BlockPos pos, HearthstoneLinkType type)
        {
            return Capture.linked(new HearthstoneHomeLink(pos.immutable(), level.dimension().location(), type));
        }

        @Override
        public HearthstoneDestinationResolution prepare(ServerPlayer player, HearthstoneHomeLink link)
        {
            if (link.externalTarget().isPresent())
            {
                return HearthstoneDestinationResolution.unavailable("item.hearthstone.destination_provider_missing");
            }
            ServerLevel level = player.server.getLevel(link.dimensionKey());
            return level == null ? HearthstoneDestinationResolution.unavailable(HearthstoneDestinationResolution.UNAVAILABLE_MESSAGE)
                    : HearthstoneDestinationResolution.ready(HearthstoneDestination.ordinary(level, link.homePos()));
        }
    };

    /**
     * Captures a validated bed/anchor, without changing the player's respawn point.
     */
    Capture capture(ServerLevel level, BlockPos pos, HearthstoneLinkType type);

    /**
     * Prepares a destination without teleporting or applying item side effects.
     */
    HearthstoneDestinationResolution prepare(ServerPlayer player, HearthstoneHomeLink link);

    /**
     * A successful link or a failure that preserves the previous link.
     */
    record Capture(HearthstoneHomeLink link, Component message)
    {
        public Capture
        {
            if ((link == null) == (message == null))
            {
                throw new IllegalArgumentException("Capture needs either a link or a failure message");
            }
        }

        public static Capture linked(HearthstoneHomeLink link)
        {
            return new Capture(Objects.requireNonNull(link), null);
        }

        public static Capture unavailable(String messageKey)
        {
            return new Capture(null, Component.translatable(messageKey));
        }
    }
}
