package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer.MachineFillingStrategy;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.BASIN_RECIPE;
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

        chamber.clearOperationState();
        return planner.prepareOperation(transported.stack) ? HOLD : PASS;
    }

    ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (handler.blockEntity.isVirtual() || chamber.getLevel() == null) {
            return PASS;
        }
        if (operation.isRunning() && operation.type == NONE) {
            return HOLD;
        }
        if (operation.executed || operation.type == BASIN_RECIPE) {
            return HOLD;
        }
        if (operation.type == NONE && planner.wasProcessedByInstalledFilter(transported)) {
            return PASS;
        }
        if (operation.type == NONE && !planner.prepareOperation(transported.stack)) {
            return PASS;
        }
        if (!matchesOperationInput(transported.stack)) {
            chamber.cancelOperationState();
            return PASS;
        }
        if (!operation.isRunning()) {
            return startProcessing(transported.stack);
        }
        if (operation.getProcessingTicks() > GasInjectionChamberBlockEntity.INJECTION_EXECUTION_TICK) {
            return HOLD;
        }
        return executeInjection(transported, handler);
    }

    boolean isFanProcessingOperationStillValid(ResourceLocation typeId) {
        return planner.isFanProcessingOperationStillValid(typeId);
    }

    private ProcessingResult startProcessing(ItemStack itemStack) {
        if (operation.type == FAN_PROCESSING && !planner.isFanProcessingOperationStillValid(operation.fanProcessingTypeId)) {
            chamber.clearOperationState();
            if (!planner.prepareOperation(itemStack)) {
                return PASS;
            }
        }

        if (operation.type.usesGas) {
            GasStack tankGas = chamber.getGasInTank();
            if (tankGas.isEmpty()) {
                return HOLD;
            }
            if (!GasStack.isSameGasSameComponents(tankGas, operation.gas)) {
                chamber.clearOperationState();
                if (!planner.prepareOperation(itemStack)) {
                    return PASS;
                }
                tankGas = chamber.getGasInTank();
            }
            if (tankGas.getAmount() < operation.gas.getAmount()) {
                return HOLD;
            }
        }

        if (!planner.prepareOperationResultsIfNeeded(itemStack)) {
            chamber.cancelOperationState();
            return PASS;
        }

        operation.setProcessingTicks(GasInjectionChamberBlockEntity.PROCESSING_TIME + GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME);
        chamber.notifyUpdate();
        return HOLD;
    }

    private ProcessingResult executeInjection(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (chamber.getLevel() == null) {
            return PASS;
        }

        int color = operation.type == FAN_PROCESSING ? GasInjectionChamberUtils.getColor(filter.getInstalledFilter()) : operation.gas.getHint();
        boolean executed;
        chamber.getGasTankBehaviour().beginMutation();
        try {
            executed = executeOperation(transported, handler);
        } finally {
            chamber.getGasTankBehaviour().endMutation();
        }

        if (executed) {
            operation.executed = true;
            visual.queueCloud(color);
        }
        else {
            operation.setProcessingTicks(-1);
            chamber.clearOperationState();
        }

        chamber.getGasTankBehaviour().sendDataImmediately();
        if (!executed) {
            return PASS;
        }

        CCBSoundEvents.INJECTING.playOnServer(chamber.getLevel(), chamber.getBlockPos(), 0.75f, 0.9f + 0.2f * chamber.getLevel().random.nextFloat());
        return HOLD;
    }

    private boolean executeOperation(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return switch (operation.type) {
            case CANISTER -> executeCanisterOperation(transported);
            case ITEM_RECIPE -> operation.resultPrepared && drainAndReplace(transported, handler);
            case FAN_PROCESSING -> executeFanProcessingOperation(transported, handler);
            case BASIN_RECIPE, NONE -> false;
        };
    }

    private boolean executeCanisterOperation(TransportedItemStack transported) {
        if (chamber.getLevel() == null || operation.gas.isEmpty()) {
            return false;
        }

        IGasCanisterContainer canisterContents = transported.stack.getCapability(GasHandler.ITEM);
        if (canisterContents == null || canisterContents.getMachineFillingStrategy() == MachineFillingStrategy.DENY) {
            return false;
        }

        Provider provider = chamber.getLevel().registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(GasInjectionChamberTransactions.operationGasParticipant(chamber, operation, provider)).add(ResourceTransaction.participant(() -> canisterContents.fill(0, operation.gas, GasAction.SIMULATE) == operation.gas.getAmount(), () -> transported.stack.copy(), () -> canisterContents.fill(0, operation.gas, GasAction.EXECUTE) == operation.gas.getAmount(), snapshot -> transported.stack = snapshot.copy()));
        return transaction.commit();
    }

    private boolean executeFanProcessingOperation(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (!operation.resultPrepared || !planner.isFanProcessingOperationStillValid(operation.fanProcessingTypeId)) {
            return false;
        }
        if (!GasInjectionChamberUtils.consumesFanProcessingGas(operation.gas)) {
            return replaceTransportedStackWithPreparedResults(transported, handler);
        }
        return drainAndReplace(transported, handler);
    }

    private boolean drainAndReplace(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (chamber.getLevel() == null || !canReplaceTransportedStackWithPreparedResults(transported)) {
            return false;
        }

        Provider provider = chamber.getLevel().registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(GasInjectionChamberTransactions.operationGasParticipant(chamber, operation, provider)).add(ResourceTransaction.participant(() -> canReplaceTransportedStackWithPreparedResults(transported), () -> transported.stack.copy(), () -> replaceTransportedStackWithPreparedResults(transported, handler), snapshot -> transported.stack = snapshot.copy()));
        return transaction.commit();
    }

    private boolean canReplaceTransportedStackWithPreparedResults(TransportedItemStack transported) {
        int batchSize = operation.input.getCount();
        return batchSize > 0 && transported.stack.getCount() >= batchSize && matchesOperationInput(transported.stack);
    }

    private boolean replaceTransportedStackWithPreparedResults(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (!canReplaceTransportedStackWithPreparedResults(transported)) {
            return false;
        }

        int batchSize = operation.input.getCount();
        transported.stack.shrink(batchSize);
        FanProcessingType completedFanProcessing = operation.type == FAN_PROCESSING && operation.fanProcessingTypeId != null ? GasInjectionChamberUtils.getFanProcessingType(operation.fanProcessingTypeId).orElse(null) : null;

        TransportedItemStack held = null;
        List<TransportedItemStack> results = new ArrayList<>(operation.results.size());
        for (ItemStack resultStack : operation.results) {
            TransportedItemStack result = transported.copy();
            result.stack = resultStack.copy();
            result.clearFanProcessingData();
            if (completedFanProcessing != null) {
                result.processedBy = completedFanProcessing;
                result.processingTime = -1;
            }
            results.add(result);
        }
        if (!transported.stack.isEmpty()) {
            held = transported.copy();
            held.clearFanProcessingData();
        }
        handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(results, held));
        return true;
    }

    private boolean matchesOperationInput(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(operation.input, stack) && stack.getCount() >= operation.input.getCount();
    }
}
