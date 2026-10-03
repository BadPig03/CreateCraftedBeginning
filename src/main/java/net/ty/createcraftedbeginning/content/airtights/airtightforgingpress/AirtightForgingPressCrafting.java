package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.foundation.transaction.TransactionParticipant;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPreparation;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.ConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.OutputPlan;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressCrafting {
    private final AirtightForgingPressBlockEntity press;

    AirtightForgingPressCrafting(AirtightForgingPressBlockEntity press) {
        this.press = press;
    }

    @Internal
    public static boolean applyRecipe(AirtightForgingPressBlockEntity press, ForgingPressRecipe recipe) {
        return ForgingPressCraftPreparation.prepare(press, recipe).map(plan -> press.commitCraft(plan.consumption(), plan.output())).orElse(false);
    }

    @Internal
    public static boolean applyPressingRecipe(AirtightForgingPressBlockEntity press, AirtightForgingPressPressingRecipe recipe) {
        return AirtightForgingPressAutomationPlanner.preparePressingRecipe(press, recipe).map(plan -> press.commitCraft(plan.consumption(), plan.output())).orElse(false);
    }

    @Internal
    public static boolean applySmithingRecipe(AirtightForgingPressBlockEntity press, SmithingRecipe recipe) {
        return AirtightForgingPressAutomationPlanner.planSmithingRecipe(press, recipe).map(plan -> press.commitCraft(plan.consumption(), plan.output())).orElse(false);
    }

    private static boolean insertOutputs(SmartInventory inventory, List<ItemStack> outputItems) {
        for (ItemStack outputStack : outputItems) {
            if (outputStack.isEmpty()) {
                continue;
            }

            ItemStack remainingStack = ItemHandlerHelper.insertItemStacked(inventory, outputStack.copy(), false);
            if (remainingStack.isEmpty()) {
                continue;
            }

            return false;
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

    private static TransactionParticipant<ItemStack> itemConsumptionParticipant(IItemHandlerModifiable inventory, ItemStack expectedStack, int amount) {
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

    ConsumptionPlan createConsumptionPlan(ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, int[] fluidAmounts, GasConsumptionPlan gasPlan) {
        IFluidHandler fluidCapability = press.getFluidCapability();
        GasStorageHandler gasCapability = press.getGasCapability();
        if (fluidAmounts.length != fluidCapability.getTanks() || gasPlan.tankAmounts().length != gasCapability.getTanks()) {
            throw new IllegalArgumentException("Forging press consumption plan tank count mismatch: expected " + fluidCapability.getTanks() + " fluid tanks and " + gasCapability.getTanks() + " gas tanks, got " + fluidAmounts.length + " and " + gasPlan.tankAmounts().length + ", respectively.");
        }

        if (fluidAmounts.length != 1 || gasCapability.getTanks() != 1) {
            throw new IllegalStateException("The airtight forging press requires exactly one fluid tank and one gas tank; got " + fluidAmounts.length + " fluid tanks and " + gasCapability.getTanks() + " gas tanks.");
        }

        ItemStack expectedPressHead = press.getPressHeadInventory().getStackInSlot(0).copy();
        FluidStack expectedFluid = press.getFluidTankBehaviour().getPrimaryHandler().getFluid().copy();
        return new ConsumptionPlan(expectedPressHead, expectedProcessingStack, processingAmount, expectedInputStack, inputAmount, expectedFluid, fluidAmounts[0], gasPlan);
    }

    boolean commitCraft(ConsumptionPlan consumptionPlan, OutputPlan outputPlan) {
        SmartFluidTank fluidTank = press.getFluidTankBehaviour().getPrimaryHandler();
        SmartGasTank gasTank = press.getGasTankBehaviour().getPrimaryHandler();
        SmartInventory outputInventory = press.getOutputInventory();
        ResourceTransaction transaction = new ResourceTransaction().require(() -> ItemStack.matches(press.getPressHeadInventory().getStackInSlot(0), consumptionPlan.expectedPressHeadStack())).add(itemConsumptionParticipant(press.getAdditionInventory(), consumptionPlan.expectedProcessingStack(), consumptionPlan.processingAmount())).add(itemConsumptionParticipant(press.getInputInventory(), consumptionPlan.expectedInputStack(), consumptionPlan.inputAmount())).add(ResourceTransaction.participant(() -> canConsumeFluid(consumptionPlan), () -> fluidTank.getFluid().copy(), () -> consumeFluid(consumptionPlan), snapshot -> fluidTank.setFluid(snapshot.copy()))).add(ResourceTransaction.participant(consumptionPlan.gasPlan()::canExecute, () -> MachineResourceSnapshots.snapshotGas(gasTank), consumptionPlan.gasPlan()::execute, snapshot -> MachineResourceSnapshots.restoreGas(gasTank, snapshot))).add(ResourceTransaction.participant(() -> outputPlanMatchesCurrent(outputPlan), () -> MachineResourceSnapshots.copyItems(outputInventory), () -> {
            applyOutputPlan(outputPlan);
            return true;
        }, snapshot -> MachineResourceSnapshots.restoreItems(outputInventory, snapshot)));
        boolean committed = transaction.commit();
        if (!committed) {
            return false;
        }

        ItemStack input = consumptionPlan.expectedInputStack();
        if (consumptionPlan.inputAmount() <= 0 || !input.is(Items.CHARCOAL) && !input.is(Items.COAL)) {
            return true;
        }

        int diamondsBefore = outputPlan.expectedSlots().stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
        int diamondsAfter = outputPlan.finalSlots().stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
        if (diamondsAfter <= diamondsBefore) {
            return true;
        }

        press.getBehaviour(CCBAdvancementBehaviour.TYPE).awardPlayer(CCBAdvancements.SUPERMASSIVE);
        return true;
    }

    private boolean canConsumeFluid(ConsumptionPlan consumptionPlan) {
        if (consumptionPlan.fluidAmount() <= 0) {
            return true;
        }

        SmartFluidTank fluidTank = press.getFluidTankBehaviour().getPrimaryHandler();
        FluidStack currentFluid = fluidTank.getFluid();
        FluidStack expectedFluid = consumptionPlan.expectedFluid();
        if (currentFluid.isEmpty() || expectedFluid.isEmpty() || currentFluid.getAmount() < consumptionPlan.fluidAmount() || !FluidStack.isSameFluidSameComponents(currentFluid, expectedFluid)) {
            return false;
        }

        FluidStack simulatedDrain = fluidTank.drain(expectedFluid.copyWithAmount(consumptionPlan.fluidAmount()), FluidAction.SIMULATE);
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
