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
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleController {
    public static final int PROCESSING_STARTED = 20;
    private static final int OPERATING_FINISHED = 40;
    private final AirtightReactorKettleBlockEntity kettle;
    private final AirtightReactorKettleAnimationState animationState;

    private boolean observedAutomaticMixingEnabled;
    private boolean contentsChanged = true;
    private boolean filterChanged;
    private boolean operating;
    private boolean windowsOpenState = true;
    private int operatingTicks;
    private int processingTicks = -1;
    private CraftingRecipe currentCraftingRecipe;
    private ReactorKettleRecipe currentRecipe;
    private long observedRecipeCacheVersion;

    public AirtightReactorKettleController(AirtightReactorKettleBlockEntity kettle, AirtightReactorKettleAnimationState animationState) {
        this.kettle = kettle;
        this.animationState = animationState;
        observedAutomaticMixingEnabled = CCBConfig.server().airtights.enableAutomaticMixingRecipes.get();
        observedRecipeCacheVersion = AirtightReactorKettleUtils.getRecipeCacheVersion();
    }

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

    public void lazyTick() {
        Level level = kettle.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long recipeCacheVersion = AirtightReactorKettleUtils.getRecipeCacheVersion();
        boolean automaticMixingEnabled = CCBConfig.server().airtights.enableAutomaticMixingRecipes.get();
        if (observedRecipeCacheVersion == recipeCacheVersion && observedAutomaticMixingEnabled == automaticMixingEnabled) {
            return;
        }

        observedRecipeCacheVersion = recipeCacheVersion;
        observedAutomaticMixingEnabled = automaticMixingEnabled;
        kettle.scheduleUpdate();
    }

    public boolean updateReactorKettle() {
        observedRecipeCacheVersion = AirtightReactorKettleUtils.getRecipeCacheVersion();
        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        float processingSpeed = getProcessingSpeed();
        if (level.isClientSide && !kettle.isVirtual() || operating || processingSpeed < SpeedLevel.FAST.getSpeedValue()) {
            return true;
        }

        Optional<ReactorKettleRecipe> reactorRecipe = AirtightReactorKettleUtils.getMatchingRecipe(kettle);
        if (reactorRecipe.isPresent()) {
            currentRecipe = reactorRecipe.get();
            currentCraftingRecipe = null;
            startOperation();
            return true;
        }

        if (!CCBConfig.server().airtights.enableAutomaticMixingRecipes.get()) {
            clearRecipes();
            return true;
        }

        Optional<RecipeHolder<CraftingRecipe>> craftingRecipeHolder = AirtightReactorKettleUtils.getMatchingCraftingRecipe(kettle);
        if (craftingRecipeHolder.isEmpty()) {
            clearRecipes();
            return true;
        }

        currentRecipe = null;
        currentCraftingRecipe = craftingRecipeHolder.get().value();
        startOperation();
        return true;
    }

    public void startProcessInPonderLevel() {
        update(false);
        updateReactorKettle();
    }

    public void notifyContentsChanged() {
        contentsChanged = true;
    }

    public void notifyFiltersChanged() {
        filterChanged = true;
    }

    public boolean getWindowsOpenState() {
        return windowsOpenState;
    }

    public boolean isOperating() {
        return operating;
    }

    public int getOperatingTicks() {
        return operatingTicks;
    }

    public int getProcessingTicks() {
        return processingTicks;
    }

    public float getDamage() {
        if (!operating) {
            return 0;
        }

        float absoluteSpeed = Mth.abs(kettle.getCore().getStructureManager().getSpeed());
        if (absoluteSpeed == 0) {
            return 0;
        }
        return absoluteSpeed / 32 * Math.max(0, CCBConfig.server().airtights.reactorKettleMixerDamageMultiplier.getF());
    }

    public float getMixerOffset(float partialTicks) {
        if (!operating) {
            return 0;
        }

        if (operatingTicks == PROCESSING_STARTED) {
            return 0.72f;
        }

        boolean isStarting = operatingTicks < PROCESSING_STARTED;
        int animationTick = isStarting ? operatingTicks : OPERATING_FINISHED - operatingTicks;
        float interpolatedTick = isStarting ? animationTick + partialTicks : animationTick - partialTicks;
        float mixerProgress = interpolatedTick / PROCESSING_STARTED;
        mixerProgress = (2 - Mth.cos(mixerProgress * Mth.PI)) / 2;
        return (mixerProgress - 0.5f) * 0.72f;
    }

    public void loadOperationState(boolean operating, int operatingTicks, int processingTicks, boolean windowsOpenState, boolean clientPacket) {
        if (!clientPacket) {
            resetTransientOperation();
            return;
        }

        this.operating = operating;
        this.operatingTicks = operatingTicks;
        this.processingTicks = processingTicks;
        this.windowsOpenState = windowsOpenState;
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

        if (operatingTicks >= OPERATING_FINISHED) {
            if (!isClientSide) {
                update(true);
            }
            return;
        }

        if (!isClientSide && !hasRequiredSpeed()) {
            update(false);
            return;
        }

        if (!isClientSide && currentRecipe == null && currentCraftingRecipe != null && !CCBConfig.server().airtights.enableAutomaticMixingRecipes.get()) {
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
        if (level == null || level.isClientSide) {
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

    private float getProcessingSpeed() {
        float kineticSpeed = Mth.abs(kettle.getCore().getStructureManager().getSpeed());
        if (kettle.getLevel() instanceof PonderLevel) {
            return SpeedLevel.FAST.getSpeedValue();
        }
        return kineticSpeed;
    }

    private boolean hasRequiredSpeed() {
        float processingSpeed = kettle.getLevel() instanceof PonderLevel ? SpeedLevel.FAST.getSpeedValue() : Mth.abs(kettle.getCore().getStructureManager().getSpeed());
        return processingSpeed >= SpeedLevel.FAST.getSpeedValue();
    }

    private void startOperation() {
        operating = true;
        operatingTicks = 0;
        kettle.sendData();
    }

    private void startProcessing() {
        float processingSpeed = getProcessingSpeed();
        if (currentRecipe == null) {
            processingTicks = 1;
        }
        else {
            int recipeDuration = currentRecipe.getProcessingDuration();
            float minimumSpeed = SpeedLevel.FAST.getSpeedValue();
            float speedMultiplier = Math.max(1.0f, processingSpeed / minimumSpeed);
            processingTicks = recipeDuration <= 0 ? 1 : Mth.clamp(Mth.ceil(recipeDuration / speedMultiplier), 1, 1_000_000);
        }

        Level level = kettle.getLevel();
        if (level == null || kettle.getInputFluidTank().isEmpty() && kettle.getOutputFluidTank().isEmpty()) {
            return;
        }

        level.playSound(null, kettle.getBlockPos(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, 0.75f, processingSpeed < 64 ? 0.75f : 1.5f);
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
        if (canContinueProcessing()) {
            operatingTicks = PROCESSING_STARTED;
        }
        kettle.sendData();
    }

    private boolean applyCurrentRecipe() {
        if (currentRecipe != null) {
            return ReactorKettleRecipe.apply(kettle, currentRecipe);
        }
        return currentCraftingRecipe != null && CCBConfig.server().airtights.enableAutomaticMixingRecipes.get() && AirtightReactorKettleUtils.applyCraftingRecipe(kettle, currentCraftingRecipe);
    }

    private boolean canContinueProcessing() {
        if (currentRecipe != null) {
            return ReactorKettleRecipe.match(kettle, currentRecipe);
        }
        return currentCraftingRecipe != null && CCBConfig.server().airtights.enableAutomaticMixingRecipes.get() && AirtightReactorKettleUtils.matchCraftingRecipe(kettle, currentCraftingRecipe);
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
        clearRecipes();
    }

    private void clearRecipes() {
        currentRecipe = null;
        currentCraftingRecipe = null;
    }
}
