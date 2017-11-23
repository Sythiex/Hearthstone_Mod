package net.minecraft.client.gui.recipebook;

import java.util.List;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRecipeUpdateListener
{
    void func_193001_a(List<IRecipe> p_193001_1_);
}