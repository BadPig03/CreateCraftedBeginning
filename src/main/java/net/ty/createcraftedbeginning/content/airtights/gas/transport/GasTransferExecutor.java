package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTransferExecutor {
    private GasTransferExecutor() {
    }

    public static GasStack simulateSourceDrain(IGasHandler sourceCap, GasStack gas, long maxAmount) {
        if (maxAmount <= 0 || gas.isEmpty()) {
            return GasStack.EMPTY;
        }

        for (int i = 0; i < sourceCap.getTanks(); i++) {
            GasStack contained = sourceCap.getGasInTank(i);
            if (contained.isEmpty() || !GasStack.isSameGasSameComponents(contained, gas)) {
                continue;
            }

            GasStack drained = sourceCap.drain(contained.copyWithAmount(maxAmount), GasAction.SIMULATE);
            if (drained.isEmpty()) {
                break;
            }

            return GasStack.isSameGasSameComponents(drained, gas) ? drained : GasStack.EMPTY;
        }

        GasStack drained = sourceCap.drain(maxAmount, GasAction.SIMULATE);
        return !drained.isEmpty() && GasStack.isSameGasSameComponents(drained, gas) ? drained : GasStack.EMPTY;
    }

    public static GasStack executeTransferPlan(IGasHandler sourceCap, GasStack gasType, List<PlannedTransfer> transferPlan) {
        long plannedAmount = 0;
        for (PlannedTransfer plannedTransfer : transferPlan) {
            plannedAmount = Math.min(gasType.getAmount(), plannedAmount + plannedTransfer.amount);
        }
        if (plannedAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack drained = executeSourceDrain(sourceCap, gasType.copyWithAmount(plannedAmount));
        if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, gasType)) {
            return drained;
        }
        return executeTargetPlan(drained, transferPlan);
    }

    public static GasStack executeTargetPlan(GasStack available, List<PlannedTransfer> transferPlan) {
        if (available.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack remainder = available.copy();
        long remainingBudget = remainder.getAmount();
        for (PlannedTransfer plannedTransfer : transferPlan) {
            if (remainingBudget <= 0 || remainder.isEmpty()) {
                break;
            }

            long offeredAmount = Math.min(plannedTransfer.amount, Math.min(remainingBudget, remainder.getAmount()));
            if (offeredAmount <= 0) {
                continue;
            }

            GasStack offered = remainder.copyWithAmount(offeredAmount);
            long filled = plannedTransfer.handler.fill(offered, GasAction.EXECUTE);
            filled = Math.clamp(filled, 0, offeredAmount);
            remainder.shrink(filled);
            remainingBudget -= filled;
        }
        return remainder;
    }

    private static GasStack executeSourceDrain(IGasHandler sourceCap, GasStack request) {
        if (request.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack drained = sourceCap.drain(request, GasAction.EXECUTE);
        if (!drained.isEmpty()) {
            return drained;
        }

        GasStack genericPreview = sourceCap.drain(request.getAmount(), GasAction.SIMULATE);
        if (genericPreview.isEmpty() || !GasStack.isSameGasSameComponents(genericPreview, request)) {
            return GasStack.EMPTY;
        }
        return sourceCap.drain(request.getAmount(), GasAction.EXECUTE);
    }

    public record PlannedTransfer(IGasHandler handler, long amount) {}
}
