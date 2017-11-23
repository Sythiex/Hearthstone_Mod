package net.minecraft.client.gui.advancements;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum AdvancementState
{
    OBTAINED(0),
    UNOBTAINED(1);

    private final int field_192671_d;

    private AdvancementState(int p_i47384_3_)
    {
        this.field_192671_d = p_i47384_3_;
    }

    public int func_192667_a()
    {
        return this.field_192671_d;
    }
}