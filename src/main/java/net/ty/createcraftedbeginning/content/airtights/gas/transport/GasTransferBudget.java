package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasTransferBudget {
    private static final long PRESSURE_UNITS_PER_GAS = GasPressure.UNITS_PER_PRESSURE * 2;

    private GasTransferBudget() {
    }

    static Step consume(long pressureRateUnits, long creditUnits) {
        if (pressureRateUnits <= 0) {
            return new Step(0, 0);
        }

        long transferBudget = pressureRateUnits / PRESSURE_UNITS_PER_GAS;
        long remainderUnits = pressureRateUnits % PRESSURE_UNITS_PER_GAS;
        long accumulatedCreditUnits = CCBMathUtils.saturatedAdd(Math.max(0, creditUnits), remainderUnits);
        if (accumulatedCreditUnits >= PRESSURE_UNITS_PER_GAS) {
            transferBudget++;
            accumulatedCreditUnits -= PRESSURE_UNITS_PER_GAS;
        }
        return new Step(transferBudget, accumulatedCreditUnits);
    }

    record Step(long budget, long creditUnits) {}
}
