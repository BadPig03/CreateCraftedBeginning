package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressure {
    public static final long UNITS_PER_PRESSURE = 1048576L;
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
        return isZero(units) ? 0 : units;
    }

    public static float toPressure(long units) {
        if (units <= 0) {
            return 0;
        }
        return (float) units / UNITS_PER_PRESSURE;
    }

    public static boolean isZero(long units) {
        return units >= -ZERO_EPSILON_UNITS && units <= ZERO_EPSILON_UNITS;
    }

    public static long addSaturated(long first, long second) {
        if (second <= 0) {
            return Math.max(0, first);
        }

        if (first >= Long.MAX_VALUE - second) {
            return Long.MAX_VALUE;
        }
        return Math.max(0, first) + second;
    }

    public static long splitShare(long totalUnits, int partCount, int partIndex) {
        if (totalUnits <= 0 || partIndex < 0 || partIndex >= partCount) {
            return 0;
        }

        long equalShare = totalUnits / partCount;
        long remainder = totalUnits % partCount;
        return equalShare + (partIndex < remainder ? 1 : 0);
    }
}
