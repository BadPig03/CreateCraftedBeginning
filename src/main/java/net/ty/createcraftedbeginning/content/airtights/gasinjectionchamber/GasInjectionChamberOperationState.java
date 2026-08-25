package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberOperationState {
    static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";

    OperationType type = OperationType.NONE;
    private int processingTicks = -1;
    private int previousProcessingTicks = -1;
    private boolean executionAttempted;

    int getProcessingTicks() {
        return processingTicks;
    }

    int getPreviousProcessingTicks() {
        return previousProcessingTicks;
    }

    void synchronizeProcessingTicks(int synchronizedTicks, boolean clientPacket) {
        if (clientPacket && processingTicks >= 0 && synchronizedTicks >= 0) {
            return;
        }

        processingTicks = synchronizedTicks;
        previousProcessingTicks = synchronizedTicks;
    }

    void capturePreviousProcessingTicks() {
        previousProcessingTicks = processingTicks;
    }

    void decrementProcessingTicks() {
        --processingTicks;
    }

    boolean isRunning() {
        return processingTicks >= 0;
    }

    boolean hasAttemptedExecution() {
        return executionAttempted;
    }

    void markExecutionAttempted() {
        executionAttempted = true;
    }

    void startProcessing(OperationType type) {
        this.type = type;
        processingTicks = GasInjectionChamberBlockEntity.PROCESSING_TIME + GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME;
        executionAttempted = false;
    }

    void clearTransientOperation() {
        type = OperationType.NONE;
        executionAttempted = false;
    }

    enum OperationType {
        NONE,
        ITEM_RECIPE,
        BASIN_RECIPE,
        CANISTER,
        FAN_PROCESSING
    }
}
