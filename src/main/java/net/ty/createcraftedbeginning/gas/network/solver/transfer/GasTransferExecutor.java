package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasTransferSnapshot;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTransferExecutor {
    private GasTransferExecutor() {
    }

    public static GasStack simulateDrain(GasHandler drainHandler, GasStack gasType, long maxAmount) {
        if (maxAmount <= 0 || gasType.isEmpty()) {
            return GasStack.EMPTY;
        }

        for (int tankIndex = 0; tankIndex < drainHandler.getTanks(); tankIndex++) {
            GasStack tankGas = drainHandler.getGasInTank(tankIndex);
            if (tankGas.isEmpty() || !GasStack.isSameGasSameComponents(tankGas, gasType)) {
                continue;
            }

            GasStack simulatedDrain = drainHandler.drain(tankGas.copyWithAmount(maxAmount), GasAction.SIMULATE);
            if (simulatedDrain.isEmpty()) {
                break;
            }

            if (!GasStack.isSameGasSameComponents(simulatedDrain, gasType)) {
                return GasStack.EMPTY;
            }

            return simulatedDrain;
        }

        GasStack simulatedDrain = drainHandler.drain(maxAmount, GasAction.SIMULATE);
        if (simulatedDrain.isEmpty() || !GasStack.isSameGasSameComponents(simulatedDrain, gasType)) {
            return GasStack.EMPTY;
        }

        return simulatedDrain;
    }

    public static long simulateDrainAmount(GasHandler drainHandler, GasStack gasType, long maxAmount) {
        GasStack simulatedDrain = simulateDrain(drainHandler, gasType, maxAmount);
        if (simulatedDrain.isEmpty() || !GasStack.isSameGasSameComponents(simulatedDrain, gasType)) {
            return 0;
        }

        return Mth.clamp(simulatedDrain.getAmount(), 0L, maxAmount);
    }

    public static long simulateFillAmount(GasHandler fillHandler, GasStack gasType, long maxAmount) {
        return simulateFillAmount(fillHandler, gasType, maxAmount, -1);
    }

    public static long simulateFillAmount(GasHandler fillHandler, GasStack gasType, long maxAmount, long sourcePressurePa) {
        if (maxAmount <= 0 || gasType.isEmpty()) {
            return 0;
        }

        GasStack request = gasType.copyWithAmount(maxAmount);
        long filled = sourcePressurePa >= 0 && fillHandler instanceof GasPressureBoundary boundary ? boundary.fillFromPressure(request, GasPressureLimits.clampToHardLimit(sourcePressurePa), GasAction.SIMULATE) : fillHandler.fill(request, GasAction.SIMULATE);
        return Mth.clamp(filled, 0L, maxAmount);
    }

    public static PooledTransferExecutionResult executePooledTransfer(GasStack gasType, List<PlannedDrain> drainPlan, List<PlannedFill> fillPlan) {
        return executePooledTransfer(gasType, drainPlan, fillPlan, execution -> {
            long remaining = execution.remainingGas().getAmount();
            long uncertainRecovery = 0;
            RuntimeException recoveryFailure = null;
            for (int index = 0; index < drainPlan.size() && remaining > 0; index++) {
                long amount = Math.min(remaining, execution.drainedAmounts()[index]);
                if (amount <= 0) {
                    continue;
                }

                GasHandler handler = drainPlan.get(index).handler();
                GasStack resource = gasType.copyWithAmount(amount);
                GasTransferSnapshot snapshot = null;
                boolean restoring = false;
                try {
                    snapshot = handler.captureTransferSnapshot();
                    restoring = true;
                    long restored;
                    if (handler instanceof GasPressureCompartment compartment) {
                        restored = compartment.restoreDrainedGas(resource, GasAction.EXECUTE);
                    }
                    else if (handler instanceof GasPressureBoundary boundary && boundary.supportsExactDrainRecovery(0)) {
                        restored = boundary.restoreDrainedGas(0, resource, GasAction.EXECUTE);
                    }
                    else {
                        restored = handler.fill(resource, GasAction.EXECUTE);
                    }
                    remaining -= Mth.clamp(restored, 0L, amount);
                }
                catch (RuntimeException | Error failure) {
                    if (restoring && !restoreSnapshot(snapshot, failure)) {
                        remaining -= amount;
                        uncertainRecovery += amount;
                        failure.addSuppressed(new IllegalStateException("Unconfirmed gas restoration of up to " + amount + " GU by " + handler.getClass().getName() + "; this amount must not be offered to another source."));
                    }
                    if (recoveryFailure == null) {
                        recoveryFailure = new IllegalStateException("Failed to restore drained gas after a pooled transfer failure.");
                    }
                    recoveryFailure.addSuppressed(failure);
                }
            }
            if (remaining > 0 || recoveryFailure != null) {
                throw new IllegalStateException("Pooled transfer recovery failed; unconfirmed amount: " + (remaining + uncertainRecovery) + " GU.", recoveryFailure);
            }
        });
    }

    static PooledTransferExecutionResult executePooledTransfer(GasStack gasType, List<PlannedDrain> drainPlan, List<PlannedFill> fillPlan, Consumer<PooledTransferExecutionResult> failureRecovery) {
        Execution execution = new Execution(gasType, drainPlan, fillPlan);
        try {
            return execution.execute();
        }
        catch (RuntimeException | Error failure) {
            execution.restoreFailedOperation(failure);
            PooledTransferExecutionResult partial = execution.result();
            try {
                failureRecovery.accept(partial);
            }
            catch (RuntimeException | Error recoveryFailure) {
                if (recoveryFailure != failure) {
                    failure.addSuppressed(recoveryFailure);
                }
                failure.addSuppressed(new IllegalStateException("Unsettled pooled transfer: " + partial.remainingGas().getAmount() + " GU offered for recovery; restoration may be partial."));
            }
            throw failure;
        }
    }

    private static boolean restoreSnapshot(@Nullable GasTransferSnapshot snapshot, Throwable failure) {
        if (snapshot == null) {
            return false;
        }

        boolean restored = false;
        try {
            snapshot.restore();
            restored = true;
            snapshot.afterRestore();
        }
        catch (RuntimeException | Error recoveryFailure) {
            if (recoveryFailure != failure) {
                failure.addSuppressed(recoveryFailure);
            }
        }
        return restored;
    }

    public record PlannedDrain(GasHandler handler, long amount) {}

    public record PlannedFill(GasHandler handler, long amount, long sourcePressurePa) {}

    public record PooledTransferExecutionResult(GasStack remainingGas, long[] drainedAmounts, long[] filledAmounts) {}

    private static final class Execution {
        private final GasStack gasType;
        private final List<PlannedDrain> drainPlan;
        private final List<PlannedFill> fillPlan;
        private final long[] drainedAmounts;
        private final long[] filledAmounts;
        private long totalDrained;
        private long totalFilled;
        private long uncertainFill;
        private long attemptedAmount;
        private boolean extracting;
        private @Nullable GasHandler attemptedHandler;
        private @Nullable GasTransferSnapshot snapshot;

        private Execution(GasStack gasType, List<PlannedDrain> drainPlan, List<PlannedFill> fillPlan) {
            this.gasType = gasType;
            this.drainPlan = drainPlan;
            this.fillPlan = fillPlan;
            drainedAmounts = new long[drainPlan.size()];
            filledAmounts = new long[fillPlan.size()];
        }

        private static GasStack executeDrain(GasHandler drainHandler, GasStack drainRequest) {
            if (drainRequest.isEmpty()) {
                return GasStack.EMPTY;
            }

            GasStack drainedGas = drainHandler.drain(drainRequest, GasAction.EXECUTE);
            if (!drainedGas.isEmpty()) {
                return drainedGas;
            }

            GasStack genericDrainPreview = drainHandler.drain(drainRequest.getAmount(), GasAction.SIMULATE);
            if (genericDrainPreview.isEmpty() || !GasStack.isSameGasSameComponents(genericDrainPreview, drainRequest)) {
                return GasStack.EMPTY;
            }

            return drainHandler.drain(drainRequest.getAmount(), GasAction.EXECUTE);
        }

        private PooledTransferExecutionResult execute() {
            if (gasType.isEmpty() || drainPlan.isEmpty() || fillPlan.isEmpty()) {
                return result();
            }

            for (int drainIndex = 0; drainIndex < drainPlan.size(); drainIndex++) {
                PlannedDrain drain = drainPlan.get(drainIndex);
                long requested = Math.clamp(drain.amount, 0L, Long.MAX_VALUE - totalDrained);
                if (requested <= 0) {
                    continue;
                }

                beginOperation(drain.handler, requested, true);
                GasStack drained = executeDrain(drain.handler, gasType.copyWithAmount(requested));
                attemptedHandler = null;
                snapshot = null;
                if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, gasType)) {
                    continue;
                }

                long amount = Mth.clamp(drained.getAmount(), 0L, requested);
                drainedAmounts[drainIndex] = amount;
                totalDrained += amount;
            }
            if (totalDrained <= 0) {
                return result();
            }

            long[] fillLimits = new long[fillPlan.size()];
            for (int fillIndex = 0; fillIndex < fillPlan.size(); fillIndex++) {
                fillLimits[fillIndex] = Math.max(0, fillPlan.get(fillIndex).amount);
            }
            long plannedFillTotal = BoundedMath.sumNonNegative(fillLimits);
            long fillBudget = Math.min(totalDrained, plannedFillTotal);
            long[] offers = GasAmountDistribution.allocateProportionally(fillBudget, fillLimits);
            for (int fillIndex = 0; fillIndex < fillPlan.size(); fillIndex++) {
                long offered = Math.min(offers[fillIndex], totalDrained - totalFilled);
                if (offered <= 0) {
                    continue;
                }

                PlannedFill fill = fillPlan.get(fillIndex);
                GasStack fillRequest = gasType.copyWithAmount(offered);
                beginOperation(fill.handler, offered, false);
                long filled = fill.sourcePressurePa >= 0 && fill.handler instanceof GasPressureBoundary boundary ? boundary.fillFromPressure(fillRequest, GasPressureLimits.clampToHardLimit(fill.sourcePressurePa), GasAction.EXECUTE) : fill.handler.fill(fillRequest, GasAction.EXECUTE);
                attemptedHandler = null;
                snapshot = null;
                filled = Mth.clamp(filled, 0L, offered);
                filledAmounts[fillIndex] = filled;
                totalFilled += filled;
            }
            return result();
        }

        private PooledTransferExecutionResult result() {
            long remainingAmount = BoundedMath.saturatedSubtract(totalDrained, totalFilled + uncertainFill);
            GasStack remaining = remainingAmount <= 0 ? GasStack.EMPTY : gasType.copyWithAmount(remainingAmount);
            return new PooledTransferExecutionResult(remaining, drainedAmounts, filledAmounts);
        }

        private void beginOperation(GasHandler handler, long amount, boolean extracting) {
            GasTransferSnapshot captured = handler.captureTransferSnapshot();
            attemptedHandler = handler;
            attemptedAmount = amount;
            this.extracting = extracting;
            snapshot = captured;
        }

        private void restoreFailedOperation(Throwable failure) {
            if (attemptedHandler == null) {
                return;
            }

            if (restoreSnapshot(snapshot, failure)) {
                return;
            }
            if (!extracting) {
                uncertainFill = attemptedAmount;
            }
            String operation = extracting ? "drain" : "fill";
            failure.addSuppressed(new IllegalStateException("Unconfirmed gas " + operation + " of up to " + attemptedAmount + " GU by " + attemptedHandler.getClass().getName() + "; no successful transfer snapshot restoration. This amount must not be blindly refunded."));
        }
    }
}
