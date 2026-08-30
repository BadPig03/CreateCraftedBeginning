package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressure {
    static final long UNITS_PER_PRESSURE = 1048576;
    private static final long ZERO_EPSILON_UNITS = 8;

    private GasPressure() {
    }

    public static long toUnits(float pressure) {
        if (!GasConsumptions.isFinite(pressure) || pressure <= 0) {
            return 0;
        }

        double scaled = pressure * UNITS_PER_PRESSURE;
        if (!GasConsumptions.isFinite(scaled) || scaled >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        long units = Math.round(scaled);
        if (isZero(units)) {
            return 0;
        }
        return units;
    }

    public static long splitShare(long totalUnits, int partCount, int partIndex) {
        if (totalUnits <= 0 || partIndex < 0 || partIndex >= partCount) {
            return 0;
        }

        long equalShare = totalUnits / partCount;
        long remainder = totalUnits % partCount;
        if (partIndex >= remainder) {
            return equalShare;
        }
        return equalShare + 1;
    }

    static boolean isZero(long units) {
        return units >= -ZERO_EPSILON_UNITS && units <= ZERO_EPSILON_UNITS;
    }
}
