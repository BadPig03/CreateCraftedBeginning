package net.ty.createcraftedbeginning.recipe.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ReactorKettleRecipeContext {
    @Nullable Level getLevel();

    IItemHandler getAvailableItems();

    IFluidHandler getAvailableFluids();

    GasStorageHandler getAvailableGases();

    IItemHandler getOutputItemCapability();

    IFluidHandler getOutputFluidCapability();

    GasHandler getOutputGasCapability();

    float getRecipeTemperature();

    boolean matchesRecipeFilter(ReactorKettleRecipe recipe);

    boolean commitRecipeCraft(int[] itemAmounts, int[] fluidAmounts, GasConsumptionPlan gasPlan, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases);
}
