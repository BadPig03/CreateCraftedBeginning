package net.ty.createcraftedbeginning.gas.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFlowResistance {
    public static final long UNITS_PER_STANDARD_SEGMENT = 1000000;
    public static final long MIN_RESISTANCE_UNITS = 1;

    private static final long BASE_CONDUCTANCE_PER_ATMOSPHERE = 1000;

    private GasFlowResistance() {
    }

    public static double conductancePerPascal(long resistanceUnits) {
        double numerator = BASE_CONDUCTANCE_PER_ATMOSPHERE * UNITS_PER_STANDARD_SEGMENT;
        double denominator = GasPressure.REFERENCE_PRESSURE_PA * Math.max(MIN_RESISTANCE_UNITS, resistanceUnits);
        return numerator / denominator;
    }

    public static long fromFactor(double resistanceFactor) {
        if (!Double.isFinite(resistanceFactor) || resistanceFactor <= 0) {
            return MIN_RESISTANCE_UNITS;
        }

        double scaled = resistanceFactor * UNITS_PER_STANDARD_SEGMENT;
        if (!Double.isFinite(scaled) || scaled >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.max(MIN_RESISTANCE_UNITS, Math.round(scaled));
    }
}
