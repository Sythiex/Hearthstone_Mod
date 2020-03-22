package com.sythiex.hearthstonemod;

import java.text.DecimalFormat;
import java.util.List;

import javax.annotation.Nullable;

import com.sythiex.hearthstonemod.HearthstoneConfig.HearthstoneSettings;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SPlaySoundEventPacket;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemHearthstone extends Item
{
	public static final String NAME = "hearthstone";
	
	public final TranslationTextComponent TEXT_ON_COOLDOWN = new TranslationTextComponent("item.hearthstone.on_cooldown");
	public final TranslationTextComponent TEXT_NO_BED = new TranslationTextComponent("item.hearthstone.no_bed");
	public final TranslationTextComponent TEXT_MISSING_BED = new TranslationTextComponent("item.hearthstone.missing_bed");
	public final TranslationTextComponent TEXT_LINKED = new TranslationTextComponent("item.hearthstone.linked");
	public final TranslationTextComponent TEXT_CANCELED = new TranslationTextComponent("item.hearthstone.canceled");
	
	public ItemHearthstone()
	{
		super(new Item.Properties().maxStackSize(1).group(ItemGroup.TOOLS));
		setRegistryName(NAME);
	}
	
	@Override
	public void inventoryTick(ItemStack itemStack, World world, Entity entity, int itemSlot, boolean isSelected)
	{
		// server side
		if(!world.isRemote)
		{
			CompoundNBT tag = itemStack.getTag();
			
			// if item has tag, decrement cooldown
			if(tag != null)
			{
				int cooldown = tag.getInt("cooldown");
				if(cooldown > 0)
				{
					cooldown--;
					tag.putInt("cooldown", cooldown);
				}
			}
			// if no tag, add a tag
			else
			{
				tag = new CompoundNBT();
				tag.putInt("cooldown", 0);
				tag.putInt("castTime", 0);
				tag.putInt("bedX", 0);
				tag.putInt("bedY", 0);
				tag.putInt("bedZ", 0);
				tag.putInt("bedDimension", 0);
				tag.putDouble("prevX", -1);
				tag.putDouble("prevY", -1);
				tag.putDouble("prevZ", -1);
				tag.putBoolean("locationSet", false);
				tag.putBoolean("isCasting", false);
				tag.putBoolean("stopCasting", false);
			}
			
			// if player is casting
			if(tag.getBoolean("isCasting") && entity instanceof PlayerEntity)
			{
				PlayerEntity player = (PlayerEntity) entity;
				
				// check if player is holding hearthstone
				ItemStack heldItem = player.getHeldItemMainhand();
				if(heldItem != null)
				{
					if(heldItem != itemStack)
					{
						tag.putBoolean("stopCasting", true);
					}
				}
				else
					tag.putBoolean("stopCasting", true);
				
				// detect player movement
				double diffX = Math.abs(tag.getDouble("prevX") - player.posX);
				double diffY = Math.abs(tag.getDouble("prevY") - player.posY);
				double diffZ = Math.abs(tag.getDouble("prevZ") - player.posZ);
				// if player moves or swaps items cancel cast
				if(((diffX > 0.05 || diffY > 0.05 || diffZ > 0.05) && tag.getDouble("prevY") != -1) || tag.getBoolean("stopCasting"))
				{
					tag.putInt("castTime", 0);
					tag.putBoolean("isCasting", false);
					tag.putBoolean("stopCasting", false);
					player.sendStatusMessage(TEXT_CANCELED, true);
				}
				else
				{
					// increment cast time
					tag.putInt("castTime", tag.getInt("castTime") + 1);
				}
				
				// initiate tp after casting
				if(tag.getInt("castTime") >= HearthstoneSettings.channelTime.get())
				{
					// stop and reset cast time
					tag.putInt("castTime", 0);
					tag.putBoolean("isCasting", false);
					
					world.playSound(null, player.posX, player.posY, player.posZ, HearthstoneMod.castSoundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
					
					// get bed location
					int bedX = tag.getInt("bedX");
					int bedY = tag.getInt("bedY");
					int bedZ = tag.getInt("bedZ");
					
					// if player is not in same dimension as bed, travel to that dimension
					int dimension = tag.getInt("bedDimension");
					int oldDimension = player.getEntityWorld().getDimension().getType().getId();
					if(dimension != oldDimension)
					{
						player = this.changeDimension(DimensionType.getById(dimension), (ServerPlayerEntity) player);
						player.setLocationAndAngles(bedX, bedY, bedZ, player.rotationYaw, player.rotationPitch);
						world = player.getEntityWorld();
					}
					
					// get block at bed location
					BlockPos bedPos = new BlockPos(bedX, bedY, bedZ);
					BlockState state = world.getBlockState(bedPos);
					Block block = state.getBlock();
					
					// checks if bed is still there
					if(block.isBed(state, world, bedPos, player))
					{
						// find open spaces around bed
						boolean north = player.world.getBlockState(bedPos.north()).getBlock().canSpawnInBlock();
						boolean northUp = player.world.getBlockState(bedPos.north().up()).getBlock().canSpawnInBlock();
						boolean northDown = player.world.getBlockState(bedPos.north().down()).isAir();
						
						boolean east = player.world.getBlockState(bedPos.east()).getBlock().canSpawnInBlock();
						boolean eastUp = player.world.getBlockState(bedPos.east().up()).getBlock().canSpawnInBlock();
						boolean eastDown = player.world.getBlockState(bedPos.east().down()).isAir();
						
						boolean south = player.world.getBlockState(bedPos.south()).getBlock().canSpawnInBlock();
						boolean southUp = player.world.getBlockState(bedPos.south().up()).getBlock().canSpawnInBlock();
						boolean southDown = player.world.getBlockState(bedPos.south().down()).isAir();
						
						boolean west = player.world.getBlockState(bedPos.west()).getBlock().canSpawnInBlock();
						boolean westUp = player.world.getBlockState(bedPos.west().up()).getBlock().canSpawnInBlock();
						boolean westDown = player.world.getBlockState(bedPos.west().down()).isAir();
						
						// tp player next to bed
						if(north && northUp && !northDown)
						{
							player.setPositionAndUpdate(bedPos.north().getX() + 0.5, bedPos.north().getY(), bedPos.north().getZ() + 0.5);
						}
						else if(east && eastUp && !eastDown)
						{
							player.setPositionAndUpdate(bedPos.east().getX() + 0.5, bedPos.east().getY(), bedPos.east().getZ() + 0.5);
						}
						else if(south && southUp && !southDown)
						{
							player.setPositionAndUpdate(bedPos.south().getX() + 0.5, bedPos.south().getY(), bedPos.south().getZ() + 0.5);
						}
						else if(west && westUp && !westDown)
						{
							player.setPositionAndUpdate(bedPos.west().getX() + 0.5, bedPos.west().getY(), bedPos.west().getZ() + 0.5);
						}
						// if no open space, tp player on top of bed
						else
						{
							player.setPositionAndUpdate(bedX + 0.5, bedY + 1, bedZ + 0.5);
						}
						
						world.playSound(null, player.posX, player.posY, player.posZ, HearthstoneMod.impactSoundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
						
						// sets hearthstone on cooldown
						tag.putInt("cooldown", HearthstoneSettings.cooldown.get());
					}
					// tps player to where bed was, then breaks link
					else
					{
						player.setPositionAndUpdate(bedX + 0.5, bedY + 1, bedZ + 0.5);
						world.playSound(null, player.posX, player.posY, player.posZ, HearthstoneMod.impactSoundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
						// sets hearthstone on cooldown
						tag.putInt("cooldown", HearthstoneSettings.cooldown.get());
						tag.putBoolean("locationSet", false);
						// informs player of broken link
						player.sendStatusMessage(TEXT_MISSING_BED, true);
					}
				}
			}
			// record position of player for detecting movement
			tag.putDouble("prevX", entity.posX);
			tag.putDouble("prevY", entity.posY);
			tag.putDouble("prevZ", entity.posZ);
			
			// save tag
			itemStack.setTag(tag);
		}
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
	{
		if(!world.isRemote)
		{
			ItemStack itemStack = player.getHeldItem(hand);
			CompoundNBT tagCompound = itemStack.getTag();
			
			// if not sneaking
			if(!player.isSneaking())
			{
				// if location is set
				if(tagCompound.getBoolean("locationSet"))
				{
					int cooldown = tagCompound.getInt("cooldown");
					
					// if off cooldown
					if(cooldown <= 0)
					{
						// if player is not casting, start casting
						if(!tagCompound.getBoolean("isCasting"))
						{
							tagCompound.putBoolean("isCasting", true);
							world.playSound(player, player.posX, player.posY, player.posZ, HearthstoneMod.channelSoundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
						}
					}
					// if on cooldown
					else
					{
						player.sendStatusMessage(TEXT_ON_COOLDOWN, true);
					}
				}
				// if location is not set
				else
				{
					player.sendStatusMessage(TEXT_NO_BED, true);
				}
			}
			// save tag
			itemStack.setTag(tagCompound);
		}
		return new ActionResult(ActionResultType.PASS, player.getHeldItem(hand));
	}
	
	@Override
	public ActionResultType onItemUse(ItemUseContext context)
	{
		if(!context.getWorld().isRemote())
		{
			ItemStack itemStack = context.getPlayer().getHeldItem(context.getPlayer().getActiveHand());
			CompoundNBT tagCompound = itemStack.getTag();
			
			// if sneaking
			if(context.getPlayer().isSneaking())
			{
				// checks if block right clicked is bed
				BlockState state = context.getWorld().getBlockState(context.getPos());
				if(context.getWorld().getBlockState(context.getPos()).getBlock().isBed(state, context.getWorld(), context.getPos(), context.getPlayer()))
				{
					// links bed to hearthstone
					tagCompound.putInt("bedX", context.getPos().getX());
					tagCompound.putInt("bedY", context.getPos().getY());
					tagCompound.putInt("bedZ", context.getPos().getZ());
					tagCompound.putInt("bedDimension", context.getPlayer().getEntityWorld().getDimension().getType().getId());
					tagCompound.putBoolean("locationSet", true);
					context.getPlayer().sendStatusMessage(TEXT_LINKED, true);
				}
				// save tag
				itemStack.setTag(tagCompound);
				return ActionResultType.SUCCESS;
			}
			else
			{
				onItemRightClick(context.getWorld(), context.getPlayer(), context.getPlayer().getActiveHand());
			}
		}
		return ActionResultType.FAIL;
	}
	
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		if(oldStack.getItem() == newStack.getItem())
		{
			CompoundNBT oldTag = oldStack.getTag();
			CompoundNBT newTag = newStack.getTag();
			if(oldTag != null && newTag != null)
			{
				if(oldTag.getInt("bedX") == newTag.getInt("bedX") && oldTag.getInt("bedY") == newTag.getInt("bedY") && oldTag.getInt("bedZ") == newTag.getInt("bedZ"))
				{
					if(oldTag.getInt("cooldown") < (newTag.getInt("cooldown") + 20) && oldTag.getInt("cooldown") > (newTag.getInt("cooldown") - 20))
						return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean showDurabilityBar(ItemStack itemStack)
	{
		CompoundNBT tagCompound = itemStack.getTag();
		if(tagCompound != null)
		{
			return tagCompound.getInt("cooldown") > 0 || tagCompound.getInt("castTime") > 0;
		}
		
		return false;
	}
	
	@Override
	public double getDurabilityForDisplay(ItemStack itemStack)
	{
		CompoundNBT tagCompound = itemStack.getTag();
		if(tagCompound.getInt("cooldown") > 0)
			return (double) tagCompound.getInt("cooldown") / (double) HearthstoneSettings.cooldown.get();
		else
			return (double) 1 - (tagCompound.getInt("castTime") / (double) HearthstoneSettings.channelTime.get());
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack itemStack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flagIn)
	{
		CompoundNBT tagCompound = itemStack.getTag();
		if(tagCompound != null)
		{
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(3);
			int cooldown = tagCompound.getInt("cooldown");
			float minutesExact, secondsExact;
			int minutes, seconds;
			minutesExact = cooldown / 1200;
			minutes = (int) minutesExact;
			secondsExact = cooldown / 20;
			seconds = (int) (secondsExact - (minutes * 60));
			tooltip.add(new StringTextComponent("Cooldown: " + minutes + " minutes " + seconds + " seconds"));
		}
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean hasEffect(ItemStack stack)
	{
		CompoundNBT tag = stack.getTag();
		if(tag != null)
		{
			return tag.getBoolean("isCasting");
		}
		return false;
	}
	
	/**
	 * A copy of {@link ServerPlayerEntity#changeDimension(DimensionType)} without any end- or nether-specific code
	 * 
	 * @param destination - destination dimension
	 * @param serverPlayer - player to change dimensions
	 * @return the player after changing dimensions
	 */
	private PlayerEntity changeDimension(DimensionType destination, ServerPlayerEntity serverPlayer)
	{
		if(!net.minecraftforge.common.ForgeHooks.onTravelToDimension(serverPlayer, destination))
			return null;
		// serverPlayer.invulnerableDimensionChange = true;
		DimensionType dimensiontype = serverPlayer.dimension;
		
		ServerWorld serverworld = serverPlayer.server.func_71218_a(dimensiontype);
		serverPlayer.dimension = destination;
		ServerWorld serverworld1 = serverPlayer.server.func_71218_a(destination);
		WorldInfo worldinfo = serverPlayer.world.getWorldInfo();
		net.minecraftforge.fml.network.NetworkHooks.sendDimensionDataPacket(serverPlayer.connection.netManager, serverPlayer);
		serverPlayer.connection.sendPacket(new SRespawnPacket(destination, worldinfo.getGenerator(), serverPlayer.interactionManager.getGameType()));
		serverPlayer.connection.sendPacket(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
		PlayerList playerlist = serverPlayer.server.getPlayerList();
		playerlist.updatePermissionLevel(serverPlayer);
		serverworld.removeEntity(serverPlayer, true); // Forge: the player entity is moved to the new world, NOT cloned. So keep the data alive with no matching invalidate call.
		serverPlayer.revive();
		double d0 = serverPlayer.posX;
		double d1 = serverPlayer.posY;
		double d2 = serverPlayer.posZ;
		float f = serverPlayer.rotationPitch;
		float f1 = serverPlayer.rotationYaw;
		double d3 = 8.0D;
		float f2 = f1;
		serverworld.getProfiler().startSection("moving");
		double moveFactor = serverworld.getDimension().getMovementFactor() / serverworld1.getDimension().getMovementFactor();
		d0 *= moveFactor;
		d2 *= moveFactor;
		
		serverPlayer.setLocationAndAngles(d0, d1, d2, f1, f);
		serverworld.getProfiler().endSection();
		serverworld.getProfiler().startSection("placing");
		double d7 = Math.min(-2.9999872E7D, serverworld1.getWorldBorder().minX() + 16.0D);
		double d4 = Math.min(-2.9999872E7D, serverworld1.getWorldBorder().minZ() + 16.0D);
		double d5 = Math.min(2.9999872E7D, serverworld1.getWorldBorder().maxX() - 16.0D);
		double d6 = Math.min(2.9999872E7D, serverworld1.getWorldBorder().maxZ() - 16.0D);
		d0 = MathHelper.clamp(d0, d7, d5);
		d2 = MathHelper.clamp(d2, d4, d6);
		serverPlayer.setLocationAndAngles(d0, d1, d2, f1, f);
		
		serverworld.getProfiler().endSection();
		serverPlayer.setWorld(serverworld1);
		serverworld1.func_217447_b(serverPlayer);
		serverPlayer.connection.setPlayerLocation(serverPlayer.posX, serverPlayer.posY, serverPlayer.posZ, f1, f);
		serverPlayer.interactionManager.func_73080_a(serverworld1);
		serverPlayer.connection.sendPacket(new SPlayerAbilitiesPacket(serverPlayer.abilities));
		playerlist.func_72354_b(serverPlayer, serverworld1);
		playerlist.sendInventory(serverPlayer);
		
		for(EffectInstance effectinstance : serverPlayer.getActivePotionEffects())
		{
			serverPlayer.connection.sendPacket(new SPlayEntityEffectPacket(serverPlayer.getEntityId(), effectinstance));
		}
		
		serverPlayer.connection.sendPacket(new SPlaySoundEventPacket(1032, BlockPos.ZERO, 0, false));
		// serverPlayer.lastExperience = -1;
		// serverPlayer.lastHealth = -1.0F;
		// serverPlayer.lastFoodLevel = -1;
		net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent(serverPlayer, dimensiontype, destination);
		return serverPlayer;
	}
}