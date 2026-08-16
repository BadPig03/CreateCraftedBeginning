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
public final class AirtightEngineDriveController {
    private static final String COMPOUND_KEY_GENERATED_SPEED = "GeneratedSpeed";

    private final AirtightEngineBlockEntity engine;
    private WeakReference<AirtightTankBlockEntity> source = new WeakReference<>(null);

    private float lastGeneratedSpeed = Float.NaN;
    private float restoredGeneratedSpeed;
    private float persistedGeneratedSpeed;
    private boolean restoringKineticNetwork;

    public AirtightEngineDriveController(AirtightEngineBlockEntity engine) {
        this.engine = engine;
    }

    public void beforeInitialize() {
        Level level = engine.getLevel();
        restoringKineticNetwork = level != null && !level.isClientSide && engine.hasKineticNetwork() && restoredGeneratedSpeed != 0;
    }

    public void afterInitialize() {
        restoringKineticNetwork = false;
        restoredGeneratedSpeed = 0;
        lastGeneratedSpeed = Float.NaN;
        refreshGeneratedRotationIfNeeded();
    }

    public void tickServer() {
        if (engine.isEngineOverStressed()) {
            lastGeneratedSpeed = Float.NaN;
            return;
        }

        refreshGeneratedRotationIfNeeded();
    }

    public float getGeneratedSpeed() {
        if (restoringKineticNetwork) {
            return restoredGeneratedSpeed;
        }

        float generatedSpeed = AirtightEngineBlockEntity.BASE_ROTATION_SPEED * getSpeedModifier() * (getRotationDirection() ? 1 : -1);
        persistedGeneratedSpeed = GasConsumptions.isFinite(generatedSpeed) ? generatedSpeed : 0;
        return persistedGeneratedSpeed;
    }

    public void writePersistent(CompoundTag tag) {
        tag.putFloat(COMPOUND_KEY_GENERATED_SPEED, persistedGeneratedSpeed);
    }

    public void readPersistent(CompoundTag tag) {
        float storedSpeed = tag.contains(COMPOUND_KEY_GENERATED_SPEED) ? tag.getFloat(COMPOUND_KEY_GENERATED_SPEED) : 0;
        restoredGeneratedSpeed = GasConsumptions.isFinite(storedSpeed) ? storedSpeed : 0;
        persistedGeneratedSpeed = restoredGeneratedSpeed;
    }

    public void rebuildKineticNetwork() {
        Level level = engine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        lastGeneratedSpeed = Float.NaN;
        engine.rebuildKineticNetwork();
    }

    @Nullable public AirtightAssemblyDriverCore getDriverCore() {
        AirtightTankBlockEntity controller = getTankController();
        return controller == null ? null : controller.getCore();
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
        AirtightAssemblyDriverCore core = getDriverCore();
        if (core == null) {
            return 0;
        }

        int engines = core.getAttachedEngines();
        return engines == 0 ? 0 : (float) core.getCurrentLevel() / engines;
    }

    private boolean getRotationDirection() {
        BlockState state = engine.getBlockState();
        boolean facesPositive = AirtightEngineBlock.getFacing(state).getAxisDirection() == AxisDirection.POSITIVE;
        return facesPositive == state.getValue(AirtightEngineBlock.CLOCKWISE);
    }

    private @Nullable AirtightTankBlockEntity getTankController() {
        AirtightTankBlockEntity tank = getTank();
        return tank == null ? null : tank.getControllerBE();
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
        Direction facing = AirtightEngineBlock.getFacing(engine.getBlockState());
        BlockPos tankPos = engine.getBlockPos().relative(facing);
        if (!level.isLoaded(tankPos)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(tankPos);
        return blockEntity instanceof AirtightTankBlockEntity tank ? tank : null;
    }
}
