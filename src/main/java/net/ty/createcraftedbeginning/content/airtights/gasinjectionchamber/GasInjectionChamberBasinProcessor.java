package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gas.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.content.airtights.gas.transaction.MachineResourceSnapshots.FluidTankSnapshot;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe.RecipeMatch;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberBasinProcessor {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;

    public GasInjectionChamberBasinProcessor(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation) {
        this.chamber = chamber;
        this.operation = operation;
    }

    private static @Nullable List<FluidStack> createFluidDrainPlan(SizedFluidIngredient ingredient, IFluidHandler fluids) {
        int remaining = ingredient.amount();
        if (remaining <= 0) {
            return null;
        }

        List<FluidStack> plan = new ArrayList<>();
        for (int tank = 0; tank < fluids.getTanks() && remaining > 0; tank++) {
            FluidStack stack = fluids.getFluidInTank(tank);
            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            int amount = Math.min(remaining, stack.getAmount());
            if (amount <= 0) {
                continue;
            }

            FluidStack request = stack.copyWithAmount(amount);
            boolean merged = false;
            for (FluidStack planned : plan) {
                if (!FluidStack.isSameFluidSameComponents(planned, request)) {
                    continue;
                }

                planned.setAmount(planned.getAmount() + amount);
                merged = true;
                break;
            }
            if (!merged) {
                plan.add(request);
            }
            remaining -= amount;
        }
        return remaining == 0 ? plan : null;
    }

    private static boolean canDrainFluids(IFluidHandler fluids, List<FluidStack> plan) {
        for (FluidStack request : plan) {
            FluidStack drained = fluids.drain(request, FluidAction.SIMULATE);
            if (drained.getAmount() != request.getAmount() || !FluidStack.isSameFluidSameComponents(drained, request)) {
                return false;
            }
        }
        return true;
    }

    private static BasinFluidSnapshot snapshotBasinFluids(BasinBlockEntity basin, SmartFluidTankBehaviour outputTank, BasinTransactionAccess transactionAccess, Provider provider) {
        List<FluidStack> outputBuffer = transactionAccess.ccb$getTransactionFluidOverflow().stream().map(FluidStack::copy).toList();
        return new BasinFluidSnapshot(MachineResourceSnapshots.snapshotFluidTanks(provider, basin.inputTank, outputTank), outputBuffer);
    }

    private static void restoreBasinFluids(BasinBlockEntity basin, SmartFluidTankBehaviour outputTank, BasinTransactionAccess transactionAccess, Provider provider, BasinFluidSnapshot snapshot) {
        MachineResourceSnapshots.restoreFluidTanks(provider, snapshot.tanks(), basin.inputTank, outputTank);
        List<FluidStack> outputBuffer = transactionAccess.ccb$getTransactionFluidOverflow();
        outputBuffer.clear();
        snapshot.outputBuffer().stream().map(FluidStack::copy).forEach(outputBuffer::add);
    }

    public void tryStartOperation() {
        Optional<BasinBlockEntity> basin = getBasin();
        if (basin.isEmpty() || !prepareOperation(basin.get())) {
            return;
        }

        operation.setProcessingTicks(GasInjectionChamberBlockEntity.PROCESSING_TIME + GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME);
        chamber.notifyUpdate();
    }

    public boolean executeRecipeOperation() {
        if (chamber.getLevel() == null) {
            return false;
        }

        Optional<BasinBlockEntity> basinOptional = getBasin();
        if (basinOptional.isEmpty()) {
            return false;
        }

        BasinBlockEntity basin = basinOptional.get();
        if (basin.inputTank == null) {
            return false;
        }

        IFluidHandler fluids = basin.inputTank.getCapability();
        FluidStack result = operation.fluidResult.copy();
        if (result.isEmpty() || basin.getFilter() == null || !basin.getFilter().test(result)) {
            return false;
        }

        BasinTransactionAccess transactionAccess = (BasinTransactionAccess) basin;
        SmartFluidTankBehaviour outputTank = transactionAccess.ccb$getTransactionOutputTank();
        Provider provider = chamber.getLevel().registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(GasInjectionChamberTransactions.operationGasParticipant(chamber, operation, provider)).add(ResourceTransaction.participant(() -> canDrainFluids(fluids, operation.fluidInputs) && basin.acceptOutputs(List.of(), List.of(result), true), () -> snapshotBasinFluids(basin, outputTank, transactionAccess, provider), () -> consumeBasinFluids(fluids) && basin.acceptOutputs(List.of(), List.of(result), false), snapshot -> restoreBasinFluids(basin, outputTank, transactionAccess, provider, snapshot)));
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

    private boolean prepareOperation(BasinBlockEntity basin) {
        if (chamber.getLevel() == null || basin.inputTank == null) {
            return false;
        }

        GasStack tankGas = chamber.getGasInTank();
        if (tankGas.isEmpty()) {
            return false;
        }

        IFluidHandler fluids = basin.inputTank.getCapability();
        Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findFluidRecipeMatch(chamber.getLevel(), fluids, tankGas);
        if (recipeMatch.isEmpty()) {
            return false;
        }

        GasInjectionRecipe recipe = recipeMatch.get().recipe();
        long requiredGas = recipe.getGasIngredient().amount();
        if (requiredGas <= 0 || tankGas.getAmount() < requiredGas) {
            return false;
        }

        List<FluidStack> fluidDrainPlan = createFluidDrainPlan(recipe.getFluidIngredient(), fluids);
        if (fluidDrainPlan == null || !canDrainFluids(fluids, fluidDrainPlan)) {
            return false;
        }

        FluidStack result = recipe.getFluidResult().copy();
        if (result.isEmpty() || basin.getFilter() == null || !basin.getFilter().test(result) || !basin.acceptOutputs(List.of(), List.of(result), true)) {
            return false;
        }

        operation.setBasinOperation(tankGas, requiredGas, fluidDrainPlan, result);
        chamber.setChanged();
        return true;
    }

    private boolean consumeBasinFluids(IFluidHandler fluids) {
        for (FluidStack request : operation.fluidInputs) {
            FluidStack drained = fluids.drain(request, FluidAction.EXECUTE);
            if (drained.getAmount() != request.getAmount() || !FluidStack.isSameFluidSameComponents(drained, request)) {
                return false;
            }
        }
        return true;
    }

    private record BasinFluidSnapshot(FluidTankSnapshot tanks, List<FluidStack> outputBuffer) {}
}
