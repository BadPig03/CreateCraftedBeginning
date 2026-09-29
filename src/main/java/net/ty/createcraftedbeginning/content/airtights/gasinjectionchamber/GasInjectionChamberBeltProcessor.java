package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer.InjectionMode;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationPlanner.BeltPlan;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots.GasTankSnapshot;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.FAN_PROCESSING;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.NONE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberBeltProcessor {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;
    private final GasInjectionChamberFilterState filter;
    private final GasInjectionChamberVisualState visual;
    private final GasInjectionChamberOperationPlanner planner;

    GasInjectionChamberBeltProcessor(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation, GasInjectionChamberFilterState filter, GasInjectionChamberVisualState visual, GasInjectionChamberOperationPlanner planner) {
        this.chamber = chamber;
        this.operation = operation;
        this.filter = filter;
        this.visual = visual;
        this.planner = planner;
    }

    ProcessingResult onItemEntered(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (handler.blockEntity.isVirtual()) {
            return PASS;
        }

        if (operation.isRunning()) {
            return HOLD;
        }

        if (planner.wasProcessedByInstalledFilter(transported)) {
            return PASS;
        }

        if (planner.createPlan(transported.stack).isEmpty()) {
            return PASS;
        }

        return HOLD;
    }

    ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (handler.blockEntity.isVirtual() || chamber.getLevel() == null) {
            return PASS;
        }

        if (operation.isRunning()) {
            if (operation.type == NONE || operation.hasAttemptedExecution() || operation.getProcessingTicks() > GasInjectionChamberBlockEntity.INJECTION_EXECUTION_TICK) {
                return HOLD;
            }

            operation.markExecutionAttempted();
            executeCurrentState(transported, handler);
            return HOLD;
        }

        if (planner.wasProcessedByInstalledFilter(transported)) {
            return PASS;
        }

        Optional<BeltPlan> planOptional = planner.createPlan(transported.stack);
        if (planOptional.isEmpty()) {
            return PASS;
        }

        BeltPlan plan = planOptional.get();
        if (!plan.hasRequiredGas()) {
            return HOLD;
        }

        operation.startProcessing(plan.type(), plan.pressureSpeedMultiplier(), plan.recipe());
        chamber.setChanged();
        chamber.notifyUpdate();
        return HOLD;
    }

    private static boolean replaceTransportedStack(BeltPlan plan, List<ItemStack> resultStacks, TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (!matchesPlanInput(plan, transported.stack)) {
            return false;
        }

        transported.stack.shrink(plan.batchSize());
        FanProcessingType completedFanProcessing = plan.type() == FAN_PROCESSING && plan.fanProcessingTypeId() != null ? GasInjectionChamberFilterItem.getFanProcessingType(plan.fanProcessingTypeId()).orElse(null) : null;
        TransportedItemStack heldRemainder = null;
        List<TransportedItemStack> transportedResults = new ArrayList<>(resultStacks.size());
        for (ItemStack resultStack : resultStacks) {
            TransportedItemStack transportedResult = transported.copy();
            transportedResult.stack = resultStack.copy();
            transportedResult.clearFanProcessingData();
            if (completedFanProcessing != null) {
                transportedResult.processedBy = completedFanProcessing;
                transportedResult.processingTime = -1;
            }
            transportedResults.add(transportedResult);
        }
        if (!transported.stack.isEmpty()) {
            heldRemainder = transported.copy();
            heldRemainder.clearFanProcessingData();
        }
        handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(transportedResults, heldRemainder));
        return true;
    }

    private static boolean matchesPlanInput(BeltPlan plan, ItemStack stack) {
        return ItemStack.isSameItemSameComponents(plan.input(), stack) && stack.getCount() >= plan.batchSize();
    }

    private void executeCurrentState(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (chamber.getLevel() == null) {
            return;
        }

        Optional<BeltPlan> planOptional = operation.type == OperationType.ITEM_RECIPE ? planner.createRecipePlan(transported.stack, operation.getRecipe()) : planner.createPlan(transported.stack);
        if (planOptional.isEmpty()) {
            return;
        }

        BeltPlan plan = planOptional.get();
        if (plan.type() != operation.type || !plan.hasRequiredGas()) {
            return;
        }

        int cloudColor = plan.type() == FAN_PROCESSING ? GasInjectionChamberFilterItem.getColor(filter.getInstalledFilter()) : plan.gas().getHint();
        boolean executionSucceeded;
        SmartGasTankBehaviour tankBehaviour = chamber.getGasTankBehaviour();
        tankBehaviour.beginMutation();
        try {
            executionSucceeded = executePlan(plan, transported, handler);
        }
        finally {
            tankBehaviour.endMutation();
        }

        if (!executionSucceeded) {
            tankBehaviour.sendDataImmediately();
            return;
        }

        visual.queueCloud(cloudColor);
        tankBehaviour.sendDataImmediately();
        Level level = chamber.getLevel();
        CCBSoundEvents.INJECTING.playOnServer(level, chamber.getBlockPos(), 0.75F, 0.9F + 0.2F * level.random.nextFloat());
    }

    private boolean executePlan(BeltPlan plan, TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return switch (plan.type()) {
            case CANISTER -> executeCanisterPlan(plan, transported);
            case ITEM_RECIPE, FAN_PROCESSING -> executeItemPlan(plan, transported, handler);
            case BASIN_RECIPE, NONE -> false;
        };
    }

    private boolean executeCanisterPlan(BeltPlan plan, TransportedItemStack transported) {
        if (chamber.getLevel() == null) {
            return false;
        }

        GasStack gasRequest = plan.gasRequest();
        if (gasRequest.isEmpty()) {
            return false;
        }

        GasCanisterContainer canisterContents = transported.stack.getCapability(CanisterCapabilities.ITEM);
        if (canisterContents == null || canisterContents.getInjectionMode() == InjectionMode.DENY) {
            return false;
        }

        ResourceTransaction transaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> GasInjectionChamberCanisterTransfer.canTransferExactly(chamber.getGasTank(), canisterContents, gasRequest, gasRequest.getAmount()), () -> new CanisterTransferSnapshot(MachineResourceSnapshots.snapshotGasTanks(chamber.getGasTankBehaviour()), transported.stack.copy()), () -> GasInjectionChamberCanisterTransfer.transferExactly(chamber.getGasTank(), canisterContents, gasRequest, gasRequest.getAmount()), snapshot -> {
            MachineResourceSnapshots.restoreGasTanks(snapshot.gasTankSnapshot(), chamber.getGasTankBehaviour());
            transported.stack = snapshot.itemStack().copy();
        }));
        return transaction.commit();
    }

    private boolean executeItemPlan(BeltPlan plan, TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (chamber.getLevel() == null || !matchesPlanInput(plan, transported.stack)) {
            return false;
        }

        Optional<List<ItemStack>> resultStacksOptional = new GasInjectionChamberBeltOutputs(chamber, filter).createResults(plan);
        if (resultStacksOptional.isEmpty()) {
            return false;
        }

        List<ItemStack> resultStacks = resultStacksOptional.get();
        ResourceTransaction transaction = new ResourceTransaction();
        if (plan.recipeGasPlan() != null) {
            transaction.add(GasInjectionChamberTransactions.gasParticipant(chamber, plan.recipeGasPlan()));
        }
        else {
            GasStack gasRequest = plan.gasRequest();
            if (!gasRequest.isEmpty()) {
                transaction.add(plan.type() == FAN_PROCESSING ? GasInjectionChamberTransactions.gasParticipant(chamber, gasRequest, plan.sourcePressurePa()) : GasInjectionChamberTransactions.gasParticipant(chamber, gasRequest));
            }
        }
        transaction.add(ResourceTransaction.participant(() -> matchesPlanInput(plan, transported.stack), () -> transported.stack.copy(), () -> replaceTransportedStack(plan, resultStacks, transported, handler), snapshot -> transported.stack = snapshot.copy()));
        return transaction.commit();
    }

    private record CanisterTransferSnapshot(GasTankSnapshot gasTankSnapshot, ItemStack itemStack) {}
}
