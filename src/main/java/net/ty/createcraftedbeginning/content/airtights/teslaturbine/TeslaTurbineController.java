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

    private boolean saveDirty;
    private boolean clientDirty;
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
        if (turbine.isOverStressed()) {
            lastGeneratedSpeed = Float.NaN;
            return;
        }

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

    float getGeneratedSpeed() {
        int direction = core.getFlowMeter().isClockwiseFlow() ? -1 : 1;
        int modifier = turbine.getBlockState().getValue(TeslaTurbineBlock.AXIS) == Axis.Z ? -1 : 1;
        return TeslaTurbineUtils.BASE_ROTATION_SPEED * core.getLevelCalculator().getCurrentLevel() * direction * modifier;
    }

    void markForSave() {
        saveDirty = true;
    }

    void markForClientSync() {
        clientDirty = true;
    }

    void markForSaveAndClientSync() {
        saveDirty = true;
        clientDirty = true;
    }

    void onReadComplete() {
        saveDirty = false;
        clientDirty = false;
        lastGeneratedSpeed = Float.NaN;
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
        if (saveDirty) {
            turbine.setChanged();
            saveDirty = false;
        }
        if (!clientDirty) {
            return;
        }

        turbine.sendData();
        clientDirty = false;
    }
}
