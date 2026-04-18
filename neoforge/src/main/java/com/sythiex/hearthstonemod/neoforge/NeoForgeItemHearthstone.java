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

public final class NeoForgeItemHearthstone extends ItemHearthstone {
    public NeoForgeItemHearthstone() {
        super(HearthstoneContentFactory.createHearthstoneProperties());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
                return HearthstoneArmPoses.applyFirstPersonChannelingTransform(poseStack, player, arm, itemInHand, partialTick);
            }
        });
    }
}
