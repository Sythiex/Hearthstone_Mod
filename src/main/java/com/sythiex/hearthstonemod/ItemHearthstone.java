package com.sythiex.hearthstonemod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ItemHearthstone extends Item
{
	public static final String NAME = "hearthstone";
	
	public final TranslationTextComponent TEXT_ON_COOLDOWN = new TranslationTextComponent("item.hearthstone.on_cooldown");
	public final TranslationTextComponent TEXT_NO_BED = new TranslationTextComponent("item.hearthstone.no_bed");
	public final TranslationTextComponent TEXT_MISSING_BED = new TranslationTextComponent("item.hearthstone.missing_bed");
	public final TranslationTextComponent TEXT_LINKED = new TranslationTextComponent("item.hearthstone.linked");
	public final TranslationTextComponent TEXT_CANCELED = new TranslationTextComponent("item.hearthstone.canceled");
	
	private Method getOrCreateKeyMethod = ObfuscationReflectionHelper.findMethod(RegistryKey.class, "func_240905_a_", ResourceLocation.class, ResourceLocation.class);
	
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
				tag.putString("dimensionResourceLocationParent", "");
				tag.putString("dimensionResourceLocation", "");
				tag.putDouble("prevX", -1);
				tag.putDouble("prevY", -1);
				tag.putDouble("prevZ", -1);
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
				double diffX = Math.abs(tag.getDouble("prevX") - player.getPosX());
				double diffY = Math.abs(tag.getDouble("prevY") - player.getPosY());
				double diffZ = Math.abs(tag.getDouble("prevZ") - player.getPosZ());
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
					
					world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), HearthstoneMod.castSoundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
					
					// get bed location
					int bedX = tag.getInt("bedX");
					int bedY = tag.getInt("bedY");
					int bedZ = tag.getInt("bedZ");
					
					// create dimension registry key from NBT
					RegistryKey<World> savedDimensionKey = getWorldRegistryKey(tag.getString("dimensionResourceLocationParent"), tag.getString("dimensionResourceLocation"));
					RegistryKey<World> playerDimensionKey = player.getEntityWorld().getDimensionKey();
					
					// if player is not in same dimension as bed, travel to that dimension
					if(savedDimensionKey != null)
					{
						if(playerDimensionKey.compareTo(savedDimensionKey) != 0)
						{
							MinecraftServer server = player.getEntityWorld().getServer();
							ServerWorld destinationServerWorld = server.getWorld(savedDimensionKey);
							ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
							player = (PlayerEntity) serverPlayer.changeDimension(destinationServerWorld, new HearthstoneTeleporter(destinationServerWorld));
							player.setLocationAndAngles(bedX, bedY, bedZ, player.rotationYaw, player.rotationPitch);
							world = player.getEntityWorld();
						}
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
						
						world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), HearthstoneMod.impactSoundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
						
						// sets hearthstone on cooldown
						tag.putInt("cooldown", HearthstoneSettings.cooldown.get());
					}
					// tps player to where bed was, then breaks link
					else
					{
						player.setPositionAndUpdate(bedX + 0.5, bedY + 1, bedZ + 0.5);
						world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), HearthstoneMod.impactSoundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
						// sets hearthstone on cooldown
						tag.putInt("cooldown", HearthstoneSettings.cooldown.get());
						// clears the saved dimension
						tag.putString("dimensionResourceLocationParent", "");
						tag.putString("dimensionResourceLocation", "");
						// informs player of broken link
						player.sendStatusMessage(TEXT_MISSING_BED, true);
					}
				}
			}
			// record position of player for detecting movement
			tag.putDouble("prevX", entity.getPosX());
			tag.putDouble("prevY", entity.getPosY());
			tag.putDouble("prevZ", entity.getPosZ());
			
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
			if(!player.isCrouching())
			{
				// if location is set
				if(tagCompound.getString("dimensionResourceLocation") != "")
				{
					int cooldown = tagCompound.getInt("cooldown");
					
					// if off cooldown
					if(cooldown == 0)
					{
						// if player is not casting, start casting
						if(!tagCompound.getBoolean("isCasting"))
						{
							tagCompound.putBoolean("isCasting", true);
							world.playSound(player, player.getPosX(), player.getPosY(), player.getPosZ(), HearthstoneMod.channelSoundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
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
			if(context.getPlayer().isCrouching())
			{
				// checks if block right clicked is bed
				BlockState state = context.getWorld().getBlockState(context.getPos());
				if(context.getWorld().getBlockState(context.getPos()).getBlock().isBed(state, context.getWorld(), context.getPos(), context.getPlayer()))
				{
					// links bed to hearthstone
					RegistryKey<World> dimensionKey = context.getPlayer().getEntityWorld().getDimensionKey();
					
					tagCompound.putInt("bedX", context.getPos().getX());
					tagCompound.putInt("bedY", context.getPos().getY());
					tagCompound.putInt("bedZ", context.getPos().getZ());
					tagCompound.putString("dimensionResourceLocationParent", dimensionKey.getRegistryName().toString());
					tagCompound.putString("dimensionResourceLocation", dimensionKey.getLocation().toString());
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
			// calculates and displays cooldown in minutes and seconds
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(3);
			int cooldown = tagCompound.getInt("cooldown");
			if(cooldown != 0)
			{
				cooldown += 20; // more intuitive cooldown timer
				float minutesExact, secondsExact;
				int minutes, seconds;
				minutesExact = cooldown / 1200;
				minutes = (int) minutesExact;
				secondsExact = cooldown / 20;
				seconds = (int) (secondsExact - (minutes * 60));
				tooltip.add(new StringTextComponent("Cooldown: " + minutes + " minutes " + seconds + " seconds"));
			}
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
	 * Gets {@code RegistryKey<World>} for a dimension using {@link RegistryKey<T>#getOrCreateKey(ResourceLocation parent, ResourceLocation location)}, but uses Strings instead of ResourceLocations
	 * 
	 * @param locationParent ResourceLocation parent represented as a String
	 * @param location       ResourceLocation location represented as a String
	 * @return {@code RegistryKey<World>}, or null if either String is empty
	 */
	private RegistryKey<World> getWorldRegistryKey(String locationParent, String location)
	{
		if(locationParent != "" && location != "")
		{
			ResourceLocation dimensionResourceLocationParent = new ResourceLocation(locationParent);
			ResourceLocation dimensionResourceLocation = new ResourceLocation(location);
			try
			{
				return (RegistryKey<World>) getOrCreateKeyMethod.invoke(null, dimensionResourceLocationParent, dimensionResourceLocation);
			}
			catch(IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch(IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch(InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}
}