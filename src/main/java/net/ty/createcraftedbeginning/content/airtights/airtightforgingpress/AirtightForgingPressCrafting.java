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
import net.ty.createcraftedbeginning.content.airtights.transaction.MachineResourceSnapshots;
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
        for (ItemStack stack : outputItems) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canConsumeItem(IItemHandler inventory, ItemStack expectedStack, int amount) {
        if (amount <= 0) {
            return true;
        }
        ItemStack current = inventory.getStackInSlot(0);
        if (current.isEmpty() || expectedStack.isEmpty() || current.getCount() < amount || !ItemStack.isSameItemSameComponents(current, expectedStack)) {
            return false;
        }
        ItemStack simulated = inventory.extractItem(0, amount, true);
        return simulated.getCount() == amount && ItemStack.isSameItemSameComponents(simulated, expectedStack);
    }

    private static boolean consumeItem(IItemHandler inventory, ItemStack expectedStack, int amount) {
        if (amount <= 0) {
            return true;
        }
        ItemStack extracted = inventory.extractItem(0, amount, false);
        return extracted.getCount() == amount && ItemStack.isSameItemSameComponents(extracted, expectedStack);
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

    private boolean canConsumeFluid(ConsumptionPlan plan) {
        if (plan.fluidAmount() <= 0) {
            return true;
        }
        FluidStack current = press.getFluidTankBehaviour().getPrimaryHandler().getFluid();
        FluidStack expected = plan.expectedFluid();
        if (current.isEmpty() || expected.isEmpty() || current.getAmount() < plan.fluidAmount() || !FluidStack.isSameFluidSameComponents(current, expected)) {
            return false;
        }
        FluidStack simulated = press.getFluidTankBehaviour().getPrimaryHandler().drain(expected.copyWithAmount(plan.fluidAmount()), FluidAction.SIMULATE);
        return simulated.getAmount() == plan.fluidAmount();
    }

    private boolean consumeFluid(ConsumptionPlan plan) {
        if (plan.fluidAmount() <= 0) {
            return true;
        }
        FluidStack expected = plan.expectedFluid();
        FluidStack drained = press.getFluidTankBehaviour().getPrimaryHandler().drain(expected.copyWithAmount(plan.fluidAmount()), FluidAction.EXECUTE);
        return drained.getAmount() == plan.fluidAmount() && FluidStack.isSameFluidSameComponents(drained, expected);
    }

    private boolean canConsumeGas(ConsumptionPlan plan) {
        if (plan.gasAmount() <= 0) {
            return true;
        }
        GasStack current = press.getGasTankBehaviour().getPrimaryHandler().getGasStack();
        GasStack expected = plan.expectedGas();
        if (current.isEmpty() || expected.isEmpty() || current.getAmount() < plan.gasAmount() || !GasStack.isSameGasSameComponents(current, expected)) {
            return false;
        }
        GasStack simulated = press.getGasTankBehaviour().getPrimaryHandler().drain(expected.copyWithAmount(plan.gasAmount()), GasAction.SIMULATE);
        return simulated.getAmount() == plan.gasAmount();
    }

    private boolean consumeGas(ConsumptionPlan plan) {
        if (plan.gasAmount() <= 0) {
            return true;
        }
        GasStack expected = plan.expectedGas();
        GasStack drained = press.getGasTankBehaviour().getPrimaryHandler().drain(expected.copyWithAmount(plan.gasAmount()), GasAction.EXECUTE);
        return drained.getAmount() == plan.gasAmount() && GasStack.isSameGasSameComponents(drained, expected);
    }

    private boolean outputPlanMatchesCurrent(OutputPlan outputPlan) {
        SmartInventory outputInventory = press.getOutputInventory();
        int slots = outputInventory.getSlots();
        List<ItemStack> expectedSlots = outputPlan.expectedSlots();
        if (expectedSlots.size() != slots || outputPlan.finalSlots().size() != slots) {
            return false;
        }
        for (int slot = 0; slot < slots; slot++) {
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
