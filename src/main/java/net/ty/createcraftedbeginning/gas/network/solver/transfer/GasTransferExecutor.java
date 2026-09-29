package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.foundation.BoundedMath;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

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
        long[] drainedAmounts = new long[drainPlan.size()];
        long[] filledAmounts = new long[fillPlan.size()];
        if (gasType.isEmpty() || drainPlan.isEmpty() || fillPlan.isEmpty()) {
            return new PooledTransferExecutionResult(GasStack.EMPTY, drainedAmounts, filledAmounts);
        }

        long totalDrained = 0;
        for (int drainIndex = 0; drainIndex < drainPlan.size(); drainIndex++) {
            PlannedDrain drain = drainPlan.get(drainIndex);
            long requested = Math.clamp(drain.amount, 0L, Long.MAX_VALUE - totalDrained);
            if (requested <= 0) {
                continue;
            }

            GasStack drained = executeDrain(drain.handler, gasType.copyWithAmount(requested));
            if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, gasType)) {
                continue;
            }

            long amount = Mth.clamp(drained.getAmount(), 0L, requested);
            drainedAmounts[drainIndex] = amount;
            totalDrained += amount;
        }
        if (totalDrained <= 0) {
            return new PooledTransferExecutionResult(GasStack.EMPTY, drainedAmounts, filledAmounts);
        }

        long[] fillLimits = new long[fillPlan.size()];
        for (int fillIndex = 0; fillIndex < fillPlan.size(); fillIndex++) {
            fillLimits[fillIndex] = Math.max(0, fillPlan.get(fillIndex).amount);
        }
        long plannedFillTotal = BoundedMath.sumNonNegative(fillLimits);
        long fillBudget = Math.min(totalDrained, plannedFillTotal);
        long[] offers = GasAmountDistribution.allocateProportionally(fillBudget, fillLimits);
        long totalFilled = 0;
        for (int fillIndex = 0; fillIndex < fillPlan.size(); fillIndex++) {
            long offered = Math.min(offers[fillIndex], totalDrained - totalFilled);
            if (offered <= 0) {
                continue;
            }

            PlannedFill fill = fillPlan.get(fillIndex);
            GasStack fillRequest = gasType.copyWithAmount(offered);
            long filled = fill.sourcePressurePa >= 0 && fill.handler instanceof GasPressureBoundary boundary ? boundary.fillFromPressure(fillRequest, GasPressureLimits.clampToHardLimit(fill.sourcePressurePa), GasAction.EXECUTE) : fill.handler.fill(fillRequest, GasAction.EXECUTE);
            filled = Mth.clamp(filled, 0L, offered);
            filledAmounts[fillIndex] = filled;
            totalFilled += filled;
        }

        long remainingAmount = BoundedMath.saturatedSubtract(totalDrained, totalFilled);
        GasStack remaining = remainingAmount <= 0 ? GasStack.EMPTY : gasType.copyWithAmount(remainingAmount);
        return new PooledTransferExecutionResult(remaining, drainedAmounts, filledAmounts);
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

    public record PlannedDrain(GasHandler handler, long amount) {}

    public record PlannedFill(GasHandler handler, long amount, long sourcePressurePa) {}

    public record PooledTransferExecutionResult(GasStack remainingGas, long[] drainedAmounts, long[] filledAmounts) {}
}
