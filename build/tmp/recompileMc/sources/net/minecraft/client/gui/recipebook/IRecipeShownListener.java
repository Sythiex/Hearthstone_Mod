package net.minecraft.client.gui.recipebook;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRecipeShownListener
{
    void func_192043_J_();

    GuiRecipeBook func_194310_f();
}