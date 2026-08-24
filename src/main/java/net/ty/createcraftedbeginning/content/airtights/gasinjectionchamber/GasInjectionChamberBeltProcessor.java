package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer.InjectionMode;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationPlanner.BeltPlan;
import net.ty.createcraftedbeginning.core.ResourceTransaction;
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

    private static boolean replaceTransportedStack(BeltPlan plan, List<ItemStack> resultStacks, TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (!matchesPlanInput(plan, transported.stack)) {
            return false;
        }

        transported.stack.shrink(plan.batchSize());
        FanProcessingType completedFanProcessing = plan.type() == FAN_PROCESSING && plan.fanProcessingTypeId() != null ? GasInjectionChamberUtils.getFanProcessingType(plan.fanProcessingTypeId()).orElse(null) : null;
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
        return planner.createPlan(transported.stack).isPresent() ? HOLD : PASS;
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
            return executeCurrentState(transported, handler);
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

        operation.startProcessing(plan.type(), GasInjectionChamberBlockEntity.PROCESSING_TIME + GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME);
        chamber.setChanged();
        chamber.notifyUpdate();
        return HOLD;
    }

    private ProcessingResult executeCurrentState(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (chamber.getLevel() == null) {
            return HOLD;
        }

        Optional<BeltPlan> planOptional = planner.createPlan(transported.stack);
        if (planOptional.isEmpty()) {
            return chamber.getGasInTank().isEmpty() ? HOLD : PASS;
        }

        BeltPlan plan = planOptional.get();
        if (!plan.hasRequiredGas()) {
            return HOLD;
        }

        int cloudColor = plan.type() == FAN_PROCESSING ? GasInjectionChamberUtils.getColor(filter.getInstalledFilter()) : plan.gas().getHint();
        boolean executionSucceeded;
        chamber.getGasTankBehaviour().beginMutation();
        try {
            executionSucceeded = executePlan(plan, transported, handler);
        } finally {
            chamber.getGasTankBehaviour().endMutation();
        }

        if (!executionSucceeded) {
            chamber.getGasTankBehaviour().sendDataImmediately();
            return HOLD;
        }

        visual.queueCloud(cloudColor);
        chamber.getGasTankBehaviour().sendDataImmediately();
        CCBSoundEvents.INJECTING.playOnServer(chamber.getLevel(), chamber.getBlockPos(), 0.75f, 0.9f + 0.2f * chamber.getLevel().random.nextFloat());
        return HOLD;
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

        IGasCanisterContainer canisterContents = transported.stack.getCapability(GasHandler.ITEM);
        if (canisterContents == null || canisterContents.getInjectionMode() == InjectionMode.DENY) {
            return false;
        }

        ResourceTransaction transaction = new ResourceTransaction().add(GasInjectionChamberTransactions.gasParticipant(chamber, gasRequest)).add(ResourceTransaction.participant(() -> canisterContents.fill(0, gasRequest, GasAction.SIMULATE) == gasRequest.getAmount(), () -> transported.stack.copy(), () -> canisterContents.fill(0, gasRequest, GasAction.EXECUTE) == gasRequest.getAmount(), snapshot -> transported.stack = snapshot.copy()));
        return transaction.commit();
    }

    private boolean executeItemPlan(BeltPlan plan, TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (chamber.getLevel() == null || !matchesPlanInput(plan, transported.stack)) {
            return false;
        }

        Optional<List<ItemStack>> resultStacksOptional = planner.createResults(plan);
        if (resultStacksOptional.isEmpty()) {
            return false;
        }

        List<ItemStack> resultStacks = resultStacksOptional.get();
        ResourceTransaction transaction = new ResourceTransaction();
        GasStack gasRequest = plan.gasRequest();
        if (!gasRequest.isEmpty()) {
            transaction.add(GasInjectionChamberTransactions.gasParticipant(chamber, gasRequest));
        }
        transaction.add(ResourceTransaction.participant(() -> matchesPlanInput(plan, transported.stack), () -> transported.stack.copy(), () -> replaceTransportedStack(plan, resultStacks, transported, handler), snapshot -> transported.stack = snapshot.copy()));
        return transaction.commit();
    }
}
