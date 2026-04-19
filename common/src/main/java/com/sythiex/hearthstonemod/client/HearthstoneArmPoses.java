package com.sythiex.hearthstonemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sythiex.hearthstonemod.content.HearthstoneContentIds;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Shared first-person and third-person pose helpers for Hearthstone channeling.
 */
public final class HearthstoneArmPoses
{
    private static final float BASE_ARM_PITCH = -1.55F;
    private static final float BASE_ARM_YAW = 0.25F;
    private static final float BASE_ARM_ROLL = 0.12F;
    private static final float BOB_SPEED = 0.18F;
    private static final float BOB_AMOUNT = 0.05F;
    private static final float TILT_SPEED = 0.15F;
    private static final float TILT_AMOUNT = 0.04F;
    private static final float FIRST_PERSON_CENTER_SHIFT_X = -0.2F;
    private static final float FIRST_PERSON_CENTER_SHIFT_Y = 0.06F;
    private static final float FIRST_PERSON_BASE_PITCH_DEGREES = 0.0F;
    private static final float FIRST_PERSON_BASE_YAW_DEGREES = -30.0F;
    private static final float FIRST_PERSON_BASE_ROLL_DEGREES = 0.0F;
    private static final float FIRST_PERSON_SWAY_PITCH_DEGREES = 12.0F;
    private static final float FIRST_PERSON_SWAY_YAW_DEGREES = 30.0F;
    private static final float FIRST_PERSON_SWAY_ROLL_DEGREES = 16.0F;
    private static final float FIRST_PERSON_SWAY_TRANSLATE_Y = 0.12F;

    private HearthstoneArmPoses()
    {}

    /**
     * Placeholder bootstrap hook kept for parity with the original mod structure.
     */
    public static void initialize()
    {}

    /**
     * Returns whether the entity's main arm should use the channeling pose.
     */
    public static boolean shouldPoseMainArm(LivingEntity entity)
    {
        return HearthstoneClientCasting.isEntityCasting(entity.getId()) && HearthstoneContentIds.isHearthstone(entity.getMainHandItem());
    }

    /**
     * Applies the centered first-person transform used while the local player
     * channels.
     */
    public static boolean applyFirstPersonChannelingTransform(PoseStack poseStack, AbstractClientPlayer player, HumanoidArm arm, ItemStack itemStack, float partialTick)
    {
        if (!HearthstoneClientCasting.isLocalStackCasting(itemStack) || arm != player.getMainArm())
        {
            return false;
        }

        float armSide = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float animationTime = player.tickCount + partialTick;
        float bob = Mth.sin(animationTime * BOB_SPEED) * BOB_AMOUNT;
        float tilt = Mth.cos(animationTime * TILT_SPEED) * TILT_AMOUNT;

        poseStack.translate(-armSide * FIRST_PERSON_CENTER_SHIFT_X, FIRST_PERSON_CENTER_SHIFT_Y + bob * FIRST_PERSON_SWAY_TRANSLATE_Y, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-armSide * FIRST_PERSON_BASE_YAW_DEGREES + tilt * FIRST_PERSON_SWAY_YAW_DEGREES * armSide));
        poseStack.mulPose(Axis.ZP.rotationDegrees(armSide * (FIRST_PERSON_BASE_ROLL_DEGREES + bob * FIRST_PERSON_SWAY_ROLL_DEGREES)));
        poseStack.mulPose(Axis.XP.rotationDegrees(FIRST_PERSON_BASE_PITCH_DEGREES + bob * FIRST_PERSON_SWAY_PITCH_DEGREES));
        return false;
    }

    /**
     * Applies the shared third-person arm pose for remote and local channeling
     * players.
     */
    public static void applyThirdPersonChannelingTransform(ModelPart armPart, LivingEntity entity, HumanoidArm arm)
    {
        if (!shouldPoseMainArm(entity) || arm != entity.getMainArm())
        {
            return;
        }

        float armSide = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float animationTime = entity.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float bob = Mth.sin(animationTime * BOB_SPEED) * BOB_AMOUNT;
        float tilt = Mth.cos(animationTime * TILT_SPEED) * TILT_AMOUNT;

        armPart.xRot = BASE_ARM_PITCH + bob;
        armPart.yRot = -BASE_ARM_YAW * armSide + tilt * 0.5F * armSide;
        armPart.zRot = BASE_ARM_ROLL * armSide + bob * 0.5F * armSide;
    }
}
