package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressController {
    private static final int CYCLE_DURATION = 30;
    @SuppressWarnings("ConstantExpression")
    private static final float PRESS_HEAD_IDLE_OFFSET = -0.625F;
    private static final float PRESS_HEAD_TRAVEL = 0.8125F;

    private final AirtightForgingPressBlockEntity press;

    private boolean observedAutomaticPressingEnabled;
    private boolean observedAutomaticCrushingEnabled;
    private boolean observedAutomaticSmithingEnabled;
    private boolean contentsChanged = true;
    private boolean filterChanged;
    private boolean ponderProcessRequested;
    private boolean operating;
    private @Nullable ForgingPressRecipe currentRecipe;
    private @Nullable AirtightForgingPressPressingRecipe currentPressingRecipe;
    private @Nullable RecipeHolder<CrushingRecipe> currentCrushingRecipe;
    private @Nullable SmithingRecipe currentSmithingRecipe;
    private float operatingTicks;
    private float pressureSpeedMultiplier = 1;
    private float operationSpeed;
    private int operationCycle;
    private long observedRecipeCacheVersion;

    @Internal
    public AirtightForgingPressController(AirtightForgingPressBlockEntity press) {
        this.press = press;
        observedAutomaticPressingEnabled = CCBConfig.server().machines.airtightForgingPress.enableAutomaticPressingRecipes.get();
        observedAutomaticCrushingEnabled = CCBConfig.server().machines.airtightForgingPress.enableAutomaticCrushingRecipes.get();
        observedAutomaticSmithingEnabled = CCBConfig.server().machines.airtightForgingPress.enableAutomaticSmithingRecipes.get();
        observedRecipeCacheVersion = AirtightForgingPressRecipeLookup.getRecipeCacheVersion();
    }

    private static float sanitizePressureSpeedMultiplier(float multiplier) {
        if (Float.isFinite(multiplier) && multiplier >= 1.0F) {
            return multiplier;
        }

        return 1;
    }

    private static float sanitizeOperationSpeed(float speed) {
        if (!Float.isFinite(speed) || speed <= 0) {
            return 0;
        }

        return Mth.clamp(speed, 1, 16);
    }

    @Internal
    public void tick() {
        Level level = press.getLevel();
        if (level == null) {
            return;
        }

        tickOperation();
        if (!contentsChanged) {
            return;
        }

        contentsChanged = false;
        press.scheduleUpdate();
    }

    @Internal
    public void lazyTick() {
        Level level = press.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long recipeCacheVersion = AirtightForgingPressRecipeLookup.getRecipeCacheVersion();
        boolean automaticPressingEnabled = CCBConfig.server().machines.airtightForgingPress.enableAutomaticPressingRecipes.get();
        boolean automaticCrushingEnabled = CCBConfig.server().machines.airtightForgingPress.enableAutomaticCrushingRecipes.get();
        boolean automaticSmithingEnabled = CCBConfig.server().machines.airtightForgingPress.enableAutomaticSmithingRecipes.get();
        if (observedRecipeCacheVersion == recipeCacheVersion && observedAutomaticPressingEnabled == automaticPressingEnabled && observedAutomaticCrushingEnabled == automaticCrushingEnabled && observedAutomaticSmithingEnabled == automaticSmithingEnabled) {
            return;
        }

        observedRecipeCacheVersion = recipeCacheVersion;
        observedAutomaticPressingEnabled = automaticPressingEnabled;
        observedAutomaticCrushingEnabled = automaticCrushingEnabled;
        observedAutomaticSmithingEnabled = automaticSmithingEnabled;
        update(true);
    }

    @Internal
    public boolean updateForgingPress() {
        observedRecipeCacheVersion = AirtightForgingPressRecipeLookup.getRecipeCacheVersion();
        Level level = press.getLevel();
        if (level == null) {
            return false;
        }

        boolean isPonderLevel = level instanceof PonderLevel;
        if (isPonderLevel && !ponderProcessRequested) {
            return true;
        }

        boolean isInactiveClient = level.isClientSide && !press.isVirtual();
        if (isInactiveClient || operating || getAvailableKineticOperationSpeed() <= 0) {
            return true;
        }

        ponderProcessRequested = false;
        Optional<ForgingPressRecipe> forgingRecipe = AirtightForgingPressRecipeLookup.getMatchingRecipe(press);
        if (forgingRecipe.isPresent()) {
            currentRecipe = forgingRecipe.get();
            currentPressingRecipe = null;
            currentCrushingRecipe = null;
            currentSmithingRecipe = null;
            startOperation();
            return true;
        }

        if (CCBConfig.server().machines.airtightForgingPress.enableAutomaticPressingRecipes.get()) {
            Optional<AirtightForgingPressPressingRecipe> pressingRecipe = AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press);
            if (pressingRecipe.isPresent()) {
                currentRecipe = null;
                currentPressingRecipe = pressingRecipe.get();
                currentCrushingRecipe = null;
                currentSmithingRecipe = null;
                startOperation();
                return true;
            }
        }

        if (CCBConfig.server().machines.airtightForgingPress.enableAutomaticCrushingRecipes.get()) {
            Optional<RecipeHolder<CrushingRecipe>> crushingRecipe = AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press);
            if (crushingRecipe.isPresent()) {
                currentRecipe = null;
                currentPressingRecipe = null;
                currentCrushingRecipe = crushingRecipe.get();
                currentSmithingRecipe = null;
                startOperation();
                return true;
            }
        }

        if (CCBConfig.server().machines.airtightForgingPress.enableAutomaticSmithingRecipes.get()) {
            Optional<RecipeHolder<SmithingRecipe>> smithingRecipe = AirtightForgingPressRecipeLookup.getMatchingSmithingRecipe(press);
            if (smithingRecipe.isPresent()) {
                currentRecipe = null;
                currentPressingRecipe = null;
                currentCrushingRecipe = null;
                currentSmithingRecipe = smithingRecipe.get().value();
                startOperation();
                return true;
            }
        }

        clearRecipes();
        return true;
    }

    @Internal
    public boolean isOperating() {
        return operating;
    }

    void startProcessInPonderLevel() {
        update(false);
        ponderProcessRequested = true;
        updateForgingPress();
    }

    void notifyContentsChanged() {
        contentsChanged = true;
    }

    void notifyFilterChanged() {
        filterChanged = true;
        contentsChanged = true;
    }

    float getPressHeadDistance(float partialTicks) {
        if (!operating) {
            return PRESS_HEAD_IDLE_OFFSET;
        }

        float cycleTicks = Mth.clamp(operatingTicks + partialTicks * operationSpeed, 0.0F, CYCLE_DURATION);
        float distance;
        if (cycleTicks < 20) {
            float progress = cycleTicks / CYCLE_DURATION * 2;
            distance = Mth.clamp(Mth.square(progress) * progress, 0.0F, 1.0F);
        }
        else {
            distance = Mth.clamp((CYCLE_DURATION - cycleTicks) / CYCLE_DURATION * 3, 0.0F, 1.0F);
        }
        return PRESS_HEAD_IDLE_OFFSET + distance * PRESS_HEAD_TRAVEL;
    }

    float getOperatingTicks() {
        return operatingTicks;
    }

    float getPressureSpeedMultiplier() {
        return pressureSpeedMultiplier;
    }

    float getOperationSpeed() {
        return operationSpeed;
    }

    int getOperationCycle() {
        return operationCycle;
    }

    void loadOperationState(boolean operating, float operatingTicks, float pressureSpeedMultiplier, float operationSpeed, int operationCycle, boolean clientPacket) {
        if (!clientPacket) {
            resetTransientOperation();
            return;
        }

        boolean sameRunningCycle = this.operating && operating && this.operationCycle == operationCycle;
        if (sameRunningCycle) {
            return;
        }

        this.operating = operating;
        this.operatingTicks = operatingTicks;
        this.pressureSpeedMultiplier = sanitizePressureSpeedMultiplier(pressureSpeedMultiplier);
        this.operationSpeed = sanitizeOperationSpeed(operationSpeed);
        this.operationCycle = operationCycle;
    }

    private void tickOperation() {
        if (filterChanged) {
            filterChanged = false;
            update(true);
            return;
        }

        if (!operating) {
            return;
        }

        if (operatingTicks >= CYCLE_DURATION) {
            update(true);
            return;
        }

        float operationSpeed = this.operationSpeed;

        if (currentRecipe == null && currentPressingRecipe != null && !CCBConfig.server().machines.airtightForgingPress.enableAutomaticPressingRecipes.get()) {
            update(false);
            return;
        }

        if (currentRecipe == null && currentCrushingRecipe != null && !CCBConfig.server().machines.airtightForgingPress.enableAutomaticCrushingRecipes.get()) {
            update(false);
            return;
        }

        if (currentRecipe == null && currentSmithingRecipe != null && !CCBConfig.server().machines.airtightForgingPress.enableAutomaticSmithingRecipes.get()) {
            update(false);
            return;
        }

        float previousOperatingTicks = operatingTicks;
        operatingTicks = Mth.clamp(operatingTicks + operationSpeed, 0.0F, CYCLE_DURATION);
        Level level = press.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        float processingStartTick = CYCLE_DURATION / 2.0F;
        boolean wasAlreadyProcessing = previousOperatingTicks >= processingStartTick;
        boolean hasNotReachedProcessing = operatingTicks < processingStartTick;
        boolean hasNoRecipe = currentRecipe == null && currentPressingRecipe == null && currentCrushingRecipe == null && currentSmithingRecipe == null;
        if (wasAlreadyProcessing || hasNotReachedProcessing || hasNoRecipe) {
            return;
        }

        ItemStack particleStack = press.getInputInventory().getStackInSlot(0).copy();
        boolean craftSucceeded;
        if (currentRecipe != null) {
            if (particleStack.isEmpty()) {
                particleStack = currentRecipe.getResultItem(level.registryAccess()).copy();
            }
            craftSucceeded = AirtightForgingPressCrafting.applyRecipe(press, currentRecipe);
        }
        else if (currentPressingRecipe != null) {
            craftSucceeded = AirtightForgingPressCrafting.applyPressingRecipe(press, currentPressingRecipe);
        }
        else if (currentCrushingRecipe != null) {
            craftSucceeded = AirtightForgingPressCrushing.prepare(press, currentCrushingRecipe).map(plan -> press.commitCraft(plan.consumption(), plan.output())).orElse(false);
        }
        else {
            SmithingRecipe smithingRecipe = currentSmithingRecipe;
            if (smithingRecipe == null) {
                return;
            }

            SmithingRecipeInput smithingInput = AirtightForgingPressAutomationPlanner.createSmithingInput(press);
            ItemStack smithingResult = smithingRecipe.assemble(smithingInput, level.registryAccess());
            if (!smithingResult.isEmpty()) {
                particleStack = smithingResult.copy();
            }
            craftSucceeded = AirtightForgingPressCrafting.applySmithingRecipe(press, smithingRecipe);
        }
        if (!craftSucceeded) {
            return;
        }

        press.getFluidTankBehaviour().sendDataImmediately();
        press.getGasTankBehaviour().sendDataImmediately();
        CCBSoundEvents.FORGING_PRESS_PRESSED.playOnServer(level, press.getBlockPos());
        spawnParticles(particleStack);
        contentsChanged = true;
        press.sendData();
    }

    private float getAvailableKineticOperationSpeed() {
        Level level = press.getLevel();
        if (level instanceof PonderLevel) {
            return 1;
        }

        float absoluteSpeed = Mth.abs(press.getCore().getStructureManager().getRealSpeed());
        float minimumSpeed = SpeedLevel.FAST.getSpeedValue();
        if (absoluteSpeed < minimumSpeed) {
            return 0;
        }

        return Mth.clamp(absoluteSpeed / minimumSpeed, 1, 16);
    }

    private float getStartingOperationSpeed(float startingSpeedMultiplier) {
        float kineticOperationSpeed = getAvailableKineticOperationSpeed();
        if (kineticOperationSpeed <= 0) {
            return 0;
        }

        return Mth.clamp(kineticOperationSpeed * sanitizePressureSpeedMultiplier(startingSpeedMultiplier), 1, 16);
    }

    private void startOperation() {
        pressureSpeedMultiplier = getStartingPressureSpeedMultiplier();
        operationSpeed = getStartingOperationSpeed(pressureSpeedMultiplier);
        operationCycle++;
        operating = true;
        operatingTicks = 0;
        press.sendData();
    }

    private float getStartingPressureSpeedMultiplier() {
        if (press.getLevel() instanceof PonderLevel || currentRecipe == null) {
            return 1;
        }

        return new ForgingPressCraftPlanner(press, currentRecipe).getPressureSpeedMultiplier();
    }

    private void update(boolean scheduleUpdate) {
        resetTransientOperation();
        press.sendData();
        Level level = press.getLevel();
        if (!scheduleUpdate || level == null || level.isClientSide && !press.isVirtual()) {
            return;
        }

        press.scheduleUpdate();
    }

    private void resetTransientOperation() {
        operating = false;
        operatingTicks = 0;
        pressureSpeedMultiplier = 1;
        operationSpeed = 0;
        clearRecipes();
    }

    private void clearRecipes() {
        currentRecipe = null;
        currentPressingRecipe = null;
        currentCrushingRecipe = null;
        currentSmithingRecipe = null;
    }

    private void spawnParticles(ItemStack particleStack) {
        Level level = press.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || press.isVirtual() || particleStack.isEmpty()) {
            return;
        }

        Vec3 particlePosition = VecHelper.getCenterOf(press.getBlockPos()).add(0, -0.625, 0);
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleStack), particlePosition.x, particlePosition.y, particlePosition.z, 16, 0.15, 0.05, 0.15, 0.08);
    }
}
