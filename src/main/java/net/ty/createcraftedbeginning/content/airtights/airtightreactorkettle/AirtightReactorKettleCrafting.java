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
import net.ty.createcraftedbeginning.content.airtights.transaction.MachineResourceSnapshots;
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

    private static boolean insertFluidOutputs(IFluidHandler target, List<FluidStack> outputs) {
        for (FluidStack stack : outputs) {
            if (stack.isEmpty() || target.fill(stack.copy(), FluidAction.EXECUTE) == stack.getAmount()) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean insertGasOutputs(IGasHandler target, List<GasStack> outputs) {
        for (GasStack stack : outputs) {
            if (stack.isEmpty() || target.fill(stack.copy(), GasAction.EXECUTE) == stack.getAmount()) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean consumeItems(IItemHandler items, List<ItemStack> expectedItems, int[] amounts) {
        for (int slot = 0; slot < amounts.length; slot++) {
            int amount = amounts[slot];
            if (amount <= 0) {
                continue;
            }

            ItemStack expected = expectedItems.get(slot);
            ItemStack extracted = items.extractItem(slot, amount, false);
            if (extracted.getCount() != amount || expected.isEmpty() || !ItemStack.isSameItemSameComponents(extracted, expected)) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeFluids(IFluidHandler fluids, List<FluidStack> expectedFluids, int[] amounts) {
        for (int tank = 0; tank < amounts.length; tank++) {
            int amount = amounts[tank];
            if (amount <= 0) {
                continue;
            }

            FluidStack expected = expectedFluids.get(tank);
            if (expected.isEmpty()) {
                return false;
            }

            FluidStack drained = fluids.drain(expected.copyWithAmount(amount), FluidAction.EXECUTE);
            if (drained.getAmount() != amount || !FluidStack.isSameFluidSameComponents(drained, expected)) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeGases(IGasHandler gases, List<GasStack> expectedGases, long[] amounts) {
        for (int tank = 0; tank < amounts.length; tank++) {
            long amount = amounts[tank];
            if (amount <= 0) {
                continue;
            }

            GasStack expected = expectedGases.get(tank);
            if (expected.isEmpty()) {
                return false;
            }

            GasStack drained = gases.drain(expected.copyWithAmount(amount), GasAction.EXECUTE);
            if (drained.getAmount() != amount || !GasStack.isSameGasSameComponents(drained, expected)) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertItemOutputs(IItemHandler target, List<ItemStack> outputs) {
        for (ItemStack stack : outputs) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public CraftPlan createCraftPlan(int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        IItemHandlerModifiable itemCapability = kettle.getItemCapability();
        IFluidHandler fluidCapability = kettle.getFluidCapability();
        IGasHandler gasCapability = kettle.getGasCapability();
        if (itemAmounts.length != itemCapability.getSlots() || fluidAmounts.length != fluidCapability.getTanks() || gasAmounts.length != gasCapability.getTanks()) {
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

        return new CraftPlan(MachineResourceSnapshots.copyItems(itemCapability), MachineResourceSnapshots.copyFluids(fluidCapability), MachineResourceSnapshots.copyGases(gasCapability), itemAmounts, fluidAmounts, gasAmounts, outputItems, outputFluids, outputGases);
    }

    public boolean commitCraft(CraftPlan plan) {
        if (kettle.getLevel() == null) {
            return false;
        }

        Provider provider = kettle.getLevel().registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesItems(kettle.getItemCapability(), plan.expectedItems()), this::snapshotItemInventories, () -> executeItemPlan(plan), this::restoreItemInventories)).add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesFluids(kettle.getFluidCapability(), plan.expectedFluids()), () -> MachineResourceSnapshots.snapshotFluidTanks(provider, kettle.getInputFluidTank(), kettle.getOutputFluidTank()), () -> executeFluidPlan(plan), snapshot -> MachineResourceSnapshots.restoreFluidTanks(provider, snapshot, kettle.getInputFluidTank(), kettle.getOutputFluidTank()))).add(ResourceTransaction.participant(() -> MachineResourceSnapshots.matchesGases(kettle.getGasCapability(), plan.expectedGases()), () -> MachineResourceSnapshots.snapshotGasTanks(provider, kettle.getInputGasTank(), kettle.getOutputGasTank()), () -> executeGasPlan(plan), snapshot -> MachineResourceSnapshots.restoreGasTanks(provider, snapshot, kettle.getInputGasTank(), kettle.getOutputGasTank())));

        boolean committed = transaction.commit();
        if (committed && plan.outputItems().stream().anyMatch(stack -> stack.is(AllItems.ANDESITE_ALLOY))) {
            kettle.awardBackToBasics();
        }
        return committed;
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, boolean simulate) {
        IFluidHandler targetFluidTank = kettle.getOutputFluidTank().getCapability();
        IGasHandler targetGasTank = kettle.getOutputGasTank().getCapability();
        boolean hasItemOutputs = outputItems.stream().anyMatch(stack -> !stack.isEmpty());
        if (hasItemOutputs && !canAcceptItemOutputs(outputItems)) {
            return false;
        }

        boolean hasFluidOutputs = outputFluids.stream().anyMatch(stack -> !stack.isEmpty());
        if (hasFluidOutputs && (targetFluidTank == null || !canAcceptFluidOutputs(targetFluidTank, outputFluids))) {
            return false;
        }

        boolean hasGasOutputs = outputGases.stream().anyMatch(stack -> !stack.isEmpty());
        if (hasGasOutputs && !canAcceptGasOutputs(targetGasTank, outputGases)) {
            return false;
        }

        if (simulate) {
            return true;
        }

        CraftPlan craftPlan = createCraftPlan(new int[kettle.getItemCapability().getSlots()], new int[kettle.getFluidCapability().getTanks()], new long[kettle.getGasCapability().getTanks()], outputItems, outputFluids, outputGases);
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
        if (!consumeItems(kettle.getItemCapability(), plan.expectedItems(), plan.itemAmounts())) {
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
        if (!consumeFluids(kettle.getFluidCapability(), plan.expectedFluids(), plan.fluidAmounts())) {
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
        if (!consumeGases(kettle.getGasCapability(), plan.expectedGases(), plan.gasAmounts())) {
            return false;
        }

        kettle.getOutputGasTank().allowInsertion();
        try {
            return insertGasOutputs(kettle.getOutputGasTank().getCapability(), plan.outputGases());
        } finally {
            kettle.getOutputGasTank().forbidInsertion();
        }
    }

    private boolean canAcceptItemOutputs(List<ItemStack> outputs) {
        IItemHandlerModifiable simulation = AirtightReactorKettleInventory.createSimulation(kettle.getOutputInventory().getSlots());
        for (int slot = 0; slot < kettle.getOutputInventory().getSlots(); slot++) {
            simulation.setStackInSlot(slot, kettle.getOutputInventory().getStackInSlot(slot).copy());
        }

        for (ItemStack stack : outputs) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(simulation, stack.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean canAcceptFluidOutputs(IFluidHandler target, List<FluidStack> outputs) {
        SmartFluidTankBehaviour simulatedTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, kettle, target.getTanks(), AirtightReactorKettleBlockEntity.getFluidCapacity(), true);
        IFluidHandler simulation = simulatedTank.getCapability();
        for (int tank = 0; tank < target.getTanks(); tank++) {
            FluidStack existing = target.getFluidInTank(tank).copy();
            if (existing.isEmpty() || simulation.fill(existing.copy(), FluidAction.EXECUTE) == existing.getAmount()) {
                continue;
            }

            return false;
        }

        for (FluidStack stack : outputs) {
            if (stack.isEmpty() || simulation.fill(stack.copy(), FluidAction.EXECUTE) == stack.getAmount()) {
                continue;
            }

            return false;
        }
        return true;
    }

    private boolean canAcceptGasOutputs(IGasHandler target, List<GasStack> outputs) {
        SmartGasTankBehaviour simulatedTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, kettle, target.getTanks(), AirtightReactorKettleBlockEntity.getGasCapacity(), true);
        IGasHandler simulation = simulatedTank.getCapability();
        for (int tank = 0; tank < target.getTanks(); tank++) {
            GasStack existing = target.getGasInTank(tank).copy();
            if (existing.isEmpty() || simulation.fill(existing.copy(), GasAction.EXECUTE) == existing.getAmount()) {
                continue;
            }

            return false;
        }

        for (GasStack stack : outputs) {
            if (stack.isEmpty() || simulation.fill(stack.copy(), GasAction.EXECUTE) == stack.getAmount()) {
                continue;
            }

            return false;
        }
        return true;
    }

    private record ItemTransactionSnapshot(List<ItemStack> inputItems, List<ItemStack> outputItems) {}
}
