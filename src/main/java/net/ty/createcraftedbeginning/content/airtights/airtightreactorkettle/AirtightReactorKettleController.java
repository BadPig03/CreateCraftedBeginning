package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ReactorKettleMixingRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleController {
    static final int PROCESSING_STARTED = 20;
    private static final int OPERATING_FINISHED = 40;
    private static final int PONDER_PROCESSING_DURATION = 50;
    private final AirtightReactorKettleBlockEntity kettle;
    private final AirtightReactorKettleAnimationState animationState;

    private boolean observedAutomaticShapelessEnabled;
    private boolean observedAutomaticMixingEnabled;
    private boolean observedAutomaticBrewingEnabled;
    private boolean contentsChanged = true;
    private boolean filterChanged;
    private boolean ponderProcessRequested;
    private boolean operating;
    private boolean windowsOpenState = true;
    private int operatingTicks;
    private int processingTicks = -1;
    private float operationKineticSpeed;
    private float processingKineticSpeed;
    private float pressureSpeedMultiplier = 1;
    private int operationCycle;
    private int processingCycle;
    private CraftingRecipe currentCraftingRecipe;
    private ReactorKettleRecipe currentRecipe;
    private long observedRecipeCacheVersion;
    private long operationRecipeCacheVersion;

    @Internal
    public AirtightReactorKettleController(AirtightReactorKettleBlockEntity kettle, AirtightReactorKettleAnimationState animationState) {
        this.kettle = kettle;
        this.animationState = animationState;
        observedAutomaticShapelessEnabled = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticShapelessRecipes.get();
        observedAutomaticMixingEnabled = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticMixingRecipes.get();
        observedAutomaticBrewingEnabled = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticBrewingRecipes.get();
        observedRecipeCacheVersion = AirtightReactorKettleRecipeLookup.getRecipeCacheVersion();
    }

    private static float sanitizeKineticSpeed(float speed) {
        if (!Float.isFinite(speed)) {
            return 0;
        }

        return speed;
    }

    private static float sanitizePressureSpeedMultiplier(float multiplier) {
        if (Float.isFinite(multiplier) && multiplier >= 1.0F) {
            return multiplier;
        }

        return 1;
    }

    @Internal
    public void tick() {
        if (kettle.getLevel() == null) {
            return;
        }

        tickOperation();
        if (!contentsChanged) {
            return;
        }

        contentsChanged = false;
        kettle.scheduleUpdate();
    }

    @Internal
    public void lazyTick() {
        Level level = kettle.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long recipeCacheVersion = AirtightReactorKettleRecipeLookup.getRecipeCacheVersion();
        boolean automaticShapelessEnabled = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticShapelessRecipes.get();
        boolean automaticMixingEnabled = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticMixingRecipes.get();
        boolean automaticBrewingEnabled = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticBrewingRecipes.get();
        if (observedRecipeCacheVersion == recipeCacheVersion && observedAutomaticShapelessEnabled == automaticShapelessEnabled && observedAutomaticMixingEnabled == automaticMixingEnabled && observedAutomaticBrewingEnabled == automaticBrewingEnabled) {
            return;
        }

        observedRecipeCacheVersion = recipeCacheVersion;
        observedAutomaticShapelessEnabled = automaticShapelessEnabled;
        observedAutomaticMixingEnabled = automaticMixingEnabled;
        observedAutomaticBrewingEnabled = automaticBrewingEnabled;
        kettle.scheduleUpdate();
    }

    @Internal
    public boolean updateReactorKettle() {
        observedRecipeCacheVersion = AirtightReactorKettleRecipeLookup.getRecipeCacheVersion();
        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        boolean isPonderLevel = level instanceof PonderLevel;
        if (isPonderLevel && !ponderProcessRequested) {
            return true;
        }

        float processingSpeed = getProcessingSpeed();
        if (level.isClientSide && !kettle.isVirtual() || operating || processingSpeed < SpeedLevel.FAST.getSpeedValue()) {
            return true;
        }

        ponderProcessRequested = false;
        Optional<ReactorKettleRecipe> reactorRecipe = AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle);
        if (reactorRecipe.isEmpty()) {
            reactorRecipe = AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle);
        }

        if (reactorRecipe.isEmpty()) {
            reactorRecipe = AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle);
        }

        if (reactorRecipe.isPresent()) {
            currentRecipe = reactorRecipe.get();
            currentCraftingRecipe = null;
            startOperation();
            return true;
        }

        if (!CCBConfig.server().machines.airtightReactorKettle.enableAutomaticShapelessRecipes.get()) {
            clearRecipes();
            return true;
        }

        Optional<RecipeHolder<CraftingRecipe>> craftingRecipeHolder = AirtightReactorKettleRecipeLookup.getMatchingCraftingRecipe(kettle);
        if (craftingRecipeHolder.isEmpty()) {
            clearRecipes();
            return true;
        }

        currentRecipe = null;
        currentCraftingRecipe = craftingRecipeHolder.get().value();
        startOperation();
        return true;
    }

    @Internal
    public boolean isOperating() {
        return operating;
    }

    void startProcessInPonderLevel() {
        update(false);
        ponderProcessRequested = true;
        updateReactorKettle();
    }

    void notifyContentsChanged() {
        contentsChanged = true;
    }

    void notifyFiltersChanged() {
        filterChanged = true;
    }

    boolean getWindowsOpenState() {
        return windowsOpenState;
    }

    int getOperatingTicks() {
        return operatingTicks;
    }

    int getProcessingTicks() {
        return processingTicks;
    }

    float getOperationKineticSpeed() {
        return operationKineticSpeed;
    }

    float getProcessingKineticSpeed() {
        return processingKineticSpeed;
    }

    float getPressureSpeedMultiplier() {
        return pressureSpeedMultiplier;
    }

    int getOperationCycle() {
        return operationCycle;
    }

    int getProcessingCycle() {
        return processingCycle;
    }

    float getAnimationKineticSpeed() {
        if (!operating) {
            return 0;
        }

        if (operatingTicks == PROCESSING_STARTED && processingKineticSpeed != 0) {
            return processingKineticSpeed;
        }

        return operationKineticSpeed;
    }

    float getDamage() {
        if (!operating) {
            return 0;
        }

        float absoluteSpeed = Mth.abs(getAnimationKineticSpeed());
        if (absoluteSpeed == 0) {
            return 0;
        }

        return absoluteSpeed / 32 * Math.max(0, CCBConfig.server().machines.airtightReactorKettle.mixerDamageMultiplier.getF());
    }

    float getMixerOffset(float partialTicks) {
        if (!operating) {
            return 0;
        }

        if (operatingTicks == PROCESSING_STARTED) {
            return 0.72F;
        }

        boolean isStarting = operatingTicks < PROCESSING_STARTED;
        int animationTick = isStarting ? operatingTicks : OPERATING_FINISHED - operatingTicks;
        float interpolatedTick = isStarting ? animationTick + partialTicks : animationTick - partialTicks;
        float mixerProgress = interpolatedTick / PROCESSING_STARTED;
        mixerProgress = (2 - Mth.cos(mixerProgress * Mth.PI)) / 2;
        return (mixerProgress - 0.5F) * 0.72F;
    }

    void loadOperationState(boolean operating, int operatingTicks, int processingTicks, boolean windowsOpenState, float operationKineticSpeed, float processingKineticSpeed, float pressureSpeedMultiplier, int operationCycle, int processingCycle, boolean clientPacket) {
        if (!clientPacket) {
            resetTransientOperation();
            return;
        }

        this.windowsOpenState = windowsOpenState;
        boolean sameRunningOperation = this.operating && operating && this.operationCycle == operationCycle;
        if (!sameRunningOperation) {
            this.operating = operating;
            this.operatingTicks = operatingTicks;
            this.processingTicks = processingTicks;
            this.operationKineticSpeed = sanitizeKineticSpeed(operationKineticSpeed);
            this.processingKineticSpeed = sanitizeKineticSpeed(processingKineticSpeed);
            this.pressureSpeedMultiplier = sanitizePressureSpeedMultiplier(pressureSpeedMultiplier);
            this.operationCycle = operationCycle;
            this.processingCycle = processingCycle;
            return;
        }

        if (this.processingCycle != processingCycle) {
            this.processingCycle = processingCycle;
            this.processingTicks = processingTicks;
            this.processingKineticSpeed = sanitizeKineticSpeed(processingKineticSpeed);
            this.pressureSpeedMultiplier = sanitizePressureSpeedMultiplier(pressureSpeedMultiplier);
        }

        if (!(this.operatingTicks <= PROCESSING_STARTED && operatingTicks > PROCESSING_STARTED)) {
            return;
        }

        this.operatingTicks = operatingTicks;
        this.processingTicks = processingTicks;
    }

    private void tickOperation() {
        Level level = kettle.getLevel();
        if (level == null) {
            return;
        }

        boolean isClientSide = level.isClientSide && !kettle.isVirtual();
        if (handleFilterChange(isClientSide)) {
            return;
        }

        updateWindowsOpenState();
        animationState.updateTargets(operating && operatingTicks <= PROCESSING_STARTED, operatingTicks, windowsOpenState);
        if (!operating) {
            return;
        }

        if (!isClientSide && currentRecipe instanceof ReactorKettleMixingRecipe mixingRecipe) {
            boolean enabled = mixingRecipe.isBrewing() ? CCBConfig.server().machines.airtightReactorKettle.enableAutomaticBrewingRecipes.get() : CCBConfig.server().machines.airtightReactorKettle.enableAutomaticMixingRecipes.get();
            if (!enabled || operationRecipeCacheVersion != AirtightReactorKettleRecipeLookup.getRecipeCacheVersion()) {
                update(true);
                return;
            }
        }

        if (operatingTicks >= OPERATING_FINISHED) {
            if (!isClientSide) {
                update(true);
            }
            return;
        }

        if (!isClientSide && currentRecipe == null && currentCraftingRecipe != null && !CCBConfig.server().machines.airtightReactorKettle.enableAutomaticShapelessRecipes.get()) {
            update(false);
            return;
        }

        if (operatingTicks != PROCESSING_STARTED) {
            operatingTicks++;
            return;
        }

        if (isClientSide) {
            return;
        }

        if (processingTicks < 0) {
            if (!hasRequiredSpeed()) {
                update(false);
                return;
            }

            startProcessing();
            return;
        }

        processingTicks--;
        if (processingTicks != 0) {
            return;
        }

        finishProcessing();
    }

    private boolean handleFilterChange(boolean isClientSide) {
        if (!filterChanged) {
            return false;
        }

        filterChanged = false;
        if (isClientSide) {
            return true;
        }

        update(true);
        return true;
    }

    private void updateWindowsOpenState() {
        Level level = kettle.getLevel();
        if (level == null || level.isClientSide && !kettle.isVirtual()) {
            return;
        }

        boolean shouldOpenWindows = shouldKeepWindowsOpen();
        if (shouldOpenWindows == windowsOpenState) {
            return;
        }

        windowsOpenState = shouldOpenWindows;
        kettle.sendData();
    }

    private boolean shouldKeepWindowsOpen() {
        boolean hasNoStoredGas = kettle.getInputGasTank().isEmpty() && kettle.getOutputGasTank().isEmpty();
        if (currentRecipe == null) {
            return hasNoStoredGas;
        }

        return hasNoStoredGas && currentRecipe.getGasIngredients().isEmpty() && currentRecipe.getGasResults().isEmpty();
    }

    private float getCurrentKineticSpeed() {
        if (kettle.getLevel() instanceof PonderLevel) {
            return SpeedLevel.FAST.getSpeedValue();
        }

        return kettle.getCore().getStructureManager().getSpeed();
    }

    private float getProcessingSpeed() {
        return Mth.abs(getCurrentKineticSpeed());
    }

    private boolean hasRequiredSpeed() {
        return getProcessingSpeed() >= SpeedLevel.FAST.getSpeedValue();
    }

    private void startOperation() {
        operationRecipeCacheVersion = AirtightReactorKettleRecipeLookup.getRecipeCacheVersion();
        operationKineticSpeed = sanitizeKineticSpeed(getCurrentKineticSpeed());
        processingKineticSpeed = 0;
        pressureSpeedMultiplier = 1;
        operationCycle++;
        operating = true;
        operatingTicks = 0;
        kettle.sendData();
    }

    private void startProcessing() {
        Level level = kettle.getLevel();
        processingKineticSpeed = sanitizeKineticSpeed(getCurrentKineticSpeed());
        float processingSpeed = Mth.abs(processingKineticSpeed);
        pressureSpeedMultiplier = currentRecipe == null || level instanceof PonderLevel ? 1.0F : new ReactorKettleCraftPlanner(kettle, currentRecipe).getPressureSpeedMultiplier();
        pressureSpeedMultiplier = sanitizePressureSpeedMultiplier(pressureSpeedMultiplier);
        processingCycle++;

        if (currentRecipe == null) {
            processingTicks = 1;
        }
        else {
            int recipeDuration = currentRecipe.getProcessingDuration();
            if (level instanceof PonderLevel) {
                processingTicks = Mth.clamp(recipeDuration, 1, PONDER_PROCESSING_DURATION);
            }
            else {
                float minimumSpeed = SpeedLevel.FAST.getSpeedValue();
                float kineticSpeedMultiplier = Math.max(1, processingSpeed / minimumSpeed);
                float totalSpeedMultiplier = kineticSpeedMultiplier * pressureSpeedMultiplier;
                processingTicks = recipeDuration <= 0 ? 1 : Mth.clamp(Mth.ceil(recipeDuration / totalSpeedMultiplier), 1, 1000000);
            }
        }
        kettle.sendData();
        if (level == null || kettle.getInputFluidTank().isEmpty() && kettle.getOutputFluidTank().isEmpty()) {
            return;
        }

        level.playSound(null, kettle.getBlockPos(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, 0.75F, processingSpeed < 64 ? 0.75F : 1.5F);
    }

    private void finishProcessing() {
        operatingTicks++;
        processingTicks = -1;
        Level level = kettle.getLevel();
        if (level == null || level.isClientSide && !kettle.isVirtual()) {
            return;
        }

        if (!applyCurrentRecipe()) {
            update(false);
            return;
        }

        kettle.getInputFluidTank().sendDataImmediately();
        kettle.getInputGasTank().sendDataImmediately();
        contentsChanged = true;
        if (!(level instanceof PonderLevel) && canContinueProcessing()) {
            operatingTicks = PROCESSING_STARTED;
        }
        kettle.sendData();
    }

    private boolean applyCurrentRecipe() {
        if (currentRecipe != null) {
            return AirtightReactorKettleCrafting.applyRecipe(kettle, currentRecipe);
        }

        return currentCraftingRecipe != null && CCBConfig.server().machines.airtightReactorKettle.enableAutomaticShapelessRecipes.get() && AirtightReactorKettleCrafting.applyCraftingRecipe(kettle, currentCraftingRecipe);
    }

    private boolean canContinueProcessing() {
        if (currentRecipe != null) {
            return new ReactorKettleCraftPlanner(kettle, currentRecipe).matches();
        }

        return currentCraftingRecipe != null && CCBConfig.server().machines.airtightReactorKettle.enableAutomaticShapelessRecipes.get() && AirtightReactorKettleMixingPlanner.matches(kettle, currentCraftingRecipe);
    }

    private void update(boolean scheduleUpdate) {
        resetTransientOperation();
        kettle.sendData();
        Level level = kettle.getLevel();
        if (!scheduleUpdate || level == null || level.isClientSide && !kettle.isVirtual()) {
            return;
        }

        kettle.scheduleUpdate();
    }

    private void resetTransientOperation() {
        operating = false;
        operatingTicks = 0;
        processingTicks = -1;
        operationKineticSpeed = 0;
        processingKineticSpeed = 0;
        pressureSpeedMultiplier = 1;
        clearRecipes();
    }

    private void clearRecipes() {
        currentRecipe = null;
        currentCraftingRecipe = null;
    }
}
