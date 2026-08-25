package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinIntegration.TransactionView;
import net.ty.createcraftedbeginning.core.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe.RecipeMatch;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.BASIN_RECIPE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberBasinProcessor {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;

    GasInjectionChamberBasinProcessor(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation) {
        this.chamber = chamber;
        this.operation = operation;
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

    private static boolean canDrainFluids(IFluidHandler fluids, List<FluidStack> drainPlan) {
        for (FluidStack drainRequest : drainPlan) {
            FluidStack simulatedDrain = fluids.drain(drainRequest, FluidAction.SIMULATE);
            if (simulatedDrain.getAmount() == drainRequest.getAmount() && FluidStack.isSameFluidSameComponents(simulatedDrain, drainRequest)) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean consumeBasinFluids(IFluidHandler fluids, List<FluidStack> drainPlan) {
        for (FluidStack drainRequest : drainPlan) {
            FluidStack executedDrain = fluids.drain(drainRequest, FluidAction.EXECUTE);
            if (executedDrain.getAmount() == drainRequest.getAmount() && FluidStack.isSameFluidSameComponents(executedDrain, drainRequest)) {
                continue;
            }

            return false;
        }
        return true;
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

    private static FluidStack getBatchResult(GasInjectionRecipe recipe, int batchSize) {
        FluidStack resultPerBatch = recipe.getFluidResult();
        if (resultPerBatch.isEmpty() || batchSize <= 0) {
            return FluidStack.EMPTY;
        }

        long resultAmount = (long) resultPerBatch.getAmount() * batchSize;
        if (resultAmount <= 0 || resultAmount > Integer.MAX_VALUE) {
            return FluidStack.EMPTY;
        }
        return resultPerBatch.copyWithAmount((int) resultAmount);
    }

    private static boolean canProcessBatch(BasinBlockEntity basin, IFluidHandler fluids, GasInjectionRecipe recipe, int batchSize) {
        List<FluidStack> drainPlan = createFluidDrainPlan(recipe.getFluidIngredient(), fluids, batchSize);
        FluidStack batchResult = getBatchResult(recipe, batchSize);
        return drainPlan != null && !batchResult.isEmpty() && canDrainFluids(fluids, drainPlan) && basin.acceptOutputs(List.of(), List.of(batchResult), true);
    }

    void tryStartOperation() {
        Optional<BasinBlockEntity> basinOptional = getBasin();
        if (basinOptional.isEmpty()) {
            return;
        }

        Optional<BasinPlan> planOptional = createPlan(basinOptional.get());
        if (planOptional.isEmpty() || !planOptional.get().hasRequiredGas()) {
            return;
        }

        operation.startProcessing(BASIN_RECIPE);
        chamber.setChanged();
        chamber.notifyUpdate();
    }

    boolean executeCurrentState() {
        if (chamber.getLevel() == null) {
            return false;
        }

        Optional<BasinBlockEntity> basinOptional = getBasin();
        if (basinOptional.isEmpty()) {
            return false;
        }

        BasinBlockEntity basin = basinOptional.get();
        Optional<BasinPlan> planOptional = createPlan(basin);
        if (planOptional.isEmpty() || !planOptional.get().hasRequiredGas()) {
            return false;
        }

        BasinPlan plan = planOptional.get();
        if (basin.inputTank == null) {
            return false;
        }

        IFluidHandler inputFluids = basin.inputTank.getCapability();
        TransactionView transactionView = GasInjectionChamberBasinIntegration.getTransactionView(basin);
        if (transactionView == null) {
            return false;
        }

        Provider registryProvider = chamber.getLevel().registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(GasInjectionChamberTransactions.gasParticipant(chamber, plan.gasRequest())).add(ResourceTransaction.participant(() -> canDrainFluids(inputFluids, plan.fluidInputs()) && basin.acceptOutputs(List.of(), List.of(plan.result()), true), () -> transactionView.snapshot(registryProvider), () -> consumeBasinFluids(inputFluids, plan.fluidInputs()) && basin.acceptOutputs(List.of(), List.of(plan.result()), false), snapshot -> transactionView.restore(registryProvider, snapshot)));
        if (!transaction.commit()) {
            return false;
        }

        basin.notifyChangeOfContents();
        basin.notifyUpdate();
        return true;
    }

    private Optional<BasinBlockEntity> getBasin() {
        if (chamber.getLevel() == null) {
            return Optional.empty();
        }

        if (chamber.getLevel().getBlockEntity(chamber.getBlockPos().below(2)) instanceof BasinBlockEntity basin) {
            return Optional.of(basin);
        }
        return Optional.empty();
    }

    private Optional<BasinPlan> createPlan(BasinBlockEntity basin) {
        if (chamber.getLevel() == null || basin.inputTank == null) {
            return Optional.empty();
        }

        GasStack availableGas = chamber.getGasInTank();
        if (availableGas.isEmpty()) {
            return Optional.empty();
        }

        IFluidHandler inputFluids = basin.inputTank.getCapability();
        Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findFluidRecipeMatch(chamber.getLevel(), inputFluids, availableGas);
        if (recipeMatch.isEmpty() || GasInjectionChamberBasinIntegration.getTransactionView(basin) == null) {
            return Optional.empty();
        }

        GasInjectionRecipe recipe = recipeMatch.get().recipe();
        int batchSize = getMaxBatchSize(basin, inputFluids, recipe);
        if (batchSize <= 0) {
            return Optional.empty();
        }

        long requiredGasAmount = recipe.getGasIngredient().amount() * batchSize;
        GasStack gasRequest = availableGas.copyWithAmount(requiredGasAmount);
        List<FluidStack> fluidDrainPlan = createFluidDrainPlan(recipe.getFluidIngredient(), inputFluids, batchSize);
        FluidStack batchResult = getBatchResult(recipe, batchSize);
        if (gasRequest.isEmpty() || fluidDrainPlan == null || batchResult.isEmpty() || !canDrainFluids(inputFluids, fluidDrainPlan) || !basin.acceptOutputs(List.of(), List.of(batchResult), true)) {
            return Optional.empty();
        }

        return Optional.of(new BasinPlan(availableGas.copy(), gasRequest, fluidDrainPlan, batchResult));
    }

    private int getMaxBatchSize(BasinBlockEntity basin, IFluidHandler fluids, GasInjectionRecipe recipe) {
        long gasPerBatch = recipe.getGasIngredient().amount();
        SizedFluidIngredient fluidIngredient = recipe.getFluidIngredient();
        FluidStack resultPerBatch = recipe.getFluidResult();
        int fluidPerBatch = fluidIngredient.amount();
        int outputPerBatch = resultPerBatch.getAmount();
        if (gasPerBatch <= 0 || fluidPerBatch <= 0 || resultPerBatch.isEmpty() || outputPerBatch <= 0 || basin.getFilter() == null || !basin.getFilter().test(resultPerBatch)) {
            return 0;
        }

        long maxByGas = chamber.getGasTank().getCapacity() / gasPerBatch;
        long maxByInput = getMatchingFluidAmount(fluidIngredient, fluids) / fluidPerBatch;
        long maxByFluidStack = Integer.MAX_VALUE / (long) Math.max(fluidPerBatch, outputPerBatch);
        long theoreticalMaximum = Math.min(maxByGas, Math.min(maxByInput, maxByFluidStack));
        if (theoreticalMaximum <= 0) {
            return 0;
        }

        int minimumBatchSize = 0;
        int maximumBatchSize = (int) theoreticalMaximum;
        while (minimumBatchSize < maximumBatchSize) {
            int candidateBatchSize = minimumBatchSize + (maximumBatchSize - minimumBatchSize + 1) / 2;
            if (!canProcessBatch(basin, fluids, recipe, candidateBatchSize)) {
                maximumBatchSize = candidateBatchSize - 1;
                continue;
            }

            minimumBatchSize = candidateBatchSize;
        }
        return minimumBatchSize;
    }

    private record BasinPlan(GasStack availableGas, GasStack gasRequest, List<FluidStack> fluidInputs, FluidStack result) {
        private boolean hasRequiredGas() {
            return availableGas.getAmount() >= gasRequest.getAmount();
        }
    }
}
