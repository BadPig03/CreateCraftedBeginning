package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class TeslaTurbineController {
    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;

    private boolean needsSave;
    private boolean needsClientSync;
    private float lastGeneratedSpeed = Float.NaN;

    TeslaTurbineController(TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    void tick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        core.getFlowMeter().tick();
        flushDirtyState();
        refreshGeneratedRotationIfNeeded();
    }

    void lazyTick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        core.getStructureManager().tick();
    }

    void initialize() {
        lastGeneratedSpeed = Float.NaN;
        refreshGeneratedRotationIfNeeded();
    }

    void onSpeedChanged() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide || turbine.getSpeed() == 0 || getGeneratedSpeed() == 0) {
            return;
        }

        if (!core.getStructureManager().isActive()) {
            return;
        }

        turbine.getAdvancementBehaviour().awardPlayer(CCBAdvancements.GENIUS_ENGINEER);
        if (core.getLevelCalculator().getCurrentLevel() != TeslaTurbineUtils.MAX_LEVEL) {
            return;
        }

        turbine.getAdvancementBehaviour().awardPlayer(CCBAdvancements.MIRACLE_OF_ENGINEERING);
    }

    void markForSave() {
        needsSave = true;
    }

    void markForClientSync() {
        needsClientSync = true;
    }

    void markForSaveAndClientSync() {
        needsSave = true;
        needsClientSync = true;
    }

    void onReadComplete() {
        needsSave = false;
        needsClientSync = false;
        lastGeneratedSpeed = Float.NaN;
    }

    float getGeneratedSpeed() {
        int flowDirectionMultiplier = core.getFlowMeter().isClockwiseFlow() ? -1 : 1;
        int axisDirectionMultiplier = turbine.getBlockState().getValue(TeslaTurbineBlock.AXIS) == Axis.Z ? -1 : 1;
        return TeslaTurbineUtils.BASE_ROTATION_SPEED * core.getLevelCalculator().getCurrentLevel() * flowDirectionMultiplier * axisDirectionMultiplier;
    }

    private void refreshGeneratedRotationIfNeeded() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        float generatedSpeed = getGeneratedSpeed();
        if (Float.compare(generatedSpeed, lastGeneratedSpeed) == 0) {
            return;
        }

        lastGeneratedSpeed = generatedSpeed;
        turbine.updateGeneratedRotation();
    }

    private void flushDirtyState() {
        if (needsSave) {
            turbine.setChanged();
            needsSave = false;
        }
        if (!needsClientSync) {
            return;
        }

        turbine.sendData();
        needsClientSync = false;
    }
}
