package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipeOutputs.Outputs;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasRecipePressureSpeed;
import net.ty.createcraftedbeginning.recipe.interfaces.ReactorKettleRecipeContext;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ReactorKettleCraftPlanner {
    private final ReactorKettleRecipeContext kettle;
    private final ReactorKettleRecipe recipe;

    public ReactorKettleCraftPlanner(ReactorKettleRecipeContext kettle, ReactorKettleRecipe recipe) {
        this.kettle = kettle;
        this.recipe = recipe;
    }

    public float getPressureSpeedMultiplier() {
        return GasConsumptionPlanner.plan(recipe.getGasRequirements(), kettle.getAvailableGases()).map(GasRecipePressureSpeed::multiplier).orElse(1.0F);
    }

    public Optional<Plan> plan() {
        return planInputs().flatMap(inputs -> acceptOutputs(inputs, new ReactorKettleRecipeOutputs(recipe).preview(inputs)));
    }

    public boolean matches() {
        return kettle.matchesRecipeFilter(recipe) && plan().isPresent();
    }

    Optional<Inputs> planInputs() {
        IItemHandler availableItems = kettle.getAvailableItems();
        IFluidHandler availableFluids = kettle.getAvailableFluids();
        GasStorageHandler availableGases = kettle.getAvailableGases();
        if (!recipe.getTemperatureRecipeData().test(kettle.getRecipeTemperature())) {
            return Optional.empty();
        }

        Level level = kettle.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        int[] plannedItemAmounts = new int[availableItems.getSlots()];
        int[] plannedFluidAmounts = new int[availableFluids.getTanks()];
        Optional<GasConsumptionPlan> gasPlan = GasConsumptionPlanner.plan(recipe.getGasRequirements(), availableGases);
        if (gasPlan.isEmpty() || !planInputConsumption(availableItems, availableFluids, plannedItemAmounts, plannedFluidAmounts)) {
            return Optional.empty();
        }

        GasConsumptionPlan gasConsumption = gasPlan.get();
        long[] plannedGasUnits = gasConsumption.tankAmounts();
        return Optional.of(new Inputs(level, availableItems, availableFluids, availableGases, plannedItemAmounts, plannedFluidAmounts, gasConsumption, plannedGasUnits));
    }

    Optional<Plan> acceptOutputs(Inputs inputs, Outputs outputs) {
        if (!new ReactorKettleOutputSimulation(kettle).accepts(inputs.items(), inputs.fluids(), inputs.gases(), outputs.items(), outputs.fluids(), outputs.gases(), inputs.itemAmounts(), inputs.fluidAmounts(), inputs.gasUnits())) {
            return Optional.empty();
        }

        return Optional.of(new Plan(inputs.itemAmounts(), inputs.fluidAmounts(), inputs.gasPlan(), outputs.items(), outputs.fluids(), outputs.gases()));
    }

    private boolean planInputConsumption(IItemHandler availableItems, IFluidHandler availableFluids, int[] itemAmounts, int[] fluidAmounts) {
        return RecipeInputAllocation.planItems(recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList(), availableItems, itemAmounts) && RecipeInputAllocation.planFluids(recipe.getFluidIngredients(), availableFluids, fluidAmounts);
    }

    public record Plan(int[] itemAmounts, int[] fluidAmounts, GasConsumptionPlan gasPlan, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        public Plan {
            itemAmounts = itemAmounts.clone();
            fluidAmounts = fluidAmounts.clone();
            outputItems = outputItems.stream().map(ItemStack::copy).toList();
            outputFluids = outputFluids.stream().map(FluidStack::copy).toList();
            outputGases = outputGases.stream().map(GasStack::copy).toList();
        }

        @Override
        public int[] itemAmounts() {
            return itemAmounts.clone();
        }

        @Override
        public int[] fluidAmounts() {
            return fluidAmounts.clone();
        }

        @Override
        public @Unmodifiable List<ItemStack> outputItems() {
            return outputItems.stream().map(ItemStack::copy).toList();
        }

        @Override
        public @Unmodifiable List<FluidStack> outputFluids() {
            return outputFluids.stream().map(FluidStack::copy).toList();
        }

        @Override
        public @Unmodifiable List<GasStack> outputGases() {
            return outputGases.stream().map(GasStack::copy).toList();
        }
    }

    record Inputs(Level level, IItemHandler items, IFluidHandler fluids, GasStorageHandler gases, int[] itemAmounts, int[] fluidAmounts, GasConsumptionPlan gasPlan, long[] gasUnits) {}
}
