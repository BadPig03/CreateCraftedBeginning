package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ReactorKettleRecipeContext {
    @Nullable Level getLevel();

    IItemHandler getAvailableItems();

    IFluidHandler getAvailableFluids();

    IGasHandler getAvailableGases();

    IItemHandler getOutputItemCapability();

    IFluidHandler getOutputFluidCapability();

    IGasHandler getOutputGasCapability();

    float getRecipeTemperature();

    boolean matchesRecipeFilter(ReactorKettleRecipe recipe);

    boolean commitRecipeCraft(int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases);
}
