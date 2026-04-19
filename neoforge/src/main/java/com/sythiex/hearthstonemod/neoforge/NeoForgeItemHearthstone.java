package com.sythiex.hearthstonemod.neoforge;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sythiex.hearthstonemod.client.HearthstoneArmPoses;
import com.sythiex.hearthstonemod.content.HearthstoneContentFactory;
import com.sythiex.hearthstonemod.content.ItemHearthstone;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * NeoForge-specific item wrapper that exposes the shared first-person hand
 * transform.
 */
public final class NeoForgeItemHearthstone extends ItemHearthstone
{
    public NeoForgeItemHearthstone()
    {
        super(HearthstoneContentFactory.createHearthstoneProperties());
    }

    /**
     * Installs the client item extension used for the first-person channeling pose.
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess,
                    float swingProcess)
            {
                return HearthstoneArmPoses.applyFirstPersonChannelingTransform(poseStack, player, arm, itemInHand, partialTick);
            }
        });
    }
}
