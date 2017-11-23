package net.minecraft.client.tutorial;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompletedTutorialStep implements ITutorialStep
{
    private final Tutorial field_193253_a;

    public CompletedTutorialStep(Tutorial p_i47584_1_)
    {
        this.field_193253_a = p_i47584_1_;
    }
}