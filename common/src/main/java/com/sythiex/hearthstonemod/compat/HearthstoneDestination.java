package com.sythiex.hearthstonemod.compat;

import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Resolved destination containing the block position to validate and a function
 * that converts the chosen arrival position into world coordinates. Call the
 * conversion on the server thread immediately before teleporting so it reflects
 * any movement or rotation of the destination. Bed/anchor validation is still required.
 */
public record HearthstoneDestination(ServerLevel level, BlockPos homePos, UnaryOperator<Vec3> toWorld)
{
    /**
     * Creates an ordinary world-space destination.
     */
    public static HearthstoneDestination ordinary(ServerLevel level, BlockPos pos)
    {
        return new HearthstoneDestination(level, pos.immutable(), UnaryOperator.identity());
    }
}
