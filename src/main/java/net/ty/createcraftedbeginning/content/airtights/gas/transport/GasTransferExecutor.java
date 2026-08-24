package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasTransferExecutor {
    private GasTransferExecutor() {
    }

    static GasStack simulateSourceDrain(IGasHandler sourceHandler, GasStack gasType, long maxAmount) {
        if (maxAmount <= 0 || gasType.isEmpty()) {
            return GasStack.EMPTY;
        }

        for (int tankIndex = 0; tankIndex < sourceHandler.getTanks(); tankIndex++) {
            GasStack tankGas = sourceHandler.getGasInTank(tankIndex);
            if (tankGas.isEmpty() || !GasStack.isSameGasSameComponents(tankGas, gasType)) {
                continue;
            }

            GasStack simulatedDrain = sourceHandler.drain(tankGas.copyWithAmount(maxAmount), GasAction.SIMULATE);
            if (simulatedDrain.isEmpty()) {
                break;
            }

            return GasStack.isSameGasSameComponents(simulatedDrain, gasType) ? simulatedDrain : GasStack.EMPTY;
        }

        GasStack simulatedDrain = sourceHandler.drain(maxAmount, GasAction.SIMULATE);
        return !simulatedDrain.isEmpty() && GasStack.isSameGasSameComponents(simulatedDrain, gasType) ? simulatedDrain : GasStack.EMPTY;
    }

    static GasStack executeTransferPlan(IGasHandler sourceHandler, GasStack gasType, List<PlannedTransfer> transferPlan) {
        long plannedAmount = 0;
        for (PlannedTransfer plannedTransfer : transferPlan) {
            plannedAmount = Math.min(gasType.getAmount(), plannedAmount + plannedTransfer.amount);
        }
        if (plannedAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack drainedGas = executeSourceDrain(sourceHandler, gasType.copyWithAmount(plannedAmount));
        if (drainedGas.isEmpty() || !GasStack.isSameGasSameComponents(drainedGas, gasType)) {
            return drainedGas;
        }
        return executeTargetPlan(drainedGas, transferPlan);
    }

    static GasStack executeTargetPlan(GasStack availableGas, List<PlannedTransfer> transferPlan) {
        if (availableGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack remainingGas = availableGas.copy();
        long remainingBudget = remainingGas.getAmount();
        for (PlannedTransfer plannedTransfer : transferPlan) {
            if (remainingBudget <= 0 || remainingGas.isEmpty()) {
                break;
            }

            long offeredAmount = Math.min(plannedTransfer.amount, Math.min(remainingBudget, remainingGas.getAmount()));
            if (offeredAmount <= 0) {
                continue;
            }

            long filledAmount = plannedTransfer.handler.fill(remainingGas.copyWithAmount(offeredAmount), GasAction.EXECUTE);
            filledAmount = Math.clamp(filledAmount, 0, offeredAmount);
            remainingGas.shrink(filledAmount);
            remainingBudget -= filledAmount;
        }
        return remainingGas;
    }

    private static GasStack executeSourceDrain(IGasHandler sourceHandler, GasStack drainRequest) {
        if (drainRequest.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack drainedGas = sourceHandler.drain(drainRequest, GasAction.EXECUTE);
        if (!drainedGas.isEmpty()) {
            return drainedGas;
        }

        GasStack genericDrainPreview = sourceHandler.drain(drainRequest.getAmount(), GasAction.SIMULATE);
        if (genericDrainPreview.isEmpty() || !GasStack.isSameGasSameComponents(genericDrainPreview, drainRequest)) {
            return GasStack.EMPTY;
        }
        return sourceHandler.drain(drainRequest.getAmount(), GasAction.EXECUTE);
    }

    record PlannedTransfer(IGasHandler handler, long amount) {}
}
