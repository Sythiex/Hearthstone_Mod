package com.sythiex.hearthstonemod.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sythiex.hearthstonemod.client.HearthstoneArmPoses;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * Applies the shared first-person Hearthstone hand transform on Fabric.
 */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin
{
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void hearthstone$applyChannelingTransform(AbstractClientPlayer player, float partialTick, float interpolatedPitch, InteractionHand hand, float swingProgress,
            ItemStack itemStack, float equipProgress, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLight, CallbackInfo callbackInfo)
    {
        HumanoidArm mainArm = player.getMainArm();
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? mainArm : (mainArm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
        HearthstoneArmPoses.applyFirstPersonChannelingTransform(poseStack, player, arm, itemStack, partialTick);
    }
}
