package com.sythiex.hearthstonemod;

import java.util.function.Function;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;

public class HearthstoneTeleporter implements ITeleporter
{
	protected final ServerWorld world;
	
	public HearthstoneTeleporter(ServerWorld serverWorld)
	{
		this.world = serverWorld;
	}
	
	@Override
	public Entity placeEntity(Entity entity, ServerWorld currentWorld, ServerWorld destWorld, float yaw, Function<Boolean, Entity> repositionEntity)
	{
		currentWorld.getProfiler().startSection("placing");
		entity.setWorld(destWorld);
		destWorld.addDuringPortalTeleport((ServerPlayerEntity) entity);
		currentWorld.getProfiler().endSection();
		return entity;
	}
}