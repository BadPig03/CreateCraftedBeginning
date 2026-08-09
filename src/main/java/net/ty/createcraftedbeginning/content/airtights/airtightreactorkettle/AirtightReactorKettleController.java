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
final class AirtightReactorKettleController {
    static final int OPERATING_FINISHED = 40;
    static final int PROCESSING_STARTED = 20;

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

    AirtightReactorKettleController(AirtightReactorKettleBlockEntity kettle, AirtightReactorKettleAnimationState animationState) {
        this.kettle = kettle;
        this.animationState = animationState;
        observedAutomaticMixingEnabled = CCBConfig.server().airtights.enableAutomaticMixingRecipes.get();
        observedRecipeCacheVersion = AirtightReactorKettleUtils.getRecipeCacheVersion();
    }

    void tick() {
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

    void lazyTick() {
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

    boolean updateReactorKettle() {
        observedRecipeCacheVersion = AirtightReactorKettleUtils.getRecipeCacheVersion();
        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        float speed = getProcessingSpeed();
        if (level.isClientSide && !kettle.isVirtual() || operating || speed < SpeedLevel.FAST.getSpeedValue()) {
            return true;
        }

        Optional<ReactorKettleRecipe> recipe = AirtightReactorKettleUtils.getMatchingRecipe(kettle);
        if (recipe.isPresent()) {
            currentRecipe = recipe.get();
            currentCraftingRecipe = null;
            startOperation();
            return true;
        }

        if (!CCBConfig.server().airtights.enableAutomaticMixingRecipes.get()) {
            clearRecipes();
            return true;
        }

        Optional<RecipeHolder<CraftingRecipe>> craftingRecipe = AirtightReactorKettleUtils.getMatchingCraftingRecipe(kettle);
        if (craftingRecipe.isEmpty()) {
            clearRecipes();
            return true;
        }

        currentRecipe = null;
        currentCraftingRecipe = craftingRecipe.get().value();
        startOperation();
        return true;
    }

    void startProcessInPonderLevel() {
        update(false);
        updateReactorKettle();
    }

    void notifyContentsChanged() {
        contentsChanged = true;
    }

    void notifyFiltersChanged() {
        filterChanged = true;
    }

    boolean isFilterChanged() {
        return filterChanged;
    }

    boolean getWindowsOpenState() {
        return windowsOpenState;
    }

    boolean isOperating() {
        return operating;
    }

    int getOperatingTicks() {
        return operatingTicks;
    }

    int getProcessingTicks() {
        return processingTicks;
    }

    float getDamage() {
        if (!operating) {
            return 0;
        }

        float absSpeed = Mth.abs(kettle.getCore().getStructureManager().getSpeed());
        if (absSpeed == 0) {
            return 0;
        }
        return absSpeed / 32 * Math.max(0, CCBConfig.server().airtights.reactorKettleMixerDamageMultiplier.getF());
    }

    float getMixerOffset(float partialTicks) {
        if (!operating) {
            return 0;
        }

        if (operatingTicks == PROCESSING_STARTED) {
            return 0.72f;
        }

        boolean starting = operatingTicks < PROCESSING_STARTED;
        int localTick = starting ? operatingTicks : OPERATING_FINISHED - operatingTicks;
        float adjustedTick = starting ? localTick + partialTicks : localTick - partialTicks;
        float progress = adjustedTick / PROCESSING_STARTED;
        progress = (2 - Mth.cos(progress * Mth.PI)) / 2;
        return (progress - 0.5f) * 0.72f;
    }

    void loadOperationState(boolean operating, int operatingTicks, int processingTicks, boolean windowsOpenState, boolean clientPacket) {
        this.operating = operating;
        this.operatingTicks = operatingTicks;
        this.processingTicks = processingTicks;
        this.windowsOpenState = windowsOpenState;
        if (clientPacket) {
            return;
        }

        resetTransientOperation();
    }

    private void tickOperation() {
        Level level = kettle.getLevel();
        if (level == null) {
            return;
        }

        boolean clientSide = level.isClientSide && !kettle.isVirtual();
        if (handleFilterChange(clientSide)) {
            return;
        }

        updateWindowsOpenState();
        animationState.updateTargets(operating && operatingTicks <= PROCESSING_STARTED, operatingTicks, windowsOpenState);
        if (!operating) {
            return;
        }

        if (operatingTicks >= OPERATING_FINISHED) {
            if (!clientSide) {
                update(true);
            }
            return;
        }

        if (!clientSide && !hasRequiredSpeed()) {
            update(false);
            return;
        }

        if (!clientSide && currentRecipe == null && currentCraftingRecipe != null && !CCBConfig.server().airtights.enableAutomaticMixingRecipes.get()) {
            update(false);
            return;
        }

        if (operatingTicks != PROCESSING_STARTED) {
            operatingTicks++;
            return;
        }

        if (clientSide) {
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

    private boolean handleFilterChange(boolean clientSide) {
        if (!filterChanged) {
            return false;
        }

        filterChanged = false;
        if (clientSide) {
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

        boolean shouldOpen = shouldKeepWindowsOpen();
        if (shouldOpen == windowsOpenState) {
            return;
        }

        windowsOpenState = shouldOpen;
        kettle.sendData();
    }

    private boolean shouldKeepWindowsOpen() {
        boolean hasNoGas = kettle.getInputGasTank().isEmpty() && kettle.getOutputGasTank().isEmpty();
        if (currentRecipe == null) {
            return hasNoGas;
        }
        return hasNoGas && currentRecipe.getGasIngredients().isEmpty() && currentRecipe.getGasResults().isEmpty();
    }

    private float getProcessingSpeed() {
        float speed = Mth.abs(kettle.getCore().getStructureManager().getSpeed());
        if (kettle.getLevel() instanceof PonderLevel) {
            return SpeedLevel.FAST.getSpeedValue();
        }
        return speed;
    }

    private boolean hasRequiredSpeed() {
        float speed = kettle.getLevel() instanceof PonderLevel ? SpeedLevel.FAST.getSpeedValue() : Mth.abs(kettle.getCore().getStructureManager().getSpeed());
        return speed >= SpeedLevel.FAST.getSpeedValue();
    }

    private void startOperation() {
        operating = true;
        operatingTicks = 0;
        kettle.sendData();
    }

    private void startProcessing() {
        float recipeSpeed = currentRecipe == null ? 0 : currentRecipe.getProcessingDuration() / 100.0f;
        float speed = getProcessingSpeed();
        int baseProcessingTicks = Mth.clamp(Mth.log2((int) (256 / speed)) * Mth.ceil(recipeSpeed * 15) + 1, 1, 1000);
        processingTicks = Mth.clamp(Mth.ceil(baseProcessingTicks), 1, 1_000_000);
        Level level = kettle.getLevel();
        if (level == null || kettle.getInputFluidTank().isEmpty() && kettle.getOutputFluidTank().isEmpty()) {
            return;
        }

        level.playSound(null, kettle.getBlockPos(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, 0.75f, speed < 64 ? 0.75f : 1.5f);
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

    private void update(boolean schedule) {
        resetTransientOperation();
        kettle.sendData();
        Level level = kettle.getLevel();
        if (!schedule || level == null || level.isClientSide && !kettle.isVirtual()) {
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
