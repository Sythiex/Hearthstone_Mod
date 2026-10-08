package com.sythiex.hearthstonemod.neoforge.compat.sable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sythiex.hearthstonemod.compat.HearthstoneDestination;
import com.sythiex.hearthstonemod.compat.HearthstoneDestinationResolution;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData.TakenLoginPoint;
import dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;

/**
 * One server-thread preparation attempt and its temporary world/sublevel loading leases.
 */
final class SableDestinationResolver implements HearthstoneDestinationResolution
{
    private static final SubLevelLoadingTicketType<UUID> SUBLEVEL_TICKET = SubLevelLoadingTicketType.create(
            ResourceLocation.fromNamespaceAndPath("hearthstonemod", "arrival"), UUIDUtil.CODEC);
    private static final TicketType<UUID> WORLD_TICKET = TicketType.create("hearthstonemod_sable_arrival", UUID::compareTo, 140);
    private final ServerLevel level;
    private final UUID trackingPointId;
    private final UUID leaseId = UUID.randomUUID();
    private final Set<ChunkPos> worldTickets = new HashSet<>();
    private ServerSubLevel ticketedSubLevel;
    private boolean loadAttempted;
    private boolean closed;

    SableDestinationResolver(ServerLevel level, UUID trackingPointId)
    {
        this.level = level;
        this.trackingPointId = trackingPointId;
    }

    /**
     * Registers the ticket codec before Sable deserializes saved loading tickets.
     */
    static void registerTicketType()
    {}

    /**
     * Removes only this adapter's tickets from a previous, interrupted session.
     */
    static void clearStaleTickets(ServerLevel level)
    {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;
        Map.copyOf(container.collectForceLoadTickets()).forEach((subLevel, tickets) ->
                Set.copyOf(tickets).forEach(ticket ->
                {
                    if (ticket.type() == SUBLEVEL_TICKET && ticket.key() instanceof UUID key)
                    {
                        container.removeForceLoadTicket(subLevel, SUBLEVEL_TICKET, key);
                    }
                }));
    }

    @Override
    public Result poll()
    {
        if (closed) return Result.unavailable(UNAVAILABLE_MESSAGE);
        TrackingPoint point = SableTrackingPoints.get(level, trackingPointId);
        if (point == null) return Result.unavailable(UNAVAILABLE_MESSAGE);
        BlockPos homePos = BlockPos.containing(point.point().x, point.point().y, point.point().z);
        if (!point.inSubLevel())
        {
            // Sable also converts points to ordinary coordinates on deletion. Those
            // coordinates do not prove the bed survived; require a new explicit link.
            return Result.unavailable(UNAVAILABLE_MESSAGE);
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || point.subLevelID() == null) return Result.unavailable(UNAVAILABLE_MESSAGE);
        if (!container.inBounds(homePos)) return Result.unavailable(UNAVAILABLE_MESSAGE);
        int plotX = (homePos.getX() >> (4 + container.getLogPlotSize())) - container.getOrigin().x;
        int plotZ = (homePos.getZ() >> (4 + container.getLogPlotSize())) - container.getOrigin().y;
        if (!container.getOccupancy().get(container.getIndex(plotX, plotZ))) return Result.unavailable(UNAVAILABLE_MESSAGE);
        SubLevel containing = Sable.HELPER.getContaining(level, point.point());
        if (containing != null && !point.subLevelID().equals(containing.getUniqueId()))
        {
            return Result.unavailable(UNAVAILABLE_MESSAGE);
        }
        SubLevel existing = container.getSubLevel(point.subLevelID());
        if (existing == null && !loadAttempted)
        {
            loadAttempted = true;
            SubLevelHoldingChunkMap holdingMap = container.getHoldingChunkMap();
            HoldingSubLevel holding = holdingMap.getHoldingSubLevel(point.subLevelID());
            if (holding != null)
            {
                // Normal holding-chunk activation loads the entire dependency chain, including unsaved structures.
                Vector3d origin = holding.data().pose().position();
                holdWorldChunk(new Vec3(origin.x, origin.y, origin.z));
                holdBounds(holding.data().bounds());
                for (UUID dependency : holding.data().dependencies())
                {
                    HoldingSubLevel dependent = holdingMap.getHoldingSubLevel(dependency);
                    if (dependent != null) holdBounds(dependent.data().bounds());
                }
            } else if (point.lastSavedSubLevelPointer() != null)
            {
                // Sable performs UUID-based snatching and loads the saved dependency chain together.
                holdingMap.snatchAndLoad(point.lastSavedSubLevelPointer(), point.subLevelID());
            } else
            {
                // A placeholder without live or saved structure identity is not a usable bed destination.
                return Result.unavailable(UNAVAILABLE_MESSAGE);
            }
            existing = container.getSubLevel(point.subLevelID());
        }
        if (!(existing instanceof ServerSubLevel subLevel)) return Result.pending();
        if (Sable.HELPER.getContaining(level, point.point()) != subLevel) return Result.unavailable(UNAVAILABLE_MESSAGE);
        if (ticketedSubLevel == null)
        {
            container.addForceLoadTicket(subLevel, SUBLEVEL_TICKET, leaseId);
            ticketedSubLevel = subLevel;
        }
        if (container.getChunk(new ChunkPos(homePos)) == null) return Result.pending();

        // Only use Sable's resolver after checking identity; its ordinary lookup trusts plot occupancy.
        TakenLoginPoint resolved = SableTrackingPoints.resolve(level, trackingPointId);
        if (resolved == null || !subLevel.getUniqueId().equals(resolved.subLevelId())) return Result.unavailable(UNAVAILABLE_MESSAGE);
        holdWorldChunk(new Vec3(resolved.position().x(), resolved.position().y(), resolved.position().z()));
        return Result.ready(new HearthstoneDestination(level, homePos, position ->
        {
            if (closed || container.getSubLevel(subLevel.getUniqueId()) != subLevel) return null;
            Vector3d transformed = subLevel.logicalPose().transformPosition(new Vector3d(position.x, position.y, position.z));
            Vec3 world = new Vec3(transformed.x, transformed.y, transformed.z);
            if (SableDestinationBridge.inPlotGrid(level, BlockPos.containing(world))) return null;
            return world;
        }));
    }

    /**
     * Sable activates held structures only when every chunk under the dependency chain is available.
     */
    private void holdBounds(BoundingBox3dc bounds)
    {
        int minX = ((int) Math.floor(bounds.minX() - 1)) >> 4;
        int maxX = ((int) Math.floor(bounds.maxX() + 1)) >> 4;
        int minZ = ((int) Math.floor(bounds.minZ() - 1)) >> 4;
        int maxZ = ((int) Math.floor(bounds.maxZ() + 1)) >> 4;
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++) holdWorldChunk(new Vec3(x * 16.0, 0, z * 16.0));
        }
    }

    /**
     * Tickets world coordinates only; vanilla chunk tickets cannot load Sable plot coordinates.
     */
    private void holdWorldChunk(Vec3 position)
    {
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || SableDestinationBridge.inPlotGrid(level, BlockPos.containing(position)))
        {
            throw new IllegalStateException("Sable destination did not resolve to world coordinates");
        }
        ChunkPos chunk = new ChunkPos(BlockPos.containing(position));
        if (worldTickets.add(chunk))
        {
            level.getChunkSource().addRegionTicket(WORLD_TICKET, chunk, 2, leaseId);
            level.getChunk(chunk.x, chunk.z);
        }
    }

    @Override
    public void close()
    {
        if (closed) return;
        closed = true;
        if (ticketedSubLevel != null)
        {
            SubLevelContainer.getContainer(level).removeForceLoadTicket(ticketedSubLevel, SUBLEVEL_TICKET, leaseId);
        }
        for (ChunkPos chunk : worldTickets) level.getChunkSource().removeRegionTicket(WORLD_TICKET, chunk, 2, leaseId);
        worldTickets.clear();
    }
}
