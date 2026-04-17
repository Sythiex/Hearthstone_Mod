package com.sythiex.hearthstonemod.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
/**
 * Shared first-person and third-person animation helpers for Hearthstone
 * channeling.
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

    private static final HumanoidModel.ArmPose HEARTHSTONE_CHANNELING = HumanoidModel.ArmPose.create("HEARTHSTONE_CHANNELING", false, HearthstoneArmPoses::applyThirdPersonChannelingTransform);

    private HearthstoneArmPoses()
    {}

    /**
     * Forces class initialization during client setup so the custom arm pose is
     * registered before the player renderer builds its internal pose tables.
     */
    public static void initialize()
    {}

    /**
     * Returns the custom third-person pose for entities currently casting.
     *
     * @param entity entity being rendered
     * @return custom arm pose while casting, or {@code null} to use vanilla poses
     */
    @Nullable
    public static HumanoidModel.ArmPose getChannelingPose(LivingEntity entity)
    {
        return HearthstoneClientCasting.isEntityCasting(entity.getId()) ? HEARTHSTONE_CHANNELING : null;
    }

    /**
     * Applies the first-person sway used while the local player channels the
     * Hearthstone.
     *
     * @param poseStack   transform stack for the held item
     * @param player      local player being rendered
     * @param arm         arm currently being rendered
     * @param itemStack   stack currently in that hand
     * @param partialTick partial tick used for smoothing
     * @return {@code false} so vanilla first-person item rendering still runs after
     *         the custom transform is applied
     */
    public static boolean applyFirstPersonChannelingTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemStack, float partialTick)
    {
        if (!HearthstoneClientCasting.isLocalStackCasting(itemStack) || arm != player.getMainArm())
        {
            return false;
        }

        float armSide = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float animationTime = player.tickCount + partialTick;
        float bob = Mth.sin(animationTime * BOB_SPEED) * BOB_AMOUNT;
        float tilt = Mth.cos(animationTime * TILT_SPEED) * TILT_AMOUNT;

        // Nudge and tilt the item closer to screen center, then add a subtle sway so
        // the first-person presentation matches the third-person channeling pose.
        poseStack.translate(-armSide * FIRST_PERSON_CENTER_SHIFT_X, FIRST_PERSON_CENTER_SHIFT_Y + bob * FIRST_PERSON_SWAY_TRANSLATE_Y, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-armSide * FIRST_PERSON_BASE_YAW_DEGREES + tilt * FIRST_PERSON_SWAY_YAW_DEGREES * armSide));
        poseStack.mulPose(Axis.ZP.rotationDegrees(armSide * (FIRST_PERSON_BASE_ROLL_DEGREES + bob * FIRST_PERSON_SWAY_ROLL_DEGREES)));
        poseStack.mulPose(Axis.XP.rotationDegrees(FIRST_PERSON_BASE_PITCH_DEGREES + bob * FIRST_PERSON_SWAY_PITCH_DEGREES));
        return false;
    }

    /**
     * Applies the custom third-person arm rotation for channeling players.
     *
     * @param model  humanoid model being posed
     * @param entity entity being rendered
     * @param arm    arm that should receive the pose
     */
    private static void applyThirdPersonChannelingTransform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm)
    {
        ModelPart armPart = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float armSide = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float animationTime = entity.tickCount + Minecraft.getInstance().getFrameTime();
        float bob = Mth.sin(animationTime * BOB_SPEED) * BOB_AMOUNT;
        float tilt = Mth.cos(animationTime * TILT_SPEED) * TILT_AMOUNT;

        // Present the Hearthstone forward with a slight inward angle
        armPart.xRot = BASE_ARM_PITCH + bob;
        armPart.yRot = -BASE_ARM_YAW * armSide + tilt * 0.5F * armSide;
        armPart.zRot = BASE_ARM_ROLL * armSide + bob * 0.5F * armSide;
    }
}
