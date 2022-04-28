package com.sythiex.hearthstonemod;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;

import com.sythiex.hearthstonemod.HearthstoneConfig.HearthstoneSettings;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class ItemHearthstone extends Item
{
	public static final String NAME = "hearthstone";
	
	public final TranslatableComponent TEXT_ON_COOLDOWN = new TranslatableComponent("item.hearthstone.on_cooldown");
	public final TranslatableComponent TEXT_NO_BED = new TranslatableComponent("item.hearthstone.no_bed");
	public final TranslatableComponent TEXT_MISSING_BED = new TranslatableComponent("item.hearthstone.missing_bed");
	public final TranslatableComponent TEXT_LINKED = new TranslatableComponent("item.hearthstone.linked");
	public final TranslatableComponent TEXT_CANCELED = new TranslatableComponent("item.hearthstone.canceled");
	public final TranslatableComponent TEXT_COOLDOWN = new TranslatableComponent("item.hearthstone.cooldown");
	public final TranslatableComponent TEXT_MINUTES = new TranslatableComponent("item.hearthstone.minutes");
	public final TranslatableComponent TEXT_SECONDS = new TranslatableComponent("item.hearthstone.seconds");
	
	public final MutableComponent TEXT_HOME_SET = new TranslatableComponent("item.hearthstone.home_set");
	
	private Method createResourceKeyMethod = ObfuscationReflectionHelper.findMethod(ResourceKey.class, "m_135790_", ResourceLocation.class, ResourceLocation.class);
	
	public ItemHearthstone()
	{
		super(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
		setRegistryName(NAME);
	}
	
	@Override
	public void inventoryTick(ItemStack itemStack, Level world, Entity entity, int itemSlot, boolean isSelected)
	{
		// server side
		if(!world.isClientSide)
		{
			CompoundTag tag = itemStack.getTag();
			
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
				tag = new CompoundTag();
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
			if(tag.getBoolean("isCasting") && entity instanceof Player)
			{
				Player player = (Player) entity;
				
				// check if player is holding hearthstone
				ItemStack heldItem = player.getMainHandItem();
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
				double diffX = Math.abs(tag.getDouble("prevX") - player.getX());
				double diffY = Math.abs(tag.getDouble("prevY") - player.getY());
				double diffZ = Math.abs(tag.getDouble("prevZ") - player.getZ());
				// if player moves or swaps items cancel cast
				if(((diffX > 0.05 || diffY > 0.05 || diffZ > 0.05) && tag.getDouble("prevY") != -1) || tag.getBoolean("stopCasting"))
				{
					tag.putInt("castTime", 0);
					tag.putBoolean("isCasting", false);
					tag.putBoolean("stopCasting", false);
					player.displayClientMessage(TEXT_CANCELED, true);
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
					
					world.playSound(null, player.getX(), player.getY(), player.getZ(), HearthstoneMod.castSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
					
					// get bed location
					int bedX = tag.getInt("bedX");
					int bedY = tag.getInt("bedY");
					int bedZ = tag.getInt("bedZ");
					
					// create dimension registry key from NBT
					ResourceKey<Level> savedDimensionKey = getWorldResourceKey(tag.getString("dimensionResourceLocationParent"), tag.getString("dimensionResourceLocation"));
					ResourceKey<Level> playerDimensionKey = player.level.dimension();
					
					// if player is not in same dimension as bed, travel to that dimension
					if(savedDimensionKey != null)
					{
						if(playerDimensionKey.compareTo(savedDimensionKey) != 0)
						{
							MinecraftServer server = player.level.getServer();
							ServerLevel destinationServerLevel = server.getLevel(savedDimensionKey);
							ServerPlayer serverPlayer = (ServerPlayer) player;
							player = (Player) serverPlayer.changeDimension(destinationServerLevel, new HearthstoneTeleporter(destinationServerLevel));
							player.moveTo(bedX, bedY, bedZ); // , player.rotationYaw, player.rotationPitch);
							world = player.level;
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
						boolean north = player.level.getBlockState(bedPos.north()).getBlock().isPossibleToRespawnInThis();
						boolean northUp = player.level.getBlockState(bedPos.north().above()).getBlock().isPossibleToRespawnInThis();
						boolean northDown = player.level.getBlockState(bedPos.north().below()).getMaterial().isSolid();
						
						boolean east = player.level.getBlockState(bedPos.east()).getBlock().isPossibleToRespawnInThis();
						boolean eastUp = player.level.getBlockState(bedPos.east().above()).getBlock().isPossibleToRespawnInThis();
						boolean eastDown = player.level.getBlockState(bedPos.east().below()).getMaterial().isSolid();
						
						boolean south = player.level.getBlockState(bedPos.south()).getBlock().isPossibleToRespawnInThis();
						boolean southUp = player.level.getBlockState(bedPos.south().above()).getBlock().isPossibleToRespawnInThis();
						boolean southDown = player.level.getBlockState(bedPos.south().below()).getMaterial().isSolid();
						
						boolean west = player.level.getBlockState(bedPos.west()).getBlock().isPossibleToRespawnInThis();
						boolean westUp = player.level.getBlockState(bedPos.west().above()).getBlock().isPossibleToRespawnInThis();
						boolean westDown = player.level.getBlockState(bedPos.west().below()).getMaterial().isSolid();
						
						// tp player next to bed
						if(north && northUp && northDown)
						{
							player.teleportTo(bedPos.north().getX() + 0.5, bedPos.north().getY(), bedPos.north().getZ() + 0.5);
						}
						else if(east && eastUp && eastDown)
						{
							player.teleportTo(bedPos.east().getX() + 0.5, bedPos.east().getY(), bedPos.east().getZ() + 0.5);
						}
						else if(south && southUp && southDown)
						{
							player.teleportTo(bedPos.south().getX() + 0.5, bedPos.south().getY(), bedPos.south().getZ() + 0.5);
						}
						else if(west && westUp && westDown)
						{
							player.teleportTo(bedPos.west().getX() + 0.5, bedPos.west().getY(), bedPos.west().getZ() + 0.5);
						}
						else // if no open space, tp player on top of bed
						{
							player.teleportTo(bedX + 0.5, bedY + 1, bedZ + 0.5);
						}
						
						world.playSound(null, player.getX(), player.getY(), player.getZ(), HearthstoneMod.impactSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
						
						// sets hearthstone on cooldown
						tag.putInt("cooldown", HearthstoneSettings.cooldown.get());
					}
					// tps player to where bed was, then breaks link
					else
					{
						player.teleportTo(bedX + 0.5, bedY + 1, bedZ + 0.5);
						world.playSound(null, player.getX(), player.getY(), player.getZ(), HearthstoneMod.impactSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
						// sets hearthstone on cooldown
						tag.putInt("cooldown", HearthstoneSettings.cooldown.get());
						// clears the saved dimension
						tag.putString("dimensionResourceLocationParent", "");
						tag.putString("dimensionResourceLocation", "");
						// informs player of broken link
						player.displayClientMessage(TEXT_MISSING_BED, true);
					}
				}
			}
			// record position of player for detecting movement
			tag.putDouble("prevX", entity.getX());
			tag.putDouble("prevY", entity.getY());
			tag.putDouble("prevZ", entity.getZ());
			
			// save tag
			itemStack.setTag(tag);
		}
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand)
	{
		if(!world.isClientSide && hand == InteractionHand.MAIN_HAND)
		{
			ItemStack itemStack = player.getItemInHand(hand);
			CompoundTag tagCompound = itemStack.getTag();
			
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
							world.playSound(player, player.getX(), player.getY(), player.getZ(), HearthstoneMod.channelSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
						}
					}
					// if on cooldown
					else
					{
						player.displayClientMessage(TEXT_ON_COOLDOWN, true);
					}
				}
				// if location is not set
				else
				{
					player.displayClientMessage(TEXT_NO_BED, true);
				}
			}
			// save tag
			itemStack.setTag(tagCompound);
		}
		return new InteractionResultHolder(InteractionResult.PASS, player.getItemInHand(hand));
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		if(!context.getLevel().isClientSide())
		{
			ItemStack itemStack = context.getPlayer().getItemInHand(InteractionHand.MAIN_HAND);
			// if main hand is not a hearthstone, return
			if(itemStack.getItem() != HearthstoneMod.hearthstone)
			{
				return InteractionResult.FAIL;
			}
			
			CompoundTag tagCompound = itemStack.getTag();
			
			// if sneaking
			if(context.getPlayer().isCrouching())
			{
				// checks if block right clicked is bed
				BlockState state = context.getLevel().getBlockState(context.getClickedPos());
				if(context.getLevel().getBlockState(context.getClickedPos()).getBlock().isBed(state, context.getLevel(), context.getClickedPos(), context.getPlayer()))
				{
					// links bed to hearthstone
					ResourceKey<Level> dimensionKey = context.getPlayer().level.dimension();
					
					tagCompound.putInt("bedX", context.getClickedPos().getX());
					tagCompound.putInt("bedY", context.getClickedPos().getY());
					tagCompound.putInt("bedZ", context.getClickedPos().getZ());
					tagCompound.putString("dimensionResourceLocationParent", dimensionKey.getRegistryName().toString());
					tagCompound.putString("dimensionResourceLocation", dimensionKey.location().toString());
					context.getPlayer().displayClientMessage(TEXT_LINKED, true);
				}
				// save tag
				itemStack.setTag(tagCompound);
				return InteractionResult.SUCCESS;
			}
			else
			{
				use(context.getLevel(), context.getPlayer(), InteractionHand.MAIN_HAND);
			}
		}
		return InteractionResult.FAIL;
	}
	
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		if(oldStack.getItem() == newStack.getItem())
		{
			CompoundTag oldTag = oldStack.getTag();
			CompoundTag newTag = newStack.getTag();
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
	public boolean isBarVisible(ItemStack itemStack)
	{
		CompoundTag tagCompound = itemStack.getTag();
		if(tagCompound != null)
		{
			return tagCompound.getInt("cooldown") > 0 || tagCompound.getInt("castTime") > 0;
		}
		
		return false;
	}
	
	@Override
	public int getBarWidth(ItemStack itemStack)
	{
		CompoundTag tagCompound = itemStack.getTag();
		if(tagCompound.getInt("cooldown") > 0)
			return Math.round(13.0F - (float)tagCompound.getInt("cooldown") * 13.0F / (float)HearthstoneSettings.cooldown.get());
		else
			return Math.round(13.0F - ((float)HearthstoneSettings.channelTime.get() - (float)tagCompound.getInt("castTime")) * 13.0F / (float)HearthstoneSettings.channelTime.get());
	}
	
	@Override
	public int getBarColor(ItemStack itemStack)
	{
		CompoundTag tagCompound = itemStack.getTag();
		if(tagCompound.getInt("cooldown") > 0)
			return Color.decode("#ff0000").hashCode(); // red
		else
			return Color.decode("#4bc1e3").hashCode(); // blue
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemStack, @Nullable Level world, List<Component> tooltip, TooltipFlag flagIn)
	{
		CompoundTag tag = itemStack.getTag();
		if(tag != null)
		{
			// display if hearthstone is linked to a bed
			if(tag.getString("dimensionResourceLocation") != "")
				tooltip.add(TEXT_HOME_SET.withStyle(ChatFormatting.GRAY));
			
			// calculates and displays cooldown in minutes and seconds
			int cooldown = tag.getInt("cooldown");
			if(cooldown != 0)
			{
				cooldown += 19; // more intuitive cooldown timer
				float minutesExact, secondsExact;
				int minutes, seconds;
				minutesExact = cooldown / 1200;
				minutes = (int) minutesExact;
				secondsExact = cooldown / 20;
				seconds = (int) (secondsExact - (minutes * 60));
				
				MutableComponent MutableComponent = new TextComponent("").append(TEXT_COOLDOWN).append(Integer.toString(minutes)).append(TEXT_MINUTES).append(Integer.toString(seconds)).append(TEXT_SECONDS);
				tooltip.add(MutableComponent.withStyle(ChatFormatting.GRAY));
			}
		}
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack stack)
	{
		CompoundTag tag = stack.getTag();
		if(tag != null)
		{
			return tag.getBoolean("isCasting");
		}
		return false;
	}
	
	/**
	 * Gets {@code ResourceKey<Level>} for a dimension using {@link ResourceKey#create(ResourceLocation, ResourceLocation)}, but uses Strings instead of ResourceLocations
	 * 
	 * @param locationParent ResourceLocation parent represented as a String
	 * @param location       ResourceLocation location represented as a String
	 * @return {@code ResourceKey<Level>}, or null if either String is empty
	 */
	
	private ResourceKey<Level> getWorldResourceKey(String locationParent, String location)
	{
		if(locationParent != "" && location != "")
		{
			ResourceLocation dimensionResourceLocationParent = new ResourceLocation(locationParent);
			ResourceLocation dimensionResourceLocation = new ResourceLocation(location);
			try
			{
				return (ResourceKey<Level>) createResourceKeyMethod.invoke(null, dimensionResourceLocationParent, dimensionResourceLocation);
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