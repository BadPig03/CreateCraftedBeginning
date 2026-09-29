package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.BoundedMath;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressure {
    public static final long REFERENCE_PRESSURE_PA = 100000;
    public static final long VACUUM_PA = 0;

    private GasPressure() {
    }

    public static String format(long pressurePa) {
        if (pressurePa < 1000) {
            return pressurePa + " Pa";
        }

        if (pressurePa < 1000000) {
            return String.format(Locale.ROOT, "%.2f kPa", pressurePa / 1000.0);
        }

        if (pressurePa < 1000000000) {
            return String.format(Locale.ROOT, "%.2f MPa", pressurePa / 1000000.0);
        }

        return String.format(Locale.ROOT, "%.2f GPa", pressurePa / 1000000000.0);
    }

    public static String formatAtm(long pressurePa) {
        return String.format(Locale.ROOT, "%.2f atm", atmospheres(pressurePa));
    }

    public static long pressure(long gasAmount, long volumeLiters) {
        if (gasAmount <= 0) {
            return VACUUM_PA;
        }

        if (volumeLiters <= 0) {
            return Long.MAX_VALUE;
        }

        return BoundedMath.saturatedMultiplyDivideNonNegative(gasAmount, REFERENCE_PRESSURE_PA, volumeLiters);
    }

    public static double pressureExact(long gasAmount, long volumeLiters) {
        if (gasAmount <= 0) {
            return VACUUM_PA;
        }

        if (volumeLiters <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        return (double) gasAmount * REFERENCE_PRESSURE_PA / volumeLiters;
    }

    public static long amount(long volumeLiters, long pressurePa) {
        if (volumeLiters <= 0 || pressurePa <= 0) {
            return 0;
        }

        return BoundedMath.saturatedMultiplyDivideNonNegative(volumeLiters, pressurePa, REFERENCE_PRESSURE_PA);
    }

    public static double amount(long volumeLiters, double pressurePa) {
        if (pressurePa <= 0 || Double.isNaN(pressurePa)) {
            return 0;
        }

        double gasAmount = pressurePa * Math.max(0, volumeLiters) / REFERENCE_PRESSURE_PA;
        if (!Double.isFinite(gasAmount)) {
            return Double.POSITIVE_INFINITY;
        }

        return gasAmount;
    }

    public static long minimumAmountForPressure(long volumeLiters, long pressurePa) {
        if (volumeLiters <= 0 || pressurePa <= VACUUM_PA) {
            return 0;
        }

        return BoundedMath.saturatedMultiplyDivideCeilNonNegative(volumeLiters, pressurePa, REFERENCE_PRESSURE_PA);
    }

    public static long amountAbovePressureFloor(long storedAmount, long volumeLiters, long minimumPressurePa) {
        if (storedAmount <= 0) {
            return 0;
        }

        long reservedAmount = minimumAmountForPressure(volumeLiters, minimumPressurePa);
        if (storedAmount <= reservedAmount) {
            return 0;
        }

        return storedAmount - reservedAmount;
    }

    public static double atmospheres(long pressurePa) {
        if (pressurePa <= 0) {
            return 0;
        }

        return (double) pressurePa / REFERENCE_PRESSURE_PA;
    }

    public static long pascals(double atmospheres) {
        if (atmospheres <= 0) {
            return VACUUM_PA;
        }

        if (!Double.isFinite(atmospheres) || atmospheres >= (double) Long.MAX_VALUE / REFERENCE_PRESSURE_PA) {
            return Long.MAX_VALUE;
        }

        return Math.max(0, Math.round(atmospheres * REFERENCE_PRESSURE_PA));
    }

    public static long floor(double pressurePa) {
        if (Double.isNaN(pressurePa) || pressurePa <= VACUUM_PA) {
            return VACUUM_PA;
        }

        if (pressurePa >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Mth.lfloor(pressurePa);
    }

    public static long ceil(double pressurePa) {
        if (pressurePa <= VACUUM_PA) {
            return VACUUM_PA;
        }

        if (Double.isNaN(pressurePa) || pressurePa >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return (long) Math.ceil(pressurePa);
    }

    public static long round(double pressurePa) {
        if (Double.isNaN(pressurePa) || pressurePa <= VACUUM_PA) {
            return VACUUM_PA;
        }

        if (pressurePa >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.round(pressurePa);
    }
}
