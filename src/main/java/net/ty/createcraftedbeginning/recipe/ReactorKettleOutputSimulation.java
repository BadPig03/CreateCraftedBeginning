package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.recipe.interfaces.ReactorKettleRecipeContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ReactorKettleOutputSimulation {
    private final ReactorKettleRecipeContext kettle;

    ReactorKettleOutputSimulation(ReactorKettleRecipeContext kettle) {
        this.kettle = kettle;
    }

    boolean accepts(IItemHandler availableItems, IFluidHandler availableFluids, GasHandler availableGases, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts) {
        IItemHandler outputInventory = kettle.getOutputItemCapability();
        IFluidHandler outputFluidTank = kettle.getOutputFluidCapability();
        GasHandler outputGasTank = kettle.getOutputGasCapability();
        return canAcceptItemOutputsAfterInputsAreConsumed(availableItems, outputInventory, outputItems, itemAmounts) && canAcceptFluidOutputsAfterInputsAreConsumed(availableFluids, outputFluidTank, outputFluids, fluidAmounts) && canAcceptGasOutputsAfterInputsAreConsumed(availableGases, outputGasTank, outputGases, gasAmounts);
    }

    private static boolean canAcceptFluidOutputsAfterInputsAreConsumed(IFluidHandler availableFluids, IFluidHandler outputTank, List<FluidStack> outputFluids, int[] extractedFluidsFromTank) {
        if (outputFluids.isEmpty()) {
            return true;
        }

        FluidStack[] simulatedFluidTanks = new FluidStack[outputTank.getTanks()];
        int outputOffset = availableFluids.getTanks() - outputTank.getTanks();
        for (int tankIndex = 0; tankIndex < outputTank.getTanks(); tankIndex++) {
            FluidStack simulatedFluid = outputTank.getFluidInTank(tankIndex).copy();
            int combinedTankIndex = outputOffset + tankIndex;
            if (combinedTankIndex >= 0 && combinedTankIndex < extractedFluidsFromTank.length && extractedFluidsFromTank[combinedTankIndex] > 0) {
                simulatedFluid.shrink(extractedFluidsFromTank[combinedTankIndex]);
            }
            simulatedFluidTanks[tankIndex] = simulatedFluid;
        }
        for (FluidStack outputFluid : outputFluids) {
            if (insertFluidIntoSimulatedTank(simulatedFluidTanks, outputTank, outputFluid.copy())) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean insertFluidIntoSimulatedTank(FluidStack[] simulatedTanks, IFluidHandler targetTank, FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            return true;
        }

        for (int tankIndex = 0; tankIndex < simulatedTanks.length; tankIndex++) {
            FluidStack tankStack = simulatedTanks[tankIndex];
            if (tankStack.isEmpty() || !FluidStack.isSameFluidSameComponents(tankStack, fluidStack) || !targetTank.isFluidValid(tankIndex, fluidStack)) {
                continue;
            }

            int insertedAmount = Math.min(fluidStack.getAmount(), targetTank.getTankCapacity(tankIndex) - tankStack.getAmount());
            if (insertedAmount > 0) {
                tankStack.setAmount(tankStack.getAmount() + insertedAmount);
            }
            return insertedAmount == fluidStack.getAmount();
        }

        int remainingAmount = fluidStack.getAmount();
        for (int tankIndex = 0; tankIndex < simulatedTanks.length; tankIndex++) {
            if (remainingAmount <= 0) {
                return true;
            }

            FluidStack tankStack = simulatedTanks[tankIndex];
            if (!tankStack.isEmpty() || !targetTank.isFluidValid(tankIndex, fluidStack)) {
                continue;
            }

            int insertedAmount = Math.min(remainingAmount, targetTank.getTankCapacity(tankIndex));
            if (insertedAmount <= 0) {
                continue;
            }

            FluidStack insertedStack = fluidStack.copy();
            insertedStack.setAmount(insertedAmount);
            simulatedTanks[tankIndex] = insertedStack;
            remainingAmount -= insertedAmount;
        }
        return remainingAmount <= 0;
    }

    private static boolean canAcceptGasOutputsAfterInputsAreConsumed(GasHandler availableGases, GasHandler outputTank, List<GasStack> outputGases, long[] extractedGasesFromTank) {
        if (outputGases.isEmpty()) {
            return true;
        }

        GasStack[] simulatedGasTanks = new GasStack[outputTank.getTanks()];
        int outputOffset = availableGases.getTanks() - outputTank.getTanks();
        for (int tankIndex = 0; tankIndex < outputTank.getTanks(); tankIndex++) {
            GasStack simulatedGas = outputTank.getGasInTank(tankIndex).copy();
            int combinedTankIndex = outputOffset + tankIndex;
            if (combinedTankIndex >= 0 && combinedTankIndex < extractedGasesFromTank.length && extractedGasesFromTank[combinedTankIndex] > 0) {
                simulatedGas.shrink(extractedGasesFromTank[combinedTankIndex]);
            }
            simulatedGasTanks[tankIndex] = simulatedGas;
        }
        for (GasStack outputGas : outputGases) {
            if (insertGasIntoSimulatedTank(simulatedGasTanks, outputTank, outputGas.copy())) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean insertGasIntoSimulatedTank(GasStack[] simulatedTanks, GasHandler targetTank, GasStack gasStack) {
        if (gasStack.isEmpty()) {
            return true;
        }

        if (!(targetTank instanceof GasStorageHandler pressurizedTarget)) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < simulatedTanks.length; tankIndex++) {
            GasStack tankStack = simulatedTanks[tankIndex];
            if (tankStack.isEmpty() || !GasStack.isSameGasSameComponents(tankStack, gasStack) || !targetTank.isGasValid(tankIndex, gasStack)) {
                continue;
            }

            long insertedAmount = Math.min(gasStack.getAmount(), pressurizedTarget.getTankMaxAmount(tankIndex) - tankStack.getAmount());
            if (insertedAmount > 0) {
                tankStack.setAmount(tankStack.getAmount() + insertedAmount);
            }
            return insertedAmount == gasStack.getAmount();
        }

        long remainingAmount = gasStack.getAmount();
        for (int tankIndex = 0; tankIndex < simulatedTanks.length; tankIndex++) {
            if (remainingAmount <= 0) {
                return true;
            }

            GasStack tankStack = simulatedTanks[tankIndex];
            if (!tankStack.isEmpty() || !targetTank.isGasValid(tankIndex, gasStack)) {
                continue;
            }

            long insertedAmount = Math.min(remainingAmount, pressurizedTarget.getTankMaxAmount(tankIndex));
            if (insertedAmount <= 0) {
                continue;
            }

            GasStack insertedStack = gasStack.copy();
            insertedStack.setAmount(insertedAmount);
            simulatedTanks[tankIndex] = insertedStack;
            remainingAmount -= insertedAmount;
        }

        return remainingAmount <= 0;
    }

    private IItemHandlerModifiable createItemOutputSimulation(int slotCount) {
        return new ItemStackHandler(slotCount) {

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                int firstFreeSlot = -1;
                for (int candidateSlot = 0; candidateSlot < getSlots(); candidateSlot++) {
                    ItemStack storedStack = getStackInSlot(candidateSlot);
                    if (candidateSlot != slot && ItemStack.isSameItemSameComponents(stack, storedStack)) {
                        return stack;
                    }

                    if (!storedStack.isEmpty() || firstFreeSlot != -1) {
                        continue;
                    }

                    firstFreeSlot = candidateSlot;
                }
                if (getStackInSlot(slot).isEmpty() && firstFreeSlot != slot) {
                    return stack;
                }

                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }
        };
    }

    private boolean canAcceptItemOutputsAfterInputsAreConsumed(IItemHandler availableItems, IItemHandler outputInventory, List<ItemStack> outputItems, int[] extractedItemsFromSlot) {
        if (outputItems.isEmpty()) {
            return true;
        }

        IItemHandlerModifiable simulatedInventory = createItemOutputSimulation(outputInventory.getSlots());
        int outputOffset = availableItems.getSlots() - outputInventory.getSlots();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            ItemStack simulatedStack = outputInventory.getStackInSlot(slot).copy();
            int combinedSlot = outputOffset + slot;
            if (combinedSlot >= 0 && combinedSlot < extractedItemsFromSlot.length && extractedItemsFromSlot[combinedSlot] > 0) {
                simulatedStack.shrink(extractedItemsFromSlot[combinedSlot]);
            }
            simulatedInventory.setStackInSlot(slot, simulatedStack);
        }
        for (ItemStack outputItem : outputItems) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(simulatedInventory, outputItem.copy(), false);
            if (remainder.isEmpty()) {
                continue;
            }

            return false;
        }

        return true;
    }
}
