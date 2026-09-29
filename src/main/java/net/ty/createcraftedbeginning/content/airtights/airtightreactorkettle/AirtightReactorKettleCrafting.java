package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity.CraftPlan;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPreparation;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleCrafting {
    private final AirtightReactorKettleBlockEntity kettle;

    AirtightReactorKettleCrafting(AirtightReactorKettleBlockEntity kettle) {
        this.kettle = kettle;
    }

    @Internal
    public static boolean applyRecipe(AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe) {
        return ReactorKettleCraftPreparation.prepare(kettle, recipe).map(plan -> kettle.commitRecipeCraft(plan.itemAmounts(), plan.fluidAmounts(), plan.gasPlan(), plan.outputItems(), plan.outputFluids(), plan.outputGases())).orElse(false);
    }

    @Internal
    public static boolean applyCraftingRecipe(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        return AirtightReactorKettleMixingPlanner.planCraftingRecipe(kettle, recipe).map(kettle::commitCraft).orElse(false);
    }

    private static boolean insertFluidOutputs(IFluidHandler outputHandler, List<FluidStack> outputFluids) {
        for (FluidStack outputFluid : outputFluids) {
            if (outputFluid.isEmpty() || outputHandler.fill(outputFluid.copy(), FluidAction.EXECUTE) == outputFluid.getAmount()) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean insertGasOutputs(GasHandler outputHandler, List<GasStack> outputGases) {
        for (GasStack outputGas : outputGases) {
            if (outputGas.isEmpty() || outputHandler.fill(outputGas.copy(), GasAction.EXECUTE) == outputGas.getAmount()) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean consumeItems(IItemHandler items, List<ItemStack> expectedItems, int[] amounts) {
        for (int slot = 0; slot < amounts.length; slot++) {
            int consumptionAmount = amounts[slot];
            if (consumptionAmount <= 0) {
                continue;
            }

            ItemStack expectedStack = expectedItems.get(slot);
            ItemStack extractedStack = items.extractItem(slot, consumptionAmount, false);
            if (extractedStack.getCount() == consumptionAmount && !expectedStack.isEmpty() && ItemStack.isSameItemSameComponents(extractedStack, expectedStack)) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean consumeFluids(IFluidHandler fluids, List<FluidStack> expectedFluids, int[] amounts) {
        for (int tank = 0; tank < amounts.length; tank++) {
            int consumptionAmount = amounts[tank];
            if (consumptionAmount <= 0) {
                continue;
            }

            FluidStack expectedFluid = expectedFluids.get(tank);
            if (expectedFluid.isEmpty()) {
                return false;
            }

            FluidStack drainedFluid = fluids.drain(expectedFluid.copyWithAmount(consumptionAmount), FluidAction.EXECUTE);
            if (drainedFluid.getAmount() == consumptionAmount && FluidStack.isSameFluidSameComponents(drainedFluid, expectedFluid)) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean insertItemOutputs(IItemHandler outputHandler, List<ItemStack> outputItems) {
        for (ItemStack outputItem : outputItems) {
            if (outputItem.isEmpty() || ItemHandlerHelper.insertItemStacked(outputHandler, outputItem.copy(), false).isEmpty()) {
                continue;
            }

            return false;
        }

        return true;
    }

    CraftPlan createCraftPlan(int[] itemAmounts, int[] fluidAmounts, GasConsumptionPlan gasPlan, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        IItemHandlerModifiable availableItems = kettle.getAvailableItems();
        IFluidHandler availableFluids = kettle.getAvailableFluids();
        GasHandler availableGases = kettle.getAvailableGases();
        if (itemAmounts.length != availableItems.getSlots() || fluidAmounts.length != availableFluids.getTanks() || gasPlan.tankAmounts().length != availableGases.getTanks()) {
            throw new IllegalArgumentException("Reactor kettle craft plan resource count mismatch: expected " + availableItems.getSlots() + " item slots, " + availableFluids.getTanks() + " fluid tanks and " + availableGases.getTanks() + " gas tanks, got " + itemAmounts.length + ", " + fluidAmounts.length + " and " + gasPlan.tankAmounts().length + ", respectively.");
        }

        for (int amount : itemAmounts) {
            if (amount >= 0) {
                continue;
            }

            throw new IllegalArgumentException("Item consumption amount must be non-negative; got " + amount + '.');
        }
        for (int amount : fluidAmounts) {
            if (amount >= 0) {
                continue;
            }

            throw new IllegalArgumentException("Fluid consumption amount must be non-negative; got " + amount + " mB.");
        }
        return new CraftPlan(MachineResourceSnapshots.copyItems(availableItems), MachineResourceSnapshots.copyFluids(availableFluids), MachineResourceSnapshots.copyGases(availableGases), itemAmounts, fluidAmounts, gasPlan, outputItems, outputFluids, outputGases);
    }

    boolean commitCraft(CraftPlan plan) {
        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        Provider registryProvider = level.registryAccess();
        SmartFluidTankBehaviour inputFluid = kettle.getInputFluidTank();
        SmartFluidTankBehaviour outputFluid = kettle.getOutputFluidTank();
        SmartGasTankBehaviour inputGas = kettle.getInputGasTank();
        SmartGasTankBehaviour outputGas = kettle.getOutputGasTank();
        ResourceTransaction craftTransaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesItems(kettle.getAvailableItems(), plan.expectedItems()), this::snapshotItemInventories, () -> executeItemPlan(plan), this::restoreItemInventories)).add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesFluids(kettle.getAvailableFluids(), plan.expectedFluids()), () -> MachineResourceSnapshots.snapshotFluidTanks(registryProvider, inputFluid, outputFluid), () -> executeFluidPlan(plan), snapshot -> MachineResourceSnapshots.restoreFluidTanks(registryProvider, snapshot, inputFluid, outputFluid))).add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesGases(kettle.getAvailableGases(), plan.expectedGases()) && plan.gasPlan().canExecute(), () -> MachineResourceSnapshots.snapshotGasTanks(inputGas, outputGas), () -> executeGasPlan(plan), snapshot -> MachineResourceSnapshots.restoreGasTanks(snapshot, inputGas, outputGas)));

        boolean craftSucceeded = craftTransaction.commit();
        if (craftSucceeded && plan.outputItems().stream().anyMatch(outputItem -> outputItem.is(AllItems.ANDESITE_ALLOY))) {
            kettle.awardBackToBasics();
        }
        return craftSucceeded;
    }

    boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        IFluidHandler outputFluidHandler = kettle.getOutputFluidTank().getCapability();
        GasHandler outputGasHandler = kettle.getOutputGasTank().getCapability();
        boolean hasItemOutputs = outputItems.stream().anyMatch(outputItem -> !outputItem.isEmpty());
        if (hasItemOutputs && !canAcceptItemOutputs(outputItems)) {
            return false;
        }

        boolean hasFluidOutputs = outputFluids.stream().anyMatch(outputFluid -> !outputFluid.isEmpty());
        if (hasFluidOutputs && (outputFluidHandler == null || !canAcceptFluidOutputs(outputFluidHandler, outputFluids))) {
            return false;
        }

        boolean hasGasOutputs = outputGases.stream().anyMatch(outputGas -> !outputGas.isEmpty());
        return !hasGasOutputs || canAcceptGasOutputs(outputGasHandler, outputGases);
    }

    private ItemTransactionSnapshot snapshotItemInventories() {
        return new ItemTransactionSnapshot(MachineResourceSnapshots.copyItems(kettle.getInputInventory()), MachineResourceSnapshots.copyItems(kettle.getOutputInventory()));
    }

    private void restoreItemInventories(ItemTransactionSnapshot snapshot) {
        MachineResourceSnapshots.restoreItems(kettle.getInputInventory(), snapshot.inputItems());
        MachineResourceSnapshots.restoreItems(kettle.getOutputInventory(), snapshot.outputItems());
    }

    private boolean executeItemPlan(CraftPlan plan) {
        if (!consumeItems(kettle.getAvailableItems(), plan.expectedItems(), plan.itemAmounts())) {
            return false;
        }

        SmartInventory output = kettle.getOutputInventory();
        output.allowInsertion();
        try {
            return insertItemOutputs(output, plan.outputItems());
        }
        finally {
            output.forbidInsertion();
        }
    }

    private boolean executeFluidPlan(CraftPlan plan) {
        if (!consumeFluids(kettle.getAvailableFluids(), plan.expectedFluids(), plan.fluidAmounts())) {
            return false;
        }

        SmartFluidTankBehaviour output = kettle.getOutputFluidTank();
        output.allowInsertion();
        try {
            return insertFluidOutputs(output.getCapability(), plan.outputFluids());
        }
        finally {
            output.forbidInsertion();
        }
    }

    private boolean executeGasPlan(CraftPlan plan) {
        if (!plan.gasPlan().execute()) {
            return false;
        }

        SmartGasTankBehaviour output = kettle.getOutputGasTank();
        output.allowInsertion();
        try {
            return insertGasOutputs(output.getCapability(), plan.outputGases());
        }
        finally {
            output.forbidInsertion();
        }
    }

    private boolean canAcceptItemOutputs(List<ItemStack> outputItems) {
        IItemHandler output = kettle.getOutputInventory();
        IItemHandlerModifiable simulatedInventory = AirtightReactorKettleInventory.createSimulation(output.getSlots());
        for (int slot = 0; slot < output.getSlots(); slot++) {
            simulatedInventory.setStackInSlot(slot, output.getStackInSlot(slot).copy());
        }

        for (ItemStack outputItem : outputItems) {
            if (outputItem.isEmpty() || ItemHandlerHelper.insertItemStacked(simulatedInventory, outputItem.copy(), false).isEmpty()) {
                continue;
            }

            return false;
        }

        return true;
    }

    private boolean canAcceptFluidOutputs(IFluidHandler outputHandler, List<FluidStack> outputFluids) {
        SmartFluidTankBehaviour simulatedTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, kettle, outputHandler.getTanks(), AirtightReactorKettleBlockEntity.getFluidCapacity(), true);
        IFluidHandler simulatedHandler = simulatedTank.getCapability();
        for (int tank = 0; tank < outputHandler.getTanks(); tank++) {
            FluidStack storedFluid = outputHandler.getFluidInTank(tank).copy();
            if (storedFluid.isEmpty() || simulatedHandler.fill(storedFluid.copy(), FluidAction.EXECUTE) == storedFluid.getAmount()) {
                continue;
            }

            return false;
        }

        for (FluidStack outputFluid : outputFluids) {
            if (outputFluid.isEmpty() || simulatedHandler.fill(outputFluid.copy(), FluidAction.EXECUTE) == outputFluid.getAmount()) {
                continue;
            }

            return false;
        }

        return true;
    }

    private boolean canAcceptGasOutputs(GasHandler outputHandler, List<GasStack> outputGases) {
        SmartGasTankBehaviour simulatedTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, kettle, outputHandler.getTanks(), AirtightReactorKettleBlockEntity.getGasCapacity(), GasPressureLimits.HARD_PRESSURE_PA, true);
        GasHandler simulatedHandler = simulatedTank.getCapability();
        for (int tank = 0; tank < outputHandler.getTanks(); tank++) {
            GasStack storedGas = outputHandler.getGasInTank(tank).copy();
            if (storedGas.isEmpty() || simulatedHandler.fill(storedGas.copy(), GasAction.EXECUTE) == storedGas.getAmount()) {
                continue;
            }

            return false;
        }

        for (GasStack outputGas : outputGases) {
            if (outputGas.isEmpty() || simulatedHandler.fill(outputGas.copy(), GasAction.EXECUTE) == outputGas.getAmount()) {
                continue;
            }

            return false;
        }

        return true;
    }

    private record ItemTransactionSnapshot(List<ItemStack> inputItems, List<ItemStack> outputItems) {}
}
