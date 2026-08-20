package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightEngineDriveController {
    private static final String COMPOUND_KEY_GENERATED_SPEED = "GeneratedSpeed";

    private final AirtightEngineBlockEntity engine;
    private WeakReference<AirtightTankBlockEntity> source = new WeakReference<>(null);

    private float lastGeneratedSpeed = Float.NaN;
    private float restoredGeneratedSpeed;
    private float persistedGeneratedSpeed;
    private boolean restoringKineticNetwork;

    AirtightEngineDriveController(AirtightEngineBlockEntity engine) {
        this.engine = engine;
    }

    void beforeInitialize() {
        Level level = engine.getLevel();
        restoringKineticNetwork = level != null && !level.isClientSide && engine.hasKineticNetwork() && restoredGeneratedSpeed != 0;
    }

    void afterInitialize() {
        restoringKineticNetwork = false;
        restoredGeneratedSpeed = 0;
        lastGeneratedSpeed = Float.NaN;
        refreshGeneratedRotationIfNeeded();
    }

    void tickServer() {
        if (engine.isEngineOverStressed()) {
            lastGeneratedSpeed = Float.NaN;
            return;
        }

        refreshGeneratedRotationIfNeeded();
    }

    float getGeneratedSpeed() {
        if (restoringKineticNetwork) {
            return restoredGeneratedSpeed;
        }

        float generatedSpeed = AirtightEngineBlockEntity.BASE_ROTATION_SPEED * getSpeedModifier() * (getRotationDirection() ? 1 : -1);
        persistedGeneratedSpeed = GasConsumptions.isFinite(generatedSpeed) ? generatedSpeed : 0;
        return persistedGeneratedSpeed;
    }

    void writePersistent(CompoundTag tag) {
        tag.putFloat(COMPOUND_KEY_GENERATED_SPEED, persistedGeneratedSpeed);
    }

    void readPersistent(CompoundTag tag) {
        float storedGeneratedSpeed = tag.contains(COMPOUND_KEY_GENERATED_SPEED) ? tag.getFloat(COMPOUND_KEY_GENERATED_SPEED) : 0;
        restoredGeneratedSpeed = GasConsumptions.isFinite(storedGeneratedSpeed) ? storedGeneratedSpeed : 0;
        persistedGeneratedSpeed = restoredGeneratedSpeed;
    }

    void rebuildKineticNetwork() {
        Level level = engine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        lastGeneratedSpeed = Float.NaN;
        engine.rebuildKineticNetwork();
    }

    @Nullable AirtightAssemblyDriverCore getDriverCore() {
        AirtightTankBlockEntity tankController = getTankController();
        return tankController == null ? null : tankController.getCore();
    }

    private void refreshGeneratedRotationIfNeeded() {
        Level level = engine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        float generatedSpeed = getGeneratedSpeed();
        if (Float.compare(generatedSpeed, lastGeneratedSpeed) == 0) {
            return;
        }

        lastGeneratedSpeed = generatedSpeed;
        engine.applyGeneratedRotation();
    }

    private float getSpeedModifier() {
        AirtightAssemblyDriverCore driverCore = getDriverCore();
        if (driverCore == null) {
            return 0;
        }

        int attachedEngines = driverCore.getAttachedEngines();
        if (attachedEngines == 0) {
            return 0;
        }
        return (float) driverCore.getCurrentLevel() / attachedEngines;
    }

    private boolean getRotationDirection() {
        BlockState state = engine.getBlockState();
        boolean facesPositiveDirection = AirtightEngineBlock.getFacing(state).getAxisDirection() == AxisDirection.POSITIVE;
        return facesPositiveDirection == state.getValue(AirtightEngineBlock.CLOCKWISE);
    }

    private @Nullable AirtightTankBlockEntity getTankController() {
        AirtightTankBlockEntity tank = getTank();
        if (tank == null) {
            return null;
        }
        return tank.getControllerBE();
    }

    private @Nullable AirtightTankBlockEntity getTank() {
        Level level = engine.getLevel();
        if (level == null) {
            return null;
        }

        AirtightTankBlockEntity tank = source.get();
        if (tank != null && !tank.isRemoved()) {
            return tank;
        }

        tank = findTank(level);
        source = new WeakReference<>(tank);
        return tank;
    }

    private @Nullable AirtightTankBlockEntity findTank(Level level) {
        Direction tankDirection = AirtightEngineBlock.getFacing(engine.getBlockState());
        BlockPos tankPos = engine.getBlockPos().relative(tankDirection);
        if (!level.isLoaded(tankPos)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(tankPos);
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            return null;
        }
        return tank;
    }
}
