package com.sythiex.hearthstonemod.neoforge.compat.sable;

import java.util.UUID;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Isolates Sable's persistent tracking-point implementation from shared item code.
 */
final class SableTrackingPoints
{
    private SableTrackingPoints()
    {}

    static UUID capture(ServerLevel level, BlockPos pos, ServerSubLevel subLevel)
    {
        // Sable 2.0.5 cannot attach a first serialization pointer to a tracking point
        // after its never-saved sublevel has already unloaded. Establish that pointer
        // while the structure is still loaded; later Sable saves maintain it.
        if (subLevel.getLastSerializationPointer() == null)
        {
            SubLevelContainer.getContainer(level).getHoldingChunkMap().saveAll();
            if (subLevel.getLastSerializationPointer() == null) return null;
        }
        return SubLevelTrackingPointSavedData.getOrLoad(level).generateTrackingPoint(Vec3.atCenterOf(pos), subLevel);
    }

    static TrackingPoint get(ServerLevel level, UUID id)
    {
        return SubLevelTrackingPointSavedData.getOrLoad(level).getTrackingPoint(id);
    }

    /**
     * Resolves without consuming the reference, which may be shared by copied items.
     */
    static SubLevelTrackingPointSavedData.TakenLoginPoint resolve(ServerLevel level, UUID id)
    {
        return SubLevelTrackingPointSavedData.getOrLoad(level).take(id, false);
    }
}
