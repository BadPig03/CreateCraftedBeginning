package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightPumpPressureController {
    private static final int RECOVERY_INITIAL_BACKOFF = 20;
    private static final int RECOVERY_MAX_BACKOFF = 640;
    private static final float MIN_PUMP_SPEED = SpeedLevel.MEDIUM.getSpeedValue();

    private final AirtightPumpBlockEntity pump;
    private final Couple<MutableBoolean> sidesToUpdate = Couple.create(MutableBoolean::new);
    private final Couple<MutableBoolean> recoveryAttempts = Couple.create(MutableBoolean::new);
    private final Couple<RecoveryState> recoveryStates = Couple.create(RecoveryState::new);

    private boolean pressureUpdate;
    private boolean lazyStateInitialized;
    private float lastLazyAbsSpeed;
    private Direction lastLazyFacing;

    public AirtightPumpPressureController(AirtightPumpBlockEntity pump) {
        this.pump = pump;
    }

    public static boolean isPullingOnSide(boolean frontSide) {
        return !frontSide;
    }

    public static boolean isSideAccessible(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && state.getValue(AirtightPumpBlock.FACING).getAxis() == direction.getAxis();
    }

    public static boolean isFront(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && direction == state.getValue(AirtightPumpBlock.FACING);
    }

    private static boolean hasRequiredSpeed(float speed) {
        return Mth.abs(speed) >= MIN_PUMP_SPEED;
    }

    public void beforeTick() {
        if (!shouldRunServerLogic() || !pressureUpdate) {
            return;
        }

        rebuildPressure();
    }

    public void afterTick() {
        Level level = pump.getLevel();
        if (!shouldRunServerLogic() || level == null) {
            return;
        }

        sidesToUpdate.forEachWithContext((update, frontSide) -> {
            if (update.isFalse()) {
                return;
            }

            update.setFalse();
            boolean recoveryAttempt = recoveryAttempts.get(frontSide).booleanValue();
            recoveryAttempts.get(frontSide).setFalse();
            Direction front = getFront();
            boolean validPath = AirtightPumpPressureNetwork.distributePressureTo(pump, frontSide ? front : front.getOpposite());
            RecoveryState recovery = recoveryStates.get(frontSide);
            if (recoveryAttempt) {
                recovery.recordRecoveryResult(validPath, level.getGameTime());
                return;
            }

            recovery.recordRebuildResult(validPath);
        });
    }

    public boolean shouldHandleSpeedChange(float previousSpeed) {
        return shouldRunServerLogic() && Mth.abs(previousSpeed) != Mth.abs(pump.getSpeed());
    }

    public boolean hasRequiredSpeed() {
        return hasRequiredSpeed(pump.getSpeed());
    }

    public void lazyTick() {
        Level level = pump.getLevel();
        GasTransportBehaviour transportBehaviour = getTransportBehaviour();
        if (!shouldRunServerLogic() || transportBehaviour == null || level == null) {
            return;
        }

        float absSpeed = Mth.abs(pump.getSpeed());
        Direction front = getFront();
        boolean stateChanged = !lazyStateInitialized || absSpeed != lastLazyAbsSpeed || front != lastLazyFacing;
        lazyStateInitialized = true;
        lastLazyAbsSpeed = absSpeed;
        lastLazyFacing = front;
        if (!isPumpRunning()) {
            return;
        }

        if (stateChanged) {
            rebuildPressure();
            return;
        }

        long gameTime = level.getGameTime();
        BlockPos frontPos = pump.getBlockPos().relative(front);
        BlockPos backPos = pump.getBlockPos().relative(front.getOpposite());
        GasTransportBehaviour frontPipe = GasPropagator.getBehaviour(level, frontPos);
        GasTransportBehaviour backPipe = GasPropagator.getBehaviour(level, backPos);
        boolean frontPressureMissing = frontPipe != null && !frontPipe.hasAnyPressureContribution();
        boolean backPressureMissing = backPipe != null && !backPipe.hasAnyPressureContribution();
        boolean recoverFront = recoveryStates.getFirst().shouldAttempt(frontPressureMissing, gameTime);
        boolean recoverBack = recoveryStates.getSecond().shouldAttempt(backPressureMissing, gameTime);
        recoverMissingPressure(recoverFront, recoverBack);
    }

    public void updatePipesOnSide(Direction side) {
        if (!isSideAccessible(side)) {
            return;
        }

        queueNetworkUpdate(isFront(side));
        GasTransportBehaviour transportBehaviour = getTransportBehaviour();
        if (transportBehaviour == null) {
            return;
        }

        transportBehaviour.wipePressure();
    }

    public void markPressureUpdate() {
        pressureUpdate = true;
    }

    public boolean canTransport(BlockState state, Direction direction) {
        return isPumpRunning() && isSideAccessible(state, direction) && isPullingOnSide(isFront(state, direction));
    }

    public boolean isPumpRunning() {
        return pump.getLevel() != null && !pump.isRemoved() && hasRequiredSpeed(pump.getSpeed());
    }

    public float getPumpPressure() {
        return isPumpRunning() ? Mth.abs(pump.getSpeed()) : 0;
    }

    public boolean isSideAccessible(Direction direction) {
        return isSideAccessible(pump.getBlockState(), direction);
    }

    public boolean isFront(Direction direction) {
        return isFront(pump.getBlockState(), direction);
    }

    private boolean shouldRunServerLogic() {
        Level level = pump.getLevel();
        return level != null && (!level.isClientSide || pump.isVirtual());
    }

    private Direction getFront() {
        return pump.getBlockState().getValue(AirtightPumpBlock.FACING);
    }

    public void rebuildPressure() {
        Level level = pump.getLevel();
        if (level == null) {
            return;
        }

        GasTransportBehaviour transportBehaviour = getTransportBehaviour();
        if (transportBehaviour != null) {
            transportBehaviour.wipePressure();
        }
        Direction front = getFront();
        GasPropagator.propagatePipe(level, pump.getBlockPos().relative(front));
        GasPropagator.propagatePipe(level, pump.getBlockPos().relative(front.getOpposite()));
        recoveryAttempts.forEach(MutableBoolean::setFalse);
        sidesToUpdate.forEach(MutableBoolean::setTrue);
        pressureUpdate = false;
    }

    private void recoverMissingPressure(boolean recoverFront, boolean recoverBack) {
        Level level = pump.getLevel();
        if (level == null || !recoverFront && !recoverBack) {
            return;
        }

        GasTransportBehaviour transportBehaviour = getTransportBehaviour();
        if (transportBehaviour != null) {
            transportBehaviour.wipePressure();
        }
        Direction front = getFront();
        if (recoverFront) {
            GasPropagator.propagatePipe(level, pump.getBlockPos().relative(front));
            recoveryAttempts.getFirst().setTrue();
            sidesToUpdate.getFirst().setTrue();
        }
        if (!recoverBack) {
            return;
        }

        GasPropagator.propagatePipe(level, pump.getBlockPos().relative(front.getOpposite()));
        recoveryAttempts.getSecond().setTrue();
        sidesToUpdate.getSecond().setTrue();
    }

    private void queueNetworkUpdate(boolean front) {
        recoveryAttempts.get(front).setFalse();
        sidesToUpdate.get(front).setTrue();
    }

    private @Nullable GasTransportBehaviour getTransportBehaviour() {
        return BlockEntityBehaviour.get(pump, GasTransportBehaviour.TYPE);
    }

    private static final class RecoveryState {
        private boolean hadValidPath;
        private int backoff = RECOVERY_INITIAL_BACKOFF;
        private long nextAttempt;

        private boolean shouldAttempt(boolean pressureMissing, long gameTime) {
            return hadValidPath && pressureMissing && gameTime >= nextAttempt;
        }

        private void recordRebuildResult(boolean validPath) {
            hadValidPath = validPath;
            backoff = RECOVERY_INITIAL_BACKOFF;
            nextAttempt = 0;
        }

        private void recordRecoveryResult(boolean validPath, long gameTime) {
            if (validPath) {
                hadValidPath = true;
                backoff = RECOVERY_INITIAL_BACKOFF;
                nextAttempt = gameTime + RECOVERY_INITIAL_BACKOFF;
                return;
            }

            nextAttempt = gameTime + backoff;
            backoff = Math.min(RECOVERY_MAX_BACKOFF, backoff * 2);
        }
    }
}
