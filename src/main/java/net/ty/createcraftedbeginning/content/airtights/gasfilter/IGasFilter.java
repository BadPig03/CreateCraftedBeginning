package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

@FunctionalInterface
public interface IGasFilter {
    boolean test(ItemStack filterItem, GasStack filterGasStack);
}
