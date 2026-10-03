package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipeLookup;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipeLookup.RecipeMatch;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberBasinPlanner {
    private final GasInjectionChamberBlockEntity chamber;

    @Internal
    public GasInjectionChamberBasinPlanner(GasInjectionChamberBlockEntity chamber) {
        this.chamber = chamber;
    }

    static boolean canDrainItems(IItemHandler items, List<ItemDrain> drainPlan) {
        for (ItemDrain itemDrain : drainPlan) {
            ItemStack simulatedDrain = items.extractItem(itemDrain.slot(), itemDrain.count(), true);
            if (simulatedDrain.getCount() == itemDrain.count() && ItemStack.isSameItemSameComponents(simulatedDrain, itemDrain.expectedStack())) {
                continue;
            }

            return false;
        }

        return true;
    }

    static boolean canDrainFluids(IFluidHandler fluids, List<FluidStack> drainPlan) {
        for (FluidStack drainRequest : drainPlan) {
            FluidStack simulatedDrain = fluids.drain(drainRequest, FluidAction.SIMULATE);
            if (simulatedDrain.getAmount() == drainRequest.getAmount() && FluidStack.isSameFluidSameComponents(simulatedDrain, drainRequest)) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static @Nullable List<ItemDrain> createItemDrainPlan(Ingredient ingredient, IItemHandler items, int batchSize) {
        if (batchSize <= 0) {
            return null;
        }

        int remainingCount = batchSize;
        List<ItemDrain> drainPlan = new ArrayList<>();
        for (int slot = 0; slot < items.getSlots() && remainingCount > 0; slot++) {
            ItemStack itemStack = items.getStackInSlot(slot);
            if (itemStack.isEmpty() || !ingredient.test(itemStack)) {
                continue;
            }

            int drainCount = Math.min(remainingCount, itemStack.getCount());
            if (drainCount <= 0) {
                continue;
            }

            drainPlan.add(new ItemDrain(slot, itemStack.copy(), drainCount));
            remainingCount -= drainCount;
        }

        if (remainingCount != 0) {
            return null;
        }

        return drainPlan;
    }

    private static @Nullable List<FluidStack> createFluidDrainPlan(SizedFluidIngredient ingredient, IFluidHandler fluids, int batchSize) {
        if (batchSize <= 0) {
            return null;
        }

        long requiredAmount = (long) ingredient.amount() * batchSize;
        if (requiredAmount <= 0 || requiredAmount > Integer.MAX_VALUE) {
            return null;
        }

        int remainingAmount = (int) requiredAmount;
        List<FluidStack> drainPlan = new ArrayList<>();
        for (int tankIndex = 0; tankIndex < fluids.getTanks() && remainingAmount > 0; tankIndex++) {
            FluidStack fluidStack = fluids.getFluidInTank(tankIndex);
            if (fluidStack.isEmpty() || !ingredient.test(fluidStack)) {
                continue;
            }

            int drainAmount = Math.min(remainingAmount, fluidStack.getAmount());
            if (drainAmount <= 0) {
                continue;
            }

            FluidStack drainRequest = fluidStack.copyWithAmount(drainAmount);
            boolean mergedWithExisting = false;
            for (FluidStack plannedDrain : drainPlan) {
                if (!FluidStack.isSameFluidSameComponents(plannedDrain, drainRequest)) {
                    continue;
                }

                plannedDrain.setAmount(plannedDrain.getAmount() + drainAmount);
                mergedWithExisting = true;
                break;
            }

            if (!mergedWithExisting) {
                drainPlan.add(drainRequest);
            }
            remainingAmount -= drainAmount;
        }

        if (remainingAmount != 0) {
            return null;
        }

        return drainPlan;
    }

    private static long getMatchingItemCount(Ingredient ingredient, IItemHandler items) {
        long matchingCount = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack itemStack = items.getStackInSlot(slot);
            if (!(!itemStack.isEmpty() && ingredient.test(itemStack))) {
                continue;
            }

            matchingCount += itemStack.getCount();
        }
        return matchingCount;
    }

    private static long getMatchingFluidAmount(SizedFluidIngredient ingredient, IFluidHandler fluids) {
        long matchingAmount = 0;
        for (int tankIndex = 0; tankIndex < fluids.getTanks(); tankIndex++) {
            FluidStack fluidStack = fluids.getFluidInTank(tankIndex);
            if (fluidStack.isEmpty() || !ingredient.test(fluidStack)) {
                continue;
            }

            matchingAmount += fluidStack.getAmount();
        }
        return matchingAmount;
    }

    private static boolean matchesBasinFilter(BasinBlockEntity basin, GasInjectionRecipe recipe) {
        FilteringBehaviour filter = basin.getFilter();
        if (filter == null) {
            return false;
        }

        if (recipe.hasItemOutput()) {
            List<ProcessingOutput> outputs = recipe.getRollableResults();
            if (outputs.isEmpty()) {
                return false;
            }

            ItemStack firstOutput = outputs.getFirst().getStack();
            return !firstOutput.isEmpty() && filter.test(firstOutput);
        }

        FluidStack fluidResult = recipe.getFluidResult();
        return !fluidResult.isEmpty() && filter.test(fluidResult);
    }

    private static boolean canProcessBatch(BasinBlockEntity basin, IItemHandler items, IFluidHandler fluids, GasInjectionRecipe recipe, int batchSize) {
        List<ItemDrain> itemDrainPlan = recipe.hasItemInput() ? createItemDrainPlan(recipe.getIngredient(), items, batchSize) : List.of();
        List<FluidStack> fluidDrainPlan = recipe.hasFluidInput() ? createFluidDrainPlan(recipe.getFluidIngredient(), fluids, batchSize) : List.of();
        if (itemDrainPlan == null || fluidDrainPlan == null || !canDrainItems(items, itemDrainPlan) || !canDrainFluids(fluids, fluidDrainPlan)) {
            return false;
        }

        GasInjectionChamberBasinOutputs outputs = new GasInjectionChamberBasinOutputs();
        List<ItemStack> itemResults = GasInjectionChamberBasinOutputs.createPotentialItemResults(recipe, batchSize);
        FluidStack fluidResult = outputs.getBatchFluidResult(recipe, batchSize);
        if (recipe.hasItemOutput() && itemResults.isEmpty() || recipe.hasFluidOutput() && fluidResult.isEmpty()) {
            return false;
        }

        List<FluidStack> fluidResults = fluidResult.isEmpty() ? List.of() : List.of(fluidResult);
        return basin.acceptOutputs(itemResults, fluidResults, true);
    }

    @Internal
    public Optional<BasinPlan> createPlan(BasinBlockEntity basin, @Nullable GasInjectionRecipe recipe) {
        return planInputs(basin, recipe).flatMap(inputs -> new GasInjectionChamberBasinOutputs().preview(basin, inputs));
    }

    Optional<BasinPlan> createPlan(BasinBlockEntity basin) {
        Level level = chamber.getLevel();
        if (level == null || basin.inputTank == null) {
            return Optional.empty();
        }

        IItemHandler inputItems = basin.getInputInventory();
        IFluidHandler inputFluids = basin.inputTank.getCapability();
        Optional<RecipeMatch> recipeMatch = new GasInjectionRecipeLookup(level, chamber.getGasTank()).findBasinRecipeMatch(inputItems, inputFluids);
        return recipeMatch.flatMap(match -> createPlan(basin, match.recipe()));
    }

    Optional<BasinInputs> planInputs(BasinBlockEntity basin, @Nullable GasInjectionRecipe recipe) {
        Level level = chamber.getLevel();
        if (level == null || basin.inputTank == null || recipe == null || recipe.hasItemInput() == recipe.hasFluidInput() || recipe.hasItemOutput() == recipe.hasFluidOutput()) {
            return Optional.empty();
        }

        GasStack availableGas = chamber.getGasInTank();
        if (availableGas.isEmpty() || GasInjectionChamberBasinIntegration.getTransactionView(basin) == null) {
            return Optional.empty();
        }

        IItemHandler inputItems = basin.getInputInventory();
        IFluidHandler inputFluids = basin.inputTank.getCapability();
        int batchSize = getMaxBatchSize(basin, inputItems, inputFluids, recipe);
        if (batchSize <= 0) {
            return Optional.empty();
        }

        Optional<GasConsumptionPlan> gasPlan = GasConsumptionPlanner.plan(recipe.getGasRequirement(), chamber.getGasTank(), batchSize);
        List<ItemDrain> itemDrainPlan = recipe.hasItemInput() ? createItemDrainPlan(recipe.getIngredient(), inputItems, batchSize) : List.of();
        List<FluidStack> fluidDrainPlan = recipe.hasFluidInput() ? createFluidDrainPlan(recipe.getFluidIngredient(), inputFluids, batchSize) : List.of();
        if (gasPlan.isEmpty() || itemDrainPlan == null || fluidDrainPlan == null || !canDrainItems(inputItems, itemDrainPlan) || !canDrainFluids(inputFluids, fluidDrainPlan)) {
            return Optional.empty();
        }

        return Optional.of(new BasinInputs(level, recipe, batchSize, gasPlan.get(), itemDrainPlan, fluidDrainPlan));
    }

    private int getMaxBatchSize(BasinBlockEntity basin, IItemHandler items, IFluidHandler fluids, GasInjectionRecipe recipe) {
        if (!matchesBasinFilter(basin, recipe)) {
            return 0;
        }

        long maxByInput;
        long maxByIntegerRange = Integer.MAX_VALUE;
        if (recipe.hasItemInput()) {
            maxByInput = getMatchingItemCount(recipe.getIngredient(), items);
        }
        else {
            SizedFluidIngredient fluidIngredient = recipe.getFluidIngredient();
            int fluidPerBatch = fluidIngredient.amount();
            if (fluidPerBatch <= 0) {
                return 0;
            }

            maxByInput = getMatchingFluidAmount(fluidIngredient, fluids) / fluidPerBatch;
            maxByIntegerRange = Math.min(maxByIntegerRange, Integer.MAX_VALUE / (long) fluidPerBatch);
        }

        if (recipe.hasFluidOutput()) {
            FluidStack resultPerBatch = recipe.getFluidResult();
            if (resultPerBatch.isEmpty() || resultPerBatch.getAmount() <= 0) {
                return 0;
            }

            maxByIntegerRange = Math.min(maxByIntegerRange, Integer.MAX_VALUE / (long) resultPerBatch.getAmount());
        }

        long theoreticalMaximum = Math.min(maxByInput, maxByIntegerRange);
        if (theoreticalMaximum <= 0) {
            return 0;
        }

        int minimumBatchSize = 0;
        int maximumBatchSize = (int) theoreticalMaximum;
        while (minimumBatchSize < maximumBatchSize) {
            int candidateBatchSize = minimumBatchSize + (maximumBatchSize - minimumBatchSize + 1) / 2;
            if (!canProcessBatch(basin, items, fluids, recipe, candidateBatchSize)) {
                maximumBatchSize = candidateBatchSize - 1;
                continue;
            }

            minimumBatchSize = candidateBatchSize;
        }
        return GasConsumptionPlanner.findMaximumMultiplier(recipe.getGasRequirement(), chamber.getGasTank(), minimumBatchSize);
    }

    @Internal
    public record ItemDrain(int slot, ItemStack expectedStack, int count) {}

    @Internal
    public record BasinPlan(GasInjectionRecipe recipe, GasConsumptionPlan gasPlan, List<ItemDrain> itemInputs, List<FluidStack> fluidInputs, List<ItemStack> itemResults, List<FluidStack> fluidResults) {
        boolean hasRequiredGas() {
            return gasPlan.canExecute();
        }
    }

    record BasinInputs(Level level, GasInjectionRecipe recipe, int batchSize, GasConsumptionPlan gasPlan, List<ItemDrain> itemInputs, List<FluidStack> fluidInputs) {}
}
