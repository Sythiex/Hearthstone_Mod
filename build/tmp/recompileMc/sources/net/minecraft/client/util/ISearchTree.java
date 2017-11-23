package net.minecraft.client.util;

import java.util.List;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ISearchTree<T>
{
    List<T> func_194038_a(String p_194038_1_);
}