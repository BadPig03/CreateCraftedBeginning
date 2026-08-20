package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.content.airtights.gas.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction.Participant;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipeContext.ConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipeContext.OutputPlan;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightForgingPressCrafting {
    private final AirtightForgingPressBlockEntity press;

    AirtightForgingPressCrafting(AirtightForgingPressBlockEntity press) {
        this.press = press;
    }

    private static boolean insertOutputs(SmartInventory inventory, List<ItemStack> outputItems) {
        for (ItemStack outputStack : outputItems) {
            if (outputStack.isEmpty()) {
                continue;
            }

            ItemStack remainingStack = ItemHandlerHelper.insertItemStacked(inventory, outputStack.copy(), false);
            if (!remainingStack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canConsumeItem(IItemHandler inventory, ItemStack expectedStack, int amount) {
        if (amount <= 0) {
            return true;
        }

        ItemStack currentStack = inventory.getStackInSlot(0);
        if (currentStack.isEmpty() || expectedStack.isEmpty() || currentStack.getCount() < amount || !ItemStack.isSameItemSameComponents(currentStack, expectedStack)) {
            return false;
        }

        ItemStack simulatedExtraction = inventory.extractItem(0, amount, true);
        return simulatedExtraction.getCount() == amount && ItemStack.isSameItemSameComponents(simulatedExtraction, expectedStack);
    }

    private static boolean consumeItem(IItemHandler inventory, ItemStack expectedStack, int amount) {
        if (amount <= 0) {
            return true;
        }

        ItemStack extractedStack = inventory.extractItem(0, amount, false);
        return extractedStack.getCount() == amount && ItemStack.isSameItemSameComponents(extractedStack, expectedStack);
    }

    private static Participant<ItemStack> itemConsumptionParticipant(IItemHandlerModifiable inventory, ItemStack expectedStack, int amount) {
        return ResourceTransaction.participant(() -> canConsumeItem(inventory, expectedStack, amount), () -> inventory.getStackInSlot(0).copy(), () -> consumeItem(inventory, expectedStack, amount), snapshot -> inventory.setStackInSlot(0, snapshot.copy()));
    }

    Optional<OutputPlan> planOutputs(List<ItemStack> outputItems) {
        SmartInventory simulatedOutput = createOutputSimulation();
        if (!insertOutputs(simulatedOutput, outputItems)) {
            return Optional.empty();
        }
        return Optional.of(new OutputPlan(MachineResourceSnapshots.copyItems(press.getOutputInventory()), MachineResourceSnapshots.copyItems(simulatedOutput)));
    }

    boolean acceptOutputs(List<ItemStack> outputItems, boolean simulate) {
        Optional<OutputPlan> plannedOutput = planOutputs(outputItems);
        if (plannedOutput.isEmpty()) {
            return false;
        }

        if (simulate) {
            return true;
        }

        OutputPlan outputPlan = plannedOutput.get();
        if (!outputPlanMatchesCurrent(outputPlan)) {
            return false;
        }

        applyOutputPlan(outputPlan);
        return true;
    }

    ConsumptionPlan createConsumptionPlan(ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, int[] fluidAmounts, long[] gasAmounts) {
        IFluidHandler fluidCapability = press.getFluidCapability();
        IGasHandler gasCapability = press.getGasCapability();
        if (fluidAmounts.length != fluidCapability.getTanks() || gasAmounts.length != gasCapability.getTanks()) {
            throw new IllegalArgumentException("Consumption plan tank count does not match the forging press");
        }
        if (fluidAmounts.length != 1 || gasAmounts.length != 1) {
            throw new IllegalStateException("The airtight forging press currently requires exactly one fluid tank and one gas tank");
        }

        ItemStack expectedPressHead = press.getPressHeadInventory().getStackInSlot(0).copy();
        FluidStack expectedFluid = press.getFluidTankBehaviour().getPrimaryHandler().getFluid().copy();
        GasStack expectedGas = press.getGasTankBehaviour().getPrimaryHandler().getGasStack().copy();
        return new ConsumptionPlan(expectedPressHead, expectedProcessingStack, processingAmount, expectedInputStack, inputAmount, expectedFluid, fluidAmounts[0], expectedGas, gasAmounts[0]);
    }

    boolean commitCraft(ConsumptionPlan consumptionPlan, OutputPlan outputPlan) {
        ResourceTransaction transaction = new ResourceTransaction().require(() -> ItemStack.matches(press.getPressHeadInventory().getStackInSlot(0), consumptionPlan.expectedPressHeadStack())).add(itemConsumptionParticipant(press.getAdditionInventory(), consumptionPlan.expectedProcessingStack(), consumptionPlan.processingAmount())).add(itemConsumptionParticipant(press.getInputInventory(), consumptionPlan.expectedInputStack(), consumptionPlan.inputAmount())).add(ResourceTransaction.participant(() -> canConsumeFluid(consumptionPlan), () -> press.getFluidTankBehaviour().getPrimaryHandler().getFluid().copy(), () -> consumeFluid(consumptionPlan), snapshot -> press.getFluidTankBehaviour().getPrimaryHandler().setFluid(snapshot.copy()))).add(ResourceTransaction.participant(() -> canConsumeGas(consumptionPlan), () -> press.getGasTankBehaviour().getPrimaryHandler().getGasStack().copy(), () -> consumeGas(consumptionPlan), snapshot -> press.getGasTankBehaviour().getPrimaryHandler().setGasStack(snapshot.copy()))).add(ResourceTransaction.participant(() -> outputPlanMatchesCurrent(outputPlan), () -> MachineResourceSnapshots.copyItems(press.getOutputInventory()), () -> {
            applyOutputPlan(outputPlan);
            return true;
        }, snapshot -> MachineResourceSnapshots.restoreItems(press.getOutputInventory(), snapshot)));
        return transaction.commit();
    }

    private boolean canConsumeFluid(ConsumptionPlan consumptionPlan) {
        if (consumptionPlan.fluidAmount() <= 0) {
            return true;
        }

        FluidStack currentFluid = press.getFluidTankBehaviour().getPrimaryHandler().getFluid();
        FluidStack expectedFluid = consumptionPlan.expectedFluid();
        if (currentFluid.isEmpty() || expectedFluid.isEmpty() || currentFluid.getAmount() < consumptionPlan.fluidAmount() || !FluidStack.isSameFluidSameComponents(currentFluid, expectedFluid)) {
            return false;
        }

        FluidStack simulatedDrain = press.getFluidTankBehaviour().getPrimaryHandler().drain(expectedFluid.copyWithAmount(consumptionPlan.fluidAmount()), FluidAction.SIMULATE);
        return simulatedDrain.getAmount() == consumptionPlan.fluidAmount();
    }

    private boolean consumeFluid(ConsumptionPlan consumptionPlan) {
        if (consumptionPlan.fluidAmount() <= 0) {
            return true;
        }

        FluidStack expectedFluid = consumptionPlan.expectedFluid();
        FluidStack drainedFluid = press.getFluidTankBehaviour().getPrimaryHandler().drain(expectedFluid.copyWithAmount(consumptionPlan.fluidAmount()), FluidAction.EXECUTE);
        return drainedFluid.getAmount() == consumptionPlan.fluidAmount() && FluidStack.isSameFluidSameComponents(drainedFluid, expectedFluid);
    }

    private boolean canConsumeGas(ConsumptionPlan consumptionPlan) {
        if (consumptionPlan.gasAmount() <= 0) {
            return true;
        }

        GasStack currentGas = press.getGasTankBehaviour().getPrimaryHandler().getGasStack();
        GasStack expectedGas = consumptionPlan.expectedGas();
        if (currentGas.isEmpty() || expectedGas.isEmpty() || currentGas.getAmount() < consumptionPlan.gasAmount() || !GasStack.isSameGasSameComponents(currentGas, expectedGas)) {
            return false;
        }

        GasStack simulatedDrain = press.getGasTankBehaviour().getPrimaryHandler().drain(expectedGas.copyWithAmount(consumptionPlan.gasAmount()), GasAction.SIMULATE);
        return simulatedDrain.getAmount() == consumptionPlan.gasAmount();
    }

    private boolean consumeGas(ConsumptionPlan consumptionPlan) {
        if (consumptionPlan.gasAmount() <= 0) {
            return true;
        }

        GasStack expectedGas = consumptionPlan.expectedGas();
        GasStack drainedGas = press.getGasTankBehaviour().getPrimaryHandler().drain(expectedGas.copyWithAmount(consumptionPlan.gasAmount()), GasAction.EXECUTE);
        return drainedGas.getAmount() == consumptionPlan.gasAmount() && GasStack.isSameGasSameComponents(drainedGas, expectedGas);
    }

    private boolean outputPlanMatchesCurrent(OutputPlan outputPlan) {
        SmartInventory outputInventory = press.getOutputInventory();
        int outputSlotCount = outputInventory.getSlots();
        List<ItemStack> expectedSlots = outputPlan.expectedSlots();
        if (expectedSlots.size() != outputSlotCount || outputPlan.finalSlots().size() != outputSlotCount) {
            return false;
        }

        for (int slot = 0; slot < outputSlotCount; slot++) {
            if (!ItemStack.matches(outputInventory.getStackInSlot(slot), expectedSlots.get(slot))) {
                return false;
            }
        }
        return true;
    }

    private void applyOutputPlan(OutputPlan outputPlan) {
        SmartInventory outputInventory = press.getOutputInventory();
        List<ItemStack> finalSlots = outputPlan.finalSlots();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            outputInventory.setStackInSlot(slot, finalSlots.get(slot).copy());
        }
    }

    private SmartInventory createOutputSimulation() {
        SmartInventory outputInventory = press.getOutputInventory();
        SmartInventory simulatedOutput = new SmartInventory(outputInventory.getSlots(), press);
        simulatedOutput.allowInsertion();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            simulatedOutput.setStackInSlot(slot, outputInventory.getStackInSlot(slot).copy());
        }
        return simulatedOutput;
    }
}
