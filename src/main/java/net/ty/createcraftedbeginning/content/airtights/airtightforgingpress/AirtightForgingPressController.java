package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
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
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightForgingPressController {
    private static final int CYCLE_DURATION = 30;
    private static final float PRESS_HEAD_IDLE_OFFSET = -0.625f;
    private static final float PRESS_HEAD_TRAVEL = 0.8125f;

    private final AirtightForgingPressBlockEntity press;

    private boolean observedAutomaticPressingEnabled;
    private boolean observedAutomaticSmithingEnabled;
    private boolean contentsChanged = true;
    private boolean filterChanged;
    private boolean operating;
    private ForgingPressRecipe currentRecipe;
    private PressingRecipe currentPressingRecipe;
    private SmithingRecipe currentSmithingRecipe;
    private float operatingTicks;
    private long observedRecipeCacheVersion;

    AirtightForgingPressController(AirtightForgingPressBlockEntity press) {
        this.press = press;
        observedAutomaticPressingEnabled = CCBConfig.server().airtights.enableAutomaticPressingRecipes.get();
        observedAutomaticSmithingEnabled = CCBConfig.server().airtights.enableAutomaticSmithingRecipes.get();
        observedRecipeCacheVersion = AirtightForgingPressUtils.getRecipeCacheVersion();
    }

    void tick() {
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

    void lazyTick() {
        Level level = press.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long recipeCacheVersion = AirtightForgingPressUtils.getRecipeCacheVersion();
        boolean automaticPressingEnabled = CCBConfig.server().airtights.enableAutomaticPressingRecipes.get();
        boolean automaticSmithingEnabled = CCBConfig.server().airtights.enableAutomaticSmithingRecipes.get();
        if (observedRecipeCacheVersion == recipeCacheVersion && observedAutomaticPressingEnabled == automaticPressingEnabled && observedAutomaticSmithingEnabled == automaticSmithingEnabled) {
            return;
        }

        observedRecipeCacheVersion = recipeCacheVersion;
        observedAutomaticPressingEnabled = automaticPressingEnabled;
        observedAutomaticSmithingEnabled = automaticSmithingEnabled;
        update(true);
    }

    boolean updateForgingPress() {
        observedRecipeCacheVersion = AirtightForgingPressUtils.getRecipeCacheVersion();
        Level level = press.getLevel();
        if (level == null) {
            return false;
        }

        boolean isInactiveClient = level.isClientSide && !press.isVirtual();
        if (isInactiveClient || operating || getOperationSpeed() <= 0) {
            return true;
        }

        Optional<ForgingPressRecipe> forgingRecipe = AirtightForgingPressUtils.getMatchingRecipe(press);
        if (forgingRecipe.isPresent()) {
            currentRecipe = forgingRecipe.get();
            currentPressingRecipe = null;
            currentSmithingRecipe = null;
            startOperation();
            return true;
        }

        if (CCBConfig.server().airtights.enableAutomaticPressingRecipes.get()) {
            Optional<RecipeHolder<PressingRecipe>> pressingRecipe = AirtightForgingPressUtils.getMatchingPressingRecipe(press);
            if (pressingRecipe.isPresent()) {
                currentRecipe = null;
                currentPressingRecipe = pressingRecipe.get().value();
                currentSmithingRecipe = null;
                startOperation();
                return true;
            }
        }

        if (CCBConfig.server().airtights.enableAutomaticSmithingRecipes.get()) {
            Optional<RecipeHolder<SmithingRecipe>> smithingRecipe = AirtightForgingPressUtils.getMatchingSmithingRecipe(press);
            if (smithingRecipe.isPresent()) {
                currentRecipe = null;
                currentPressingRecipe = null;
                currentSmithingRecipe = smithingRecipe.get().value();
                startOperation();
                return true;
            }
        }

        clearRecipes();
        return true;
    }

    void startProcessInPonderLevel() {
        update(false);
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

        float cycleTicks = Mth.clamp(operatingTicks + partialTicks * getOperationSpeed(), 0, CYCLE_DURATION);
        float distance;
        if (cycleTicks < 20) {
            float progress = cycleTicks / CYCLE_DURATION * 2;
            distance = Mth.clamp(Mth.square(progress) * progress, 0, 1);
        }
        else {
            distance = Mth.clamp((CYCLE_DURATION - cycleTicks) / CYCLE_DURATION * 3, 0, 1);
        }
        return PRESS_HEAD_IDLE_OFFSET + distance * PRESS_HEAD_TRAVEL;
    }

    boolean isOperating() {
        return operating;
    }

    float getOperatingTicks() {
        return operatingTicks;
    }

    void loadOperationState(boolean operating, float operatingTicks, boolean clientPacket) {
        this.operating = operating;
        this.operatingTicks = operatingTicks;
        if (clientPacket) {
            return;
        }

        resetTransientOperation();
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

        float operationSpeed = getOperationSpeed();
        if (operationSpeed <= 0) {
            update(false);
            return;
        }

        if (currentRecipe == null && currentPressingRecipe != null && !CCBConfig.server().airtights.enableAutomaticPressingRecipes.get()) {
            update(false);
            return;
        }

        if (currentRecipe == null && currentSmithingRecipe != null && !CCBConfig.server().airtights.enableAutomaticSmithingRecipes.get()) {
            update(false);
            return;
        }

        float previousOperatingTicks = operatingTicks;
        operatingTicks = Mth.clamp(operatingTicks + operationSpeed, 0, CYCLE_DURATION);
        Level level = press.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        float processingStartTick = CYCLE_DURATION / 2.0f;
        boolean wasAlreadyProcessing = previousOperatingTicks >= processingStartTick;
        boolean hasNotReachedProcessing = operatingTicks < processingStartTick;
        boolean hasNoRecipe = currentRecipe == null && currentPressingRecipe == null && currentSmithingRecipe == null;
        if (wasAlreadyProcessing || hasNotReachedProcessing || hasNoRecipe) {
            return;
        }

        ItemStack particleStack = press.getInputInventory().getStackInSlot(0).copy();
        boolean craftSucceeded;
        if (currentRecipe != null) {
            if (particleStack.isEmpty()) {
                particleStack = currentRecipe.getResultItem(level.registryAccess()).copy();
            }
            craftSucceeded = ForgingPressRecipe.apply(press, currentRecipe);
        }
        else if (currentPressingRecipe != null) {
            craftSucceeded = AirtightForgingPressUtils.applyPressingRecipe(press, currentPressingRecipe);
        }
        else {
            SmithingRecipeInput smithingInput = AirtightForgingPressUtils.createSmithingInput(press);
            ItemStack smithingResult = currentSmithingRecipe.assemble(smithingInput, level.registryAccess());
            if (!smithingResult.isEmpty()) {
                particleStack = smithingResult.copy();
            }
            craftSucceeded = AirtightForgingPressUtils.applySmithingRecipe(press, currentSmithingRecipe);
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

    private float getOperationSpeed() {
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

    private void startOperation() {
        operating = true;
        operatingTicks = 0;
        press.sendData();
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
        clearRecipes();
    }

    private void clearRecipes() {
        currentRecipe = null;
        currentPressingRecipe = null;
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
