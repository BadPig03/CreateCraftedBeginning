package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpPressureNetwork.PressureDistributionResult;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPumpPressureController {
    private static final int RECOVERY_INITIAL_BACKOFF = 20;
    private static final int RECOVERY_MAX_BACKOFF = 640;

    private final AirtightPumpBlockEntity pump;
    private final Couple<MutableBoolean> sidesToUpdate = Couple.create(MutableBoolean::new);
    private final Couple<MutableBoolean> recoveryAttempts = Couple.create(MutableBoolean::new);
    private final Couple<RecoveryState> recoveryStates = Couple.create(RecoveryState::new);

    private boolean pressureUpdate;
    private boolean lazyStateInitialized;
    private float lastLazyAbsSpeed;
    private Direction lastLazyFacing;

    AirtightPumpPressureController(AirtightPumpBlockEntity pump) {
        this.pump = pump;
    }

    static boolean isPullingOnSide(boolean frontSide) {
        return !frontSide;
    }

    private static boolean isSideAccessible(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && state.getValue(AirtightPumpBlock.FACING).getAxis() == direction.getAxis();
    }

    private static boolean isFront(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && direction == state.getValue(AirtightPumpBlock.FACING);
    }

    private static boolean hasRequiredSpeed(float speed) {
        return Mth.abs(speed) >= AirtightPumpBlock.MINIMUM_REQUIRED_SPEED_LEVEL.getSpeedValue();
    }

    void beforeTick() {
        if (!shouldRunServerLogic() || !pressureUpdate) {
            return;
        }

        rebuildPressure();
    }

    void afterTick() {
        Level level = pump.getLevel();
        if (!shouldRunServerLogic() || level == null) {
            return;
        }

        sidesToUpdate.forEachWithContext((shouldUpdate, isFrontSide) -> {
            if (shouldUpdate.isFalse()) {
                return;
            }

            shouldUpdate.setFalse();
            boolean isRecoveryAttempt = recoveryAttempts.get(isFrontSide).booleanValue();
            recoveryAttempts.get(isFrontSide).setFalse();
            Direction frontDirection = getFront();
            PressureDistributionResult distributionResult = AirtightPumpPressureNetwork.distributePressureTo(pump, isFrontSide ? frontDirection : frontDirection.getOpposite());
            RecoveryState recoveryState = recoveryStates.get(isFrontSide);
            if (isRecoveryAttempt) {
                recoveryState.recordRecoveryResult(distributionResult, level.getGameTime());
                return;
            }

            recoveryState.recordRebuildResult(distributionResult);
        });
    }

    boolean shouldHandleSpeedChange(float previousSpeed) {
        return shouldRunServerLogic() && !Mth.equal(Mth.abs(previousSpeed), Mth.abs(pump.getSpeed()));
    }

    boolean hasRequiredSpeed() {
        return hasRequiredSpeed(pump.getSpeed());
    }

    void lazyTick() {
        Level level = pump.getLevel();
        GasTransportBehaviour pumpTransport = getTransportBehaviour();
        if (!shouldRunServerLogic() || pumpTransport == null || level == null) {
            return;
        }

        float absoluteSpeed = Mth.abs(pump.getSpeed());
        Direction frontDirection = getFront();
        boolean pumpStateChanged = !lazyStateInitialized || !Mth.equal(absoluteSpeed, lastLazyAbsSpeed) || frontDirection != lastLazyFacing;
        lazyStateInitialized = true;
        lastLazyAbsSpeed = absoluteSpeed;
        lastLazyFacing = frontDirection;
        if (!isPumpRunning()) {
            return;
        }

        if (pumpStateChanged) {
            rebuildPressure();
            return;
        }

        long gameTime = level.getGameTime();
        BlockPos frontPipePos = pump.getBlockPos().relative(frontDirection);
        BlockPos backPipePos = pump.getBlockPos().relative(frontDirection.getOpposite());
        GasTransportBehaviour frontTransport = GasPropagator.getBehaviour(level, frontPipePos);
        GasTransportBehaviour backTransport = GasPropagator.getBehaviour(level, backPipePos);
        boolean frontPressureMissing = frontTransport != null && !frontTransport.hasAnyPressureContribution();
        boolean backPressureMissing = backTransport != null && !backTransport.hasAnyPressureContribution();
        boolean shouldRecoverFront = recoveryStates.getFirst().shouldAttempt(frontPressureMissing, gameTime);
        boolean shouldRecoverBack = recoveryStates.getSecond().shouldAttempt(backPressureMissing, gameTime);
        recoverMissingPressure(shouldRecoverFront, shouldRecoverBack);
    }

    void updatePipesOnSide(Direction direction) {
        if (!isSideAccessible(direction)) {
            return;
        }

        queueNetworkUpdate(isFront(direction));
        GasTransportBehaviour transportBehaviour = getTransportBehaviour();
        if (transportBehaviour == null) {
            return;
        }

        transportBehaviour.wipePressure();
    }

    void markPressureUpdate() {
        pressureUpdate = true;
    }

    boolean canTransport(BlockState state, Direction direction) {
        return isPumpRunning() && isSideAccessible(state, direction) && isPullingOnSide(isFront(state, direction));
    }

    boolean isPumpRunning() {
        return pump.getLevel() != null && !pump.isRemoved() && hasRequiredSpeed(pump.getSpeed());
    }

    float getPumpPressure() {
        if (!isPumpRunning()) {
            return 0;
        }
        return Mth.abs(pump.getSpeed());
    }

    boolean isSideAccessible(Direction direction) {
        return isSideAccessible(pump.getBlockState(), direction);
    }

    boolean isFront(Direction direction) {
        return isFront(pump.getBlockState(), direction);
    }

    void rebuildPressure() {
        Level level = pump.getLevel();
        if (level == null) {
            return;
        }

        GasTransportBehaviour transportBehaviour = getTransportBehaviour();
        if (transportBehaviour != null) {
            transportBehaviour.wipePressure();
        }
        Direction frontDirection = getFront();
        GasPropagator.propagatePipe(level, pump.getBlockPos().relative(frontDirection));
        GasPropagator.propagatePipe(level, pump.getBlockPos().relative(frontDirection.getOpposite()));
        recoveryAttempts.forEach(MutableBoolean::setFalse);
        sidesToUpdate.forEach(MutableBoolean::setTrue);
        pressureUpdate = false;
    }

    private boolean shouldRunServerLogic() {
        Level level = pump.getLevel();
        return level != null && (!level.isClientSide || pump.isVirtual());
    }

    private Direction getFront() {
        return pump.getBlockState().getValue(AirtightPumpBlock.FACING);
    }

    private void recoverMissingPressure(boolean shouldRecoverFront, boolean shouldRecoverBack) {
        Level level = pump.getLevel();
        if (level == null || !shouldRecoverFront && !shouldRecoverBack) {
            return;
        }

        GasTransportBehaviour transportBehaviour = getTransportBehaviour();
        if (transportBehaviour != null) {
            transportBehaviour.wipePressure();
        }
        Direction frontDirection = getFront();
        if (shouldRecoverFront) {
            GasPropagator.propagatePipe(level, pump.getBlockPos().relative(frontDirection));
            recoveryAttempts.getFirst().setTrue();
            sidesToUpdate.getFirst().setTrue();
        }
        if (!shouldRecoverBack) {
            return;
        }

        GasPropagator.propagatePipe(level, pump.getBlockPos().relative(frontDirection.getOpposite()));
        recoveryAttempts.getSecond().setTrue();
        sidesToUpdate.getSecond().setTrue();
    }

    private void queueNetworkUpdate(boolean isFrontSide) {
        recoveryAttempts.get(isFrontSide).setFalse();
        sidesToUpdate.get(isFrontSide).setTrue();
    }

    private @Nullable GasTransportBehaviour getTransportBehaviour() {
        return BlockEntityBehaviour.get(pump, GasTransportBehaviour.TYPE);
    }

    private static final class RecoveryState {
        private boolean hadValidPath;
        private boolean topologyIncomplete;
        private int backoffTicks = RECOVERY_INITIAL_BACKOFF;
        private long nextAttemptGameTime;

        private boolean shouldAttempt(boolean pressureMissing, long gameTime) {
            return (hadValidPath || topologyIncomplete) && pressureMissing && gameTime >= nextAttemptGameTime;
        }

        private void recordRebuildResult(PressureDistributionResult distributionResult) {
            hadValidPath = distributionResult.validPath();
            topologyIncomplete = distributionResult.topologyIncomplete();
            backoffTicks = RECOVERY_INITIAL_BACKOFF;
            nextAttemptGameTime = 0;
        }

        private void recordRecoveryResult(PressureDistributionResult distributionResult, long gameTime) {
            topologyIncomplete = distributionResult.topologyIncomplete();
            if (distributionResult.validPath()) {
                hadValidPath = true;
                backoffTicks = RECOVERY_INITIAL_BACKOFF;
                nextAttemptGameTime = gameTime + RECOVERY_INITIAL_BACKOFF;
                return;
            }

            nextAttemptGameTime = gameTime + backoffTicks;
            backoffTicks = Math.min(RECOVERY_MAX_BACKOFF, backoffTicks * 2);
        }
    }
}
