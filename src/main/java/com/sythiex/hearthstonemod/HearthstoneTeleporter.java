package com.sythiex.hearthstonemod;

import java.util.function.Function;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.server.ServerWorld;

public class HearthstoneTeleporter extends Teleporter
{
	public HearthstoneTeleporter(ServerWorld serverWorld)
	{
		super(serverWorld);
	}
	
	@Override
	public Entity placeEntity(Entity entityIn, ServerWorld currentWorld, ServerWorld destWorld, float yaw, Function<Boolean, Entity> repositionEntity)
	{
		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) entityIn;
		double d0 = serverPlayer.getPosX();
		double d1 = serverPlayer.getPosY();
		double d2 = serverPlayer.getPosZ();
		float f = serverPlayer.rotationPitch;
		float f1 = serverPlayer.rotationYaw;
		double d3 = 8.0D;
		float f2 = f1;
		currentWorld.getProfiler().startSection("moving");
		double moveFactor = currentWorld.getDimension().getMovementFactor() / destWorld.getDimension().getMovementFactor();
		d0 *= moveFactor;
		d2 *= moveFactor;
		
		serverPlayer.setLocationAndAngles(d0, d1, d2, f1, f);
		currentWorld.getProfiler().endSection();
		currentWorld.getProfiler().startSection("placing");
		double d7 = Math.min(-2.9999872E7D, destWorld.getWorldBorder().minX() + 16.0D);
		double d4 = Math.min(-2.9999872E7D, destWorld.getWorldBorder().minZ() + 16.0D);
		double d5 = Math.min(2.9999872E7D, destWorld.getWorldBorder().maxX() - 16.0D);
		double d6 = Math.min(2.9999872E7D, destWorld.getWorldBorder().maxZ() - 16.0D);
		d0 = MathHelper.clamp(d0, d7, d5);
		d2 = MathHelper.clamp(d2, d4, d6);
		serverPlayer.setLocationAndAngles(d0, d1, d2, f1, f);
		
		currentWorld.getProfiler().endSection();
		serverPlayer.setWorld(destWorld);
		destWorld.addDuringPortalTeleport(serverPlayer);
		//CriteriaTriggers.CHANGED_DIMENSION.trigger(serverPlayer, currentWorld.dimension.getType(), destWorld.dimension.getType());
		serverPlayer.connection.setPlayerLocation(serverPlayer.getPosX(), serverPlayer.getPosY(), serverPlayer.getPosZ(), f1, f);
		return serverPlayer;
	}
	
	//@formatter:off
	/*
	@Override
	public boolean placeInPortal(Entity pEntity, float rotationYaw)
	{
		this.world.getBlockState(new BlockPos((int) this.x, (int) this.y, (int) this.z)); // dummy load to maybe gen chunk
		pEntity.setPosition(this.x, this.y, this.z);
		pEntity.setMotion(0, 0, 0);
		return true;
	}
	*/
	//@formatter:on
}