package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.RecipeManager;
import net.ty.createcraftedbeginning.recipe.CCBRecipeReloads;
import net.ty.createcraftedbeginning.recipe.CCBRecipeReloads.Generation;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberOperationState {
    @Internal
    public static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";
    static final String COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER = "PressureSpeedMultiplier";
    static final String COMPOUND_KEY_PROCESSING_CYCLE = "ProcessingCycle";

    OperationType type = OperationType.NONE;
    private float processingTicks = -1;
    private float previousProcessingTicks = -1;
    private float pressureSpeedMultiplier = 1;
    private int processingCycle;
    private boolean executionAttempted;
    private @Nullable GasInjectionRecipe recipe;
    private @Nullable Generation recipeGeneration;

    private static float sanitizePressureSpeedMultiplier(float multiplier) {
        if (Float.isFinite(multiplier) && multiplier >= 1.0F) {
            return multiplier;
        }

        return 1;
    }

    @Internal
    public void startProcessing(OperationType type, float pressureSpeedMultiplier, @Nullable GasInjectionRecipe recipe, RecipeManager recipeManager) {
        processingCycle++;
        this.type = type;
        this.pressureSpeedMultiplier = sanitizePressureSpeedMultiplier(pressureSpeedMultiplier);
        this.recipe = recipe;
        recipeGeneration = recipe == null ? null : CCBRecipeReloads.getGeneration(recipeManager);
        processingTicks = GasInjectionChamberBlockEntity.PROCESSING_TIME + GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME;
        previousProcessingTicks = processingTicks;
        executionAttempted = false;
    }

    float getProcessingTicks() {
        return processingTicks;
    }

    float getPreviousProcessingTicks() {
        return previousProcessingTicks;
    }

    float getPressureSpeedMultiplier() {
        return pressureSpeedMultiplier;
    }

    int getProcessingCycle() {
        return processingCycle;
    }

    @Nullable GasInjectionRecipe getRecipe() {
        return recipe;
    }

    boolean isRecipeCurrent(RecipeManager recipeManager) {
        return recipe == null || recipeGeneration == CCBRecipeReloads.getGeneration(recipeManager);
    }

    void loadProcessingState(float syncedTicks, float syncedSpeedMultiplier, int syncedCycle, boolean clientPacket) {
        boolean sameRunningCycle = processingTicks >= 0 && syncedTicks >= 0 && processingCycle == syncedCycle;
        if (clientPacket && sameRunningCycle) {
            return;
        }

        processingCycle = syncedCycle;
        pressureSpeedMultiplier = sanitizePressureSpeedMultiplier(syncedSpeedMultiplier);
        processingTicks = syncedTicks;
        previousProcessingTicks = syncedTicks;
    }

    void capturePreviousProcessingTicks() {
        previousProcessingTicks = processingTicks;
    }

    void advanceProcessingTicks() {
        processingTicks -= pressureSpeedMultiplier;
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

    void clearTransientOperation() {
        type = OperationType.NONE;
        processingTicks = -1;
        previousProcessingTicks = -1;
        pressureSpeedMultiplier = 1;
        executionAttempted = false;
        recipe = null;
        recipeGeneration = null;
    }

    @Internal
    public enum OperationType {
        NONE,
        ITEM_RECIPE,
        BASIN_RECIPE,
        CANISTER,
        FAN_PROCESSING
    }
}
