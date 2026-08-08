package net.ty.createcraftedbeginning.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import java.util.List;

public interface ReactorKettleRecipeContext {
    Level getLevel();

    IItemHandler getItemCapability();

    IFluidHandler getFluidCapability();

    IGasHandler getGasCapability();

    IItemHandler getOutputItemCapability();

    IFluidHandler getOutputFluidCapability();

    IGasHandler getOutputGasCapability();

    float getRecipeTemperature();

    boolean matchesRecipeFilter(ReactorKettleRecipe recipe);

    boolean commitRecipeCraft(int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases);
}
