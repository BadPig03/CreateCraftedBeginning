package net.ty.createcraftedbeginning.api.gascanisters;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasConsumptions {
    private static final double INTEGER_TOLERANCE = 1.0e-9;
    private static final long MAX_SAFE_GAS_COST = 92233720368547757L;
    private static final int DISPLAY_SCALE = 2;

    private GasConsumptions() {
    }

    public static long roundUp(double rawAmount) {
        if (!isNonNegativeFinite(rawAmount)) {
            return -1;
        }

        if (rawAmount == 0) {
            return 0;
        }

        double nearestInteger = Math.rint(rawAmount);
        double tolerance = INTEGER_TOLERANCE * Math.max(1, Math.abs(rawAmount));
        if (nearestInteger >= 1 && Math.abs(rawAmount - nearestInteger) <= tolerance) {
            rawAmount = nearestInteger;
        }
        if (rawAmount > MAX_SAFE_GAS_COST) {
            return -1;
        }
        return (long) Math.ceil(rawAmount);
    }

    @SuppressWarnings("unused")
    public static int roundUpToInt(double rawAmount) {
        long rounded = roundUp(rawAmount);
        if (rounded < 0) {
            return -1;
        }
        return Math.clamp(rounded, 0, Integer.MAX_VALUE);
    }

    public static boolean isFinite(float value) {
        return Float.isFinite(value);
    }

    public static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    public static boolean isNonNegativeFinite(double value) {
        return isFinite(value) && value >= 0;
    }

    public static boolean isNonNegative(int value) {
        return value >= 0;
    }

    public static String format(double value) {
        if (!isFinite(value)) {
            return Double.toString(value);
        }
        return BigDecimal.valueOf(value).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static String formatPercent(double multiplier) {
        return format(multiplier * 100);
    }
}
