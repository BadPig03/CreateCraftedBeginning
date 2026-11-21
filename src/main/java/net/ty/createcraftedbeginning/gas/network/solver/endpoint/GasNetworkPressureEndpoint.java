package net.ty.createcraftedbeginning.gas.network.solver.endpoint;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasTransferSnapshot;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import net.ty.createcraftedbeginning.gas.transfer.GasTransferRollback;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasNetworkPressureEndpoint(TransferAccess access, PressureState pressureState, TransferLimits transferLimits, FlowResistance flowResistance, Recovery recovery, List<PredictionAccess> predictionAccesses) {
    public GasNetworkPressureEndpoint {
        predictionAccesses = List.copyOf(predictionAccesses);
    }

    public GasNetworkPressureEndpoint(TransferAccess access, PressureState pressureState, TransferLimits transferLimits, FlowResistance flowResistance, Recovery recovery) {
        this(access, pressureState, transferLimits, flowResistance, recovery, predictionAccesses(access));
    }

    private static List<PredictionAccess> predictionAccesses(TransferAccess access) {
        List<PredictionAccess> result = new ArrayList<>();
        if (access.drainHandler() instanceof GasPressureCompartment compartment) {
            result.add(new PredictionAccess(compartment, access.drainFaces()));
        }
        if (access.fillHandler() instanceof GasPressureCompartment compartment) {
            result.add(new PredictionAccess(compartment, access.fillFaces()));
        }
        return result;
    }

    public Object transferIdentity() {
        if (!predictionAccesses.isEmpty()) {
            return predictionAccesses.getFirst().compartment().getCompartmentIdentity();
        }

        GasHandler drain = access.drainHandler();
        if (drain != null) {
            return drain;
        }

        GasHandler fill = access.fillHandler();
        if (fill != null) {
            return fill;
        }

        return this;
    }

    public long maxQuantizedDrainAmount(GasStack gas, long requestedAmount) {
        long capped = Mth.clamp(requestedAmount, 0, transferLimits.drainLimit());
        if (!pressureState.fixedPressure()) {
            capped = Math.min(capped, pressureState.currentAmount());
        }
        if (capped <= 0) {
            return 0;
        }

        GasHandler handler = access.drainHandler();
        if (handler == null) {
            return 0;
        }

        return GasTransferExecutor.simulateDrainAmount(handler, gas, capped);
    }

    public long maxQuantizedFillAmount(GasStack gas, long requestedAmount, double manifoldPressurePa) {
        long capped = Mth.clamp(requestedAmount, 0, transferLimits.fillLimit());
        if (!pressureState.fixedPressure()) {
            capped = Math.min(capped, BoundedMath.saturatedSubtract(pressureState.maxAmount(), pressureState.currentAmount()));
        }
        if (capped <= 0) {
            return 0;
        }

        GasHandler handler = access.fillHandler();
        if (handler == null) {
            return 0;
        }

        long sourcePressurePa = GasPressure.round(GasPressureLimits.clampToHardLimit(manifoldPressurePa));
        return GasTransferExecutor.simulateFillAmount(handler, gas, capped, sourcePressurePa);
    }

    public long restoreRemainder(Level level, GasStack remainder) {
        if (remainder.isEmpty()) {
            return 0;
        }

        GasHandler drainHandler = access.drainHandler();
        if (drainHandler == null) {
            return 0;
        }

        if (recovery.discardRemainder()) {
            return remainder.getAmount();
        }

        GasStack remaining = remainder.copy();
        GasTransferSnapshot snapshot = null;
        boolean restoring = false;
        try {
            if (recovery.compartments().isEmpty()) {
                snapshot = drainHandler.captureTransferSnapshot();
                restoring = true;
                long restored;
                if (drainHandler instanceof GasPressureCompartment compartment) {
                    restored = compartment.restoreDrainedGas(remaining, GasAction.EXECUTE);
                }
                else if (drainHandler instanceof GasPressureBoundary boundary && boundary.supportsExactDrainRecovery(0)) {
                    restored = boundary.restoreDrainedGas(0, remaining, GasAction.EXECUTE);
                }
                else {
                    restored = drainHandler.fill(remaining, GasAction.EXECUTE);
                }
                restoring = false;
                restored = Mth.clamp(restored, 0L, remaining.getAmount());
                remaining.shrink(restored);
            }
            else {
                Set<Object> attemptedIdentities = Collections.newSetFromMap(new IdentityHashMap<>());
                for (GasPressureCompartment compartment : recovery.compartments()) {
                    if (remaining.isEmpty() || !attemptedIdentities.add(compartment.getCompartmentIdentity())) {
                        continue;
                    }

                    snapshot = compartment.captureTransferSnapshot();
                    restoring = true;
                    long restored = compartment.restoreDrainedGas(remaining, GasAction.EXECUTE);
                    restoring = false;
                    restored = Mth.clamp(restored, 0L, remaining.getAmount());
                    remaining.shrink(restored);
                }
            }
        }
        catch (RuntimeException | Error failure) {
            if (restoring && !GasTransferRollback.restore(snapshot, failure)) {
                failure.addSuppressed(new IllegalStateException("Unconfirmed gas remainder restoration of up to " + remaining.getAmount() + " GU; this amount was not retained for retry."));
                throw failure;
            }

            try {
                if (!retainRemainder(level, remaining)) {
                    failure.addSuppressed(new IllegalStateException("Gas remainder could not be retained after restoration failed: " + remaining.getAmount() + " GU."));
                }
            }
            catch (RuntimeException | Error retentionFailure) {
                if (retentionFailure != failure) {
                    failure.addSuppressed(retentionFailure);
                }
            }
            throw failure;
        }

        long secured = BoundedMath.saturatedSubtract(remainder.getAmount(), remaining.getAmount());
        if (remaining.isEmpty() || !retainRemainder(level, remaining)) {
            return secured;
        }

        return remainder.getAmount();
    }

    private boolean retainRemainder(Level level, GasStack remaining) {
        GasPipeConnection recoveryConnection = recovery.connection();
        BlockPos recoveryPipePos = recovery.pipePos();
        if (recoveryConnection == null || recoveryPipePos == null || !recoveryConnection.retainPendingTransfer(remaining)) {
            return false;
        }

        BlockEntity blockEntity = level.isLoaded(recoveryPipePos) ? level.getBlockEntity(recoveryPipePos) : null;
        if (blockEntity != null) {
            blockEntity.setChanged();
        }
        return true;
    }

    public record TransferAccess(@Nullable GasHandler drainHandler, @Nullable GasHandler fillHandler, List<BlockFace> drainFaces, List<BlockFace> fillFaces) {
        public TransferAccess {
            drainFaces = List.copyOf(drainFaces);
            fillFaces = List.copyOf(fillFaces);
        }
    }

    public record PressureState(double pressurePa, boolean fixedPressure, long currentAmount, long volume, long maxPressurePa, long maxAmount) {}

    public record TransferLimits(long drainLimit, long fillLimit) {}

    public record PredictionAccess(GasPressureCompartment compartment, List<BlockFace> faces) {
        public PredictionAccess {
            faces = List.copyOf(faces);
        }
    }

    public record FlowResistance(long drainResistanceUnits, long fillResistanceUnits) {
        private static final long STANDARD_PORT_EXTRA_UNITS = GasFlowResistance.UNITS_PER_STANDARD_SEGMENT / 2 - GasFlowResistance.BASE_ENDPOINT_RESISTANCE_UNITS;

        public static final FlowResistance NONE = new FlowResistance(0, 0);
        static final FlowResistance STANDARD_PIPE_PORT = new FlowResistance(STANDARD_PORT_EXTRA_UNITS, STANDARD_PORT_EXTRA_UNITS);

        public FlowResistance {
            drainResistanceUnits = Math.max(0, drainResistanceUnits);
            fillResistanceUnits = Math.max(0, fillResistanceUnits);
        }
    }

    public record Recovery(List<GasPressureCompartment> compartments, @Nullable GasPipeConnection connection, @Nullable BlockPos pipePos, boolean discardRemainder) {
        public static final Recovery NONE = new Recovery(List.of(), null, null, false);
        public static final Recovery DISCARD = new Recovery(List.of(), null, null, true);

        public Recovery {
            compartments = List.copyOf(compartments);
            pipePos = pipePos == null ? null : pipePos.immutable();
        }
    }
}
