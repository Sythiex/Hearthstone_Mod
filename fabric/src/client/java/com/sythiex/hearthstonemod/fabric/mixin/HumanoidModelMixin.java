package com.sythiex.hearthstonemod.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.sythiex.hearthstonemod.client.HearthstoneArmPoses;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/**
 * Applies the shared third-person channeling arm pose on Fabric.
 */
@Mixin(HumanoidModel.class)
abstract class HumanoidModelMixin<T extends LivingEntity>
{
    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Inject(method = "poseRightArm", at = @At("TAIL"))
    private void hearthstone$poseRightArm(T entity, CallbackInfo callbackInfo)
    {
        HearthstoneArmPoses.applyThirdPersonChannelingTransform(this.rightArm, entity, HumanoidArm.RIGHT);
    }

    @Inject(method = "poseLeftArm", at = @At("TAIL"))
    private void hearthstone$poseLeftArm(T entity, CallbackInfo callbackInfo)
    {
        HearthstoneArmPoses.applyThirdPersonChannelingTransform(this.leftArm, entity, HumanoidArm.LEFT);
    }
}
