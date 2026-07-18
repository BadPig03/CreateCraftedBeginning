package net.ty.createcraftedbeginning.api.gascanisters;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class GasConsumptionUtils {
    private static final double INTEGER_TOLERANCE = 1.0e-9;
    private static final long MAX_SAFE_GAS_COST = 92233720368547757L;
    private static final int DISPLAY_SCALE = 2;

    private GasConsumptionUtils() {
    }

    /**
     * Rounds the supplied gas amount up to the next whole unit.
     *
     * @param rawAmount the raw amount value to use
     * @return the round up value
     */
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

    /**
     * Rounds the supplied gas amount up and clamps it to an integer.
     *
     * @param rawAmount the raw amount value to use
     * @return the round up to int value
     */
    public static int roundUpToInt(double rawAmount) {
        long rounded = roundUp(rawAmount);
        if (rounded < 0) {
            return -1;
        }
        return rounded >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rounded;
    }

    /**
     * Checks whether the supplied value is finite.
     *
     * @param value the value to inspect or process
     * @return {@code true} if the supplied value is finite; otherwise {@code false}
     */
    public static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    /**
     * Checks whether the supplied value is finite and non-negative.
     *
     * @param value the value to inspect or process
     * @return {@code true} if the supplied value is finite and non-negative; otherwise {@code false}
     */
    public static boolean isNonNegativeFinite(double value) {
        return isFinite(value) && value >= 0;
    }

    /**
     * Checks whether the supplied value is non-negative.
     *
     * @param value the value to inspect or process
     * @return {@code true} if the supplied value is non-negative; otherwise {@code false}
     */
    public static boolean isNonNegative(int value) {
        return value >= 0;
    }

    /**
     * Formats the supplied value for display.
     *
     * @param value the value to inspect or process
     * @return the formatted text
     */
    public static String format(double value) {
        if (!Double.isFinite(value)) {
            return Double.toString(value);
        }

        return BigDecimal.valueOf(value).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * Formats the supplied value as a percentage.
     *
     * @param multiplier the multiplier to apply
     * @return the formatted text
     */
    public static String formatPercent(double multiplier) {
        return format(multiplier * 100);
    }
}
