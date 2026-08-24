package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberOperationState {
    public static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";

    public OperationType type = OperationType.NONE;
    private int processingTicks = -1;
    private int previousProcessingTicks = -1;
    private boolean executionAttempted;

    public int getProcessingTicks() {
        return processingTicks;
    }

    public int getPreviousProcessingTicks() {
        return previousProcessingTicks;
    }

    public void synchronizeProcessingTicks(int synchronizedTicks, boolean clientPacket) {
        if (clientPacket && processingTicks >= 0 && synchronizedTicks >= 0) {
            return;
        }

        processingTicks = synchronizedTicks;
        previousProcessingTicks = synchronizedTicks;
    }

    public void capturePreviousProcessingTicks() {
        previousProcessingTicks = processingTicks;
    }

    public void decrementProcessingTicks() {
        --processingTicks;
    }

    public boolean isRunning() {
        return processingTicks >= 0;
    }

    public boolean hasAttemptedExecution() {
        return executionAttempted;
    }

    public void markExecutionAttempted() {
        executionAttempted = true;
    }

    public void startProcessing(OperationType type, int ticks) {
        this.type = type;
        processingTicks = ticks;
        executionAttempted = false;
    }

    public void clearTransientOperation() {
        type = OperationType.NONE;
        executionAttempted = false;
    }

    public enum OperationType {
        NONE,
        ITEM_RECIPE,
        BASIN_RECIPE,
        CANISTER,
        FAN_PROCESSING
    }
}
