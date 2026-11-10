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
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PredictedTransferLimits;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressurePrediction;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.math.GasPressureTransferMath;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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

    public GasNetworkPressureEndpoint(TransferAccess access, PressureState pressureState, TransferLimits transferLimits, Recovery recovery) {
        this(access, pressureState, transferLimits, FlowResistance.NONE, recovery);
    }

    public static @Nullable List<GasNetworkPressureEndpoint> predictAll(GasStack gas, List<GasNetworkPressureEndpoint> endpoints, Map<GasNetworkPressureEndpoint, Long> deltas) {
        Map<Object, GasStack> contents = new IdentityHashMap<>();
        Set<Object> affectedGroups = Collections.newSetFromMap(new IdentityHashMap<>());
        for (GasNetworkPressureEndpoint endpoint : endpoints) {
            long delta = endpoint.pressureState.fixedPressure() ? 0 : deltas.getOrDefault(endpoint, 0L);
            long amount = Mth.clamp(BoundedMath.saturatedAdd(endpoint.pressureState.currentAmount(), delta), 0, endpoint.pressureState.maxAmount());
            for (PredictionAccess access : endpoint.predictionAccesses) {
                GasPressureCompartment compartment = access.compartment();
                GasStack projectedGas = delta == 0 ? compartment.getGasStack().copyWithAmount(amount) : gas.copyWithAmount(amount);
                contents.put(compartment.getCompartmentIdentity(), projectedGas);
                if (delta == 0) {
                    continue;
                }

                affectedGroups.add(compartment.getPredictionGroup());
            }
        }

        GasPressurePrediction prediction = new GasPressurePrediction(contents);
        List<GasNetworkPressureEndpoint> projected = new ArrayList<>(endpoints.size());
        for (GasNetworkPressureEndpoint endpoint : endpoints) {
            long delta = deltas.getOrDefault(endpoint, 0L);
            boolean affected = delta != 0;
            for (PredictionAccess access : endpoint.predictionAccesses) {
                affected |= affectedGroups.contains(access.compartment().getPredictionGroup());
            }

            GasNetworkPressureEndpoint hypothetical = affected ? endpoint.withAmountDelta(gas, delta, prediction) : endpoint;
            if (hypothetical == null) {
                return null;
            }

            projected.add(hypothetical);
        }
        return List.copyOf(projected);
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
        if (access.drainHandler() != null) {
            return access.drainHandler();
        }
        if (access.fillHandler() != null) {
            return access.fillHandler();
        }

        return this;
    }

    public boolean canQuantizedDrain(GasStack gas) {
        return maxQuantizedDrainAmount(gas, 1) >= 1;
    }

    public boolean canQuantizedFill(GasStack gas, double manifoldPressurePa) {
        return maxQuantizedFillAmount(gas, 1, manifoldPressurePa) >= 1;
    }

    public long quantizedPressureStepAmount() {
        if (pressureState.fixedPressure()) {
            return 1;
        }

        long volume = pressureState.volume();
        if (volume <= 0) {
            return 1;
        }

        long whole = volume / GasPressure.REFERENCE_PRESSURE_PA;
        long remainder = volume % GasPressure.REFERENCE_PRESSURE_PA;
        if (remainder == 0) {
            return Math.max(1, whole);
        }

        return Math.max(1, BoundedMath.saturatedAdd(whole, 1));
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

    public @Nullable GasNetworkPressureEndpoint withAmountDelta(GasStack gas, long delta) {
        return withAmountDelta(gas, delta, null);
    }

    public long maxSafeDrainAmount(GasStack gas, double manifoldPressurePa, long flowBudget) {
        long capped = Mth.clamp(flowBudget, 0, transferLimits.drainLimit());
        if (capped <= 0) {
            return 0;
        }

        GasHandler handler = access.drainHandler();
        if (handler == null) {
            return 0;
        }

        if (pressureState.fixedPressure()) {
            return capped;
        }

        double clampedManifoldPressurePa = GasPressureLimits.clampToHardLimit(manifoldPressurePa);
        long pressureLimitedAmount = GasPressureTransferMath.maxDrainAmount(pressureState.currentAmount(), pressureState.volume(), pressureState.pressurePa(), clampedManifoldPressurePa);
        long simulatedAmount = GasTransferExecutor.simulateDrainAmount(handler, gas, capped);
        return Math.min(capped, Math.min(pressureLimitedAmount, simulatedAmount));
    }

    public long maxSafeFillAmount(GasStack gas, double manifoldPressurePa, long flowBudget) {
        long capped = Mth.clamp(flowBudget, 0, transferLimits.fillLimit());
        if (capped <= 0) {
            return 0;
        }

        GasHandler handler = access.fillHandler();
        if (handler == null) {
            return 0;
        }

        double clampedManifoldPressurePa = GasPressureLimits.clampToHardLimit(manifoldPressurePa);
        long sourcePressurePa = GasPressure.round(clampedManifoldPressurePa);
        long simulatedAmount = GasTransferExecutor.simulateFillAmount(handler, gas, capped, sourcePressurePa);
        if (pressureState.fixedPressure()) {
            return Math.min(capped, simulatedAmount);
        }

        long pressureLimitedAmount = GasPressureTransferMath.maxFillAmount(pressureState.currentAmount(), pressureState.maxAmount(), pressureState.volume(), pressureState.pressurePa(), clampedManifoldPressurePa, pressureState.maxPressurePa());
        return Math.min(capped, Math.min(pressureLimitedAmount, simulatedAmount));
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
            if (restoring) {
                if (snapshot == null) {
                    failure.addSuppressed(new IllegalStateException("Gas remainder restoration changed an unknown amount; pending transfer was not duplicated."));
                    throw failure;
                }

                try {
                    snapshot.restore();
                }
                catch (RuntimeException | Error rollbackFailure) {
                    if (rollbackFailure != failure) {
                        failure.addSuppressed(rollbackFailure);
                    }
                    throw failure;
                }
                try {
                    snapshot.afterRestore();
                }
                catch (RuntimeException | Error notificationFailure) {
                    if (notificationFailure != failure) {
                        failure.addSuppressed(notificationFailure);
                    }
                }
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

        BlockEntity blockEntity = level.getBlockEntity(recoveryPipePos);
        if (blockEntity != null) {
            blockEntity.setChanged();
        }
        return true;
    }

    private @Nullable GasNetworkPressureEndpoint withAmountDelta(GasStack gas, long delta, @Nullable GasPressurePrediction prediction) {
        if (delta == 0 && prediction == null || pressureState.fixedPressure()) {
            return this;
        }

        long updatedAmount;
        if (delta > 0) {
            updatedAmount = Math.min(pressureState.maxAmount(), BoundedMath.saturatedAdd(pressureState.currentAmount(), delta));
        }
        else {
            updatedAmount = Math.max(0, BoundedMath.saturatedAdd(pressureState.currentAmount(), delta));
        }
        double updatedPressure = GasPressureLimits.clampToHardLimit(GasPressure.pressureExact(updatedAmount, pressureState.volume()));
        if (predictionAccesses.isEmpty()) {
            return null;
        }

        List<BlockFace> drainFaces = new ArrayList<>();
        List<BlockFace> fillFaces = new ArrayList<>();
        GasHandler drainHandler = null;
        GasHandler fillHandler = null;
        long updatedDrainLimit = 0;
        long updatedFillLimit = 0;
        for (PredictionAccess predictionAccess : predictionAccesses) {
            GasPressureCompartment compartment = predictionAccess.compartment();
            PredictedTransferLimits limits = prediction == null ? compartment.predictTransferLimits(gas, updatedAmount) : compartment.predictTransferLimits(gas, prediction);
            if (limits == null) {
                return null;
            }

            long drain = Math.max(0, Math.min(limits.drainLimit(), updatedAmount));
            long fill = Math.max(0, Math.min(limits.fillLimit(), pressureState.maxAmount() - updatedAmount));
            if (drain > 0) {
                for (BlockFace face : predictionAccess.faces()) {
                    if (drainFaces.contains(face)) {
                        continue;
                    }

                    drainFaces.add(face);
                }
            }
            if (fill > 0) {
                for (BlockFace face : predictionAccess.faces()) {
                    if (fillFaces.contains(face)) {
                        continue;
                    }

                    fillFaces.add(face);
                }
            }
            if (drain > updatedDrainLimit) {
                updatedDrainLimit = drain;
                drainHandler = compartment;
            }
            if (!(fill > updatedFillLimit)) {
                continue;
            }

            updatedFillLimit = fill;
            fillHandler = compartment;
        }
        PressureState updatedPressureState = new PressureState(updatedPressure, false, updatedAmount, pressureState.volume(), pressureState.maxPressurePa(), pressureState.maxAmount());
        TransferLimits updatedTransferLimits = new TransferLimits(updatedDrainLimit, updatedFillLimit);
        TransferAccess updatedAccess = new TransferAccess(drainHandler, fillHandler, drainFaces, fillFaces);
        return new GasNetworkPressureEndpoint(updatedAccess, updatedPressureState, updatedTransferLimits, flowResistance, recovery, predictionAccesses);
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
        public static final FlowResistance NONE = new FlowResistance(0, 0);

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
