package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTransferBudget {
    private static final long PRESSURE_UNITS_PER_GAS = GasPressure.UNITS_PER_PRESSURE * 2;

    private GasTransferBudget() {
    }

    public static Step consume(long pressureRateUnits, long creditUnits) {
        if (pressureRateUnits <= 0) {
            return new Step(0, 0);
        }

        long budget = pressureRateUnits / PRESSURE_UNITS_PER_GAS;
        long remainder = pressureRateUnits % PRESSURE_UNITS_PER_GAS;
        long accumulatedRemainder = Math.max(0, creditUnits) + remainder;
        if (accumulatedRemainder >= PRESSURE_UNITS_PER_GAS) {
            budget++;
            accumulatedRemainder -= PRESSURE_UNITS_PER_GAS;
        }
        return new Step(budget, accumulatedRemainder);
    }

    public record Step(long budget, long creditUnits) {}
}
