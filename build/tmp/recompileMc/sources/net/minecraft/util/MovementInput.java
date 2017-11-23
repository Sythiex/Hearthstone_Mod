package net.minecraft.util;

import net.minecraft.util.math.Vec2f;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MovementInput
{
    /** The speed at which the player is strafing. Postive numbers to the left and negative to the right. */
    public float moveStrafe;
    public float field_192832_b;
    public boolean forwardKeyDown;
    public boolean backKeyDown;
    public boolean leftKeyDown;
    public boolean rightKeyDown;
    public boolean jump;
    public boolean sneak;

    public void updatePlayerMoveState()
    {
    }

    public Vec2f getMoveVector()
    {
        return new Vec2f(this.moveStrafe, this.field_192832_b);
    }
}