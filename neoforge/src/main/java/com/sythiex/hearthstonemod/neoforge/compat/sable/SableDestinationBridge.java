package com.sythiex.hearthstonemod.neoforge.compat.sable;

import java.util.Optional;
import java.util.UUID;

import com.sythiex.hearthstonemod.compat.HearthstoneDestinationBridge;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution;
import com.sythiex.hearthstonemod.compat.HearthstoneExternalTarget;
import com.sythiex.hearthstonemod.content.HearthstoneHomeLink;
import com.sythiex.hearthstonemod.content.HearthstoneLinkType;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Routes reserved plots through persistent Sable references, never ordinary coordinate fallback.
 */
public final class SableDestinationBridge implements HearthstoneDestinationBridge
{
    public static final ResourceLocation PROVIDER_ID = ResourceLocation.fromNamespaceAndPath("hearthstonemod", "sable");

    @Override
    public Capture capture(ServerLevel level, BlockPos pos, HearthstoneLinkType type)
    {
        if (!inPlotGrid(level, pos)) return DEFAULT.capture(level, pos, type);
        if (!(Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel subLevel))
        {
            return Capture.unavailable(HearthstoneDestinationResolution.UNAVAILABLE_MESSAGE);
        }
        UUID id = SableTrackingPoints.capture(level, pos, subLevel);
        if (id == null) return Capture.unavailable(HearthstoneDestinationResolution.UNAVAILABLE_MESSAGE);
        return Capture.linked(new HearthstoneHomeLink(pos.immutable(), level.dimension().location(), type,
                Optional.of(new HearthstoneExternalTarget(PROVIDER_ID, id))));
    }

    @Override
    public HearthstoneDestinationResolution prepare(ServerPlayer player, HearthstoneHomeLink link)
    {
        ServerLevel level = player.server.getLevel(link.dimensionKey());
        if (level == null) return HearthstoneDestinationResolution.unavailable(HearthstoneDestinationResolution.UNAVAILABLE_MESSAGE);
        if (link.externalTarget().isPresent())
        {
            HearthstoneExternalTarget reference = link.externalTarget().get();
            return PROVIDER_ID.equals(reference.providerId()) ? new SableDestinationResolver(level, reference.trackingPointId()) : DEFAULT.prepare(player, link);
        }
        if (inPlotGrid(level, link.homePos()))
        {
            return HearthstoneDestinationResolution.unavailable("item.hearthstone.destination_relink");
        }
        return DEFAULT.prepare(player, link);
    }

    static boolean inPlotGrid(ServerLevel level, BlockPos pos)
    {
        return Sable.HELPER.isInPlotGrid(level, pos.getX() >> 4, pos.getZ() >> 4);
    }
}
