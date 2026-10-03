package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinIntegration.TransactionView;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinPlanner.BasinPlan;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinPlanner.ItemDrain;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasRecipePressureSpeed;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.BASIN_RECIPE;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberBasinProcessor {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;
    private final GasInjectionChamberBasinPlanner planner;

    @Internal
    public GasInjectionChamberBasinProcessor(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation) {
        this.chamber = chamber;
        this.operation = operation;
        planner = new GasInjectionChamberBasinPlanner(chamber);
    }

    private static boolean consumeBasinItems(IItemHandler items, List<ItemDrain> drainPlan) {
        for (ItemDrain itemDrain : drainPlan) {
            ItemStack executedDrain = items.extractItem(itemDrain.slot(), itemDrain.count(), false);
            if (executedDrain.getCount() == itemDrain.count() && ItemStack.isSameItemSameComponents(executedDrain, itemDrain.expectedStack())) {
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

    @Internal
    public boolean executeCurrentState() {
        Level level = chamber.getLevel();
        if (level == null) {
            return false;
        }

        Optional<BasinBlockEntity> basinOptional = getBasin();
        if (basinOptional.isEmpty()) {
            return false;
        }

        BasinBlockEntity basin = basinOptional.get();
        Optional<BasinPlan> planOptional = planner.planInputs(basin, operation.getRecipe()).flatMap(inputs -> new GasInjectionChamberBasinOutputs().roll(basin, inputs));
        if (planOptional.isEmpty() || basin.inputTank == null) {
            return false;
        }

        BasinPlan plan = planOptional.get();
        if (!plan.hasRequiredGas()) {
            return false;
        }

        IItemHandler inputItems = basin.getInputInventory();
        IFluidHandler inputFluids = basin.inputTank.getCapability();
        TransactionView transactionView = GasInjectionChamberBasinIntegration.getTransactionView(basin);
        if (transactionView == null) {
            return false;
        }

        Provider registryProvider = level.registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(GasInjectionChamberTransactions.gasParticipant(chamber, plan.gasPlan())).add(ResourceTransaction.participant(() -> GasInjectionChamberBasinPlanner.canDrainItems(inputItems, plan.itemInputs()) && GasInjectionChamberBasinPlanner.canDrainFluids(inputFluids, plan.fluidInputs()) && basin.acceptOutputs(plan.itemResults(), plan.fluidResults(), true), () -> transactionView.snapshot(registryProvider), () -> consumeBasinItems(inputItems, plan.itemInputs()) && consumeBasinFluids(inputFluids, plan.fluidInputs()) && basin.acceptOutputs(plan.itemResults(), plan.fluidResults(), false), snapshot -> transactionView.restore(registryProvider, snapshot)));
        if (!transaction.commit()) {
            return false;
        }

        basin.notifyChangeOfContents();
        basin.notifyUpdate();
        return true;
    }

    void tryStartOperation() {
        Optional<BasinBlockEntity> basinOptional = getBasin();
        if (basinOptional.isEmpty()) {
            return;
        }

        Optional<BasinPlan> planOptional = planner.createPlan(basinOptional.get());
        if (planOptional.isEmpty()) {
            return;
        }

        BasinPlan plan = planOptional.get();
        if (!plan.hasRequiredGas()) {
            return;
        }

        operation.startProcessing(BASIN_RECIPE, GasRecipePressureSpeed.multiplier(plan.gasPlan()), plan.recipe());
        chamber.setChanged();
        chamber.notifyUpdate();
    }

    private Optional<BasinBlockEntity> getBasin() {
        Level level = chamber.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        if (level.getBlockEntity(chamber.getBlockPos().below(2)) instanceof BasinBlockEntity basin) {
            return Optional.of(basin);
        }

        return Optional.empty();
    }
}
