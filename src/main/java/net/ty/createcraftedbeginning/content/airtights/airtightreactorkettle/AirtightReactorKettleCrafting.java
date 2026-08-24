package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity.CraftPlan;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleCrafting {
    private final AirtightReactorKettleBlockEntity kettle;

    public AirtightReactorKettleCrafting(AirtightReactorKettleBlockEntity kettle) {
        this.kettle = kettle;
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

    private static boolean insertGasOutputs(IGasHandler outputHandler, List<GasStack> outputGases) {
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
            if (extractedStack.getCount() != consumptionAmount || expectedStack.isEmpty() || !ItemStack.isSameItemSameComponents(extractedStack, expectedStack)) {
                return false;
            }
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
            if (drainedFluid.getAmount() != consumptionAmount || !FluidStack.isSameFluidSameComponents(drainedFluid, expectedFluid)) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeGases(IGasHandler gases, List<GasStack> expectedGases, long[] amounts) {
        for (int tank = 0; tank < amounts.length; tank++) {
            long consumptionAmount = amounts[tank];
            if (consumptionAmount <= 0) {
                continue;
            }

            GasStack expectedGas = expectedGases.get(tank);
            if (expectedGas.isEmpty()) {
                return false;
            }

            GasStack drainedGas = gases.drain(expectedGas.copyWithAmount(consumptionAmount), GasAction.EXECUTE);
            if (drainedGas.getAmount() != consumptionAmount || !GasStack.isSameGasSameComponents(drainedGas, expectedGas)) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertItemOutputs(IItemHandler outputHandler, List<ItemStack> outputItems) {
        for (ItemStack outputItem : outputItems) {
            if (outputItem.isEmpty()) {
                continue;
            }

            if (!ItemHandlerHelper.insertItemStacked(outputHandler, outputItem.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public CraftPlan createCraftPlan(int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        IItemHandlerModifiable availableItems = kettle.getAvailableItems();
        IFluidHandler availableFluids = kettle.getAvailableFluids();
        IGasHandler availableGases = kettle.getAvailableGases();
        if (itemAmounts.length != availableItems.getSlots() || fluidAmounts.length != availableFluids.getTanks() || gasAmounts.length != availableGases.getTanks()) {
            throw new IllegalArgumentException("Craft plan resource counts do not match the reactor kettle");
        }
        for (int amount : itemAmounts) {
            if (amount < 0) {
                throw new IllegalArgumentException("Item consumption amounts must not be negative");
            }
        }
        for (int amount : fluidAmounts) {
            if (amount < 0) {
                throw new IllegalArgumentException("Fluid consumption amounts must not be negative");
            }
        }
        for (long amount : gasAmounts) {
            if (amount < 0) {
                throw new IllegalArgumentException("Gas consumption amounts must not be negative");
            }
        }

        return new CraftPlan(MachineResourceSnapshots.copyItems(availableItems), MachineResourceSnapshots.copyFluids(availableFluids), MachineResourceSnapshots.copyGases(availableGases), itemAmounts, fluidAmounts, gasAmounts, outputItems, outputFluids, outputGases);
    }

    public boolean commitCraft(CraftPlan plan) {
        if (kettle.getLevel() == null) {
            return false;
        }

        Provider registryProvider = kettle.getLevel().registryAccess();
        ResourceTransaction craftTransaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesItems(kettle.getAvailableItems(), plan.expectedItems()), this::snapshotItemInventories, () -> executeItemPlan(plan), this::restoreItemInventories)).add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesFluids(kettle.getAvailableFluids(), plan.expectedFluids()), () -> MachineResourceSnapshots.snapshotFluidTanks(registryProvider, kettle.getInputFluidTank(), kettle.getOutputFluidTank()), () -> executeFluidPlan(plan), snapshot -> MachineResourceSnapshots.restoreFluidTanks(registryProvider, snapshot, kettle.getInputFluidTank(), kettle.getOutputFluidTank()))).add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesGases(kettle.getAvailableGases(), plan.expectedGases()), () -> MachineResourceSnapshots.snapshotGasTanks(registryProvider, kettle.getInputGasTank(), kettle.getOutputGasTank()), () -> executeGasPlan(plan), snapshot -> MachineResourceSnapshots.restoreGasTanks(registryProvider, snapshot, kettle.getInputGasTank(), kettle.getOutputGasTank())));

        boolean craftSucceeded = craftTransaction.commit();
        if (craftSucceeded && plan.outputItems().stream().anyMatch(outputItem -> outputItem.is(AllItems.ANDESITE_ALLOY))) {
            kettle.awardBackToBasics();
        }
        return craftSucceeded;
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, boolean simulate) {
        IFluidHandler outputFluidHandler = kettle.getOutputFluidTank().getCapability();
        IGasHandler outputGasHandler = kettle.getOutputGasTank().getCapability();
        boolean hasItemOutputs = outputItems.stream().anyMatch(outputItem -> !outputItem.isEmpty());
        if (hasItemOutputs && !canAcceptItemOutputs(outputItems)) {
            return false;
        }

        boolean hasFluidOutputs = outputFluids.stream().anyMatch(outputFluid -> !outputFluid.isEmpty());
        if (hasFluidOutputs && (outputFluidHandler == null || !canAcceptFluidOutputs(outputFluidHandler, outputFluids))) {
            return false;
        }

        boolean hasGasOutputs = outputGases.stream().anyMatch(outputGas -> !outputGas.isEmpty());
        if (hasGasOutputs && !canAcceptGasOutputs(outputGasHandler, outputGases)) {
            return false;
        }

        if (simulate) {
            return true;
        }

        CraftPlan craftPlan = createCraftPlan(new int[kettle.getAvailableItems().getSlots()], new int[kettle.getAvailableFluids().getTanks()], new long[kettle.getAvailableGases().getTanks()], outputItems, outputFluids, outputGases);
        return kettle.commitCraft(craftPlan);
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

        kettle.getOutputInventory().allowInsertion();
        try {
            return insertItemOutputs(kettle.getOutputInventory(), plan.outputItems());
        } finally {
            kettle.getOutputInventory().forbidInsertion();
        }
    }

    private boolean executeFluidPlan(CraftPlan plan) {
        if (!consumeFluids(kettle.getAvailableFluids(), plan.expectedFluids(), plan.fluidAmounts())) {
            return false;
        }

        kettle.getOutputFluidTank().allowInsertion();
        try {
            return insertFluidOutputs(kettle.getOutputFluidTank().getCapability(), plan.outputFluids());
        } finally {
            kettle.getOutputFluidTank().forbidInsertion();
        }
    }

    private boolean executeGasPlan(CraftPlan plan) {
        if (!consumeGases(kettle.getAvailableGases(), plan.expectedGases(), plan.gasAmounts())) {
            return false;
        }

        kettle.getOutputGasTank().allowInsertion();
        try {
            return insertGasOutputs(kettle.getOutputGasTank().getCapability(), plan.outputGases());
        } finally {
            kettle.getOutputGasTank().forbidInsertion();
        }
    }

    private boolean canAcceptItemOutputs(List<ItemStack> outputItems) {
        IItemHandlerModifiable simulatedInventory = AirtightReactorKettleInventory.createSimulation(kettle.getOutputInventory().getSlots());
        for (int slot = 0; slot < kettle.getOutputInventory().getSlots(); slot++) {
            simulatedInventory.setStackInSlot(slot, kettle.getOutputInventory().getStackInSlot(slot).copy());
        }

        for (ItemStack outputItem : outputItems) {
            if (outputItem.isEmpty()) {
                continue;
            }

            if (!ItemHandlerHelper.insertItemStacked(simulatedInventory, outputItem.copy(), false).isEmpty()) {
                return false;
            }
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

    private boolean canAcceptGasOutputs(IGasHandler outputHandler, List<GasStack> outputGases) {
        SmartGasTankBehaviour simulatedTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, kettle, outputHandler.getTanks(), AirtightReactorKettleBlockEntity.getGasCapacity(), true);
        IGasHandler simulatedHandler = simulatedTank.getCapability();
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
