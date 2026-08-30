package net.ty.createcraftedbeginning.foundation;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBMathUtils {
    private CCBMathUtils() {
    }

    public static long saturatedAdd(long first, long second) {
        if (second > 0 && first > Long.MAX_VALUE - second) {
            return Long.MAX_VALUE;
        }

        if (second < 0 && first < Long.MIN_VALUE - second) {
            return Long.MIN_VALUE;
        }
        return first + second;
    }

    public static int clampNonNegative(int value, int maximum) {
        return Math.clamp(value, 0, maximum);
    }

    public static int clampNonNegative(long value, int maximum) {
        return Math.clamp(value, 0, maximum);
    }

    public static long clampNonNegative(long value, long maximum) {
        return Math.clamp(value, 0, maximum);
    }

    public static float clampNonNegative(float value, float maximum) {
        return Math.clamp(value, 0, maximum);
    }

    public static double clampNonNegative(double value, double maximum) {
        return Math.clamp(value, 0, maximum);
    }

    public static int clampMagnitude(int value, int maximumMagnitude) {
        return Math.clamp(value, -maximumMagnitude, maximumMagnitude);
    }

    @SuppressWarnings("unused")
    public static long clampMagnitude(long value, long maximumMagnitude) {
        return Math.clamp(value, -maximumMagnitude, maximumMagnitude);
    }

    public static float clampMagnitude(float value, float maximumMagnitude) {
        return Math.clamp(value, -maximumMagnitude, maximumMagnitude);
    }

    @SuppressWarnings("unused")
    public static double clampMagnitude(double value, double maximumMagnitude) {
        return Math.clamp(value, -maximumMagnitude, maximumMagnitude);
    }

    public static int clampUnit(int value) {
        return Math.clamp(value, 0, 1);
    }

    public static long clampUnit(long value) {
        return Math.clamp(value, 0, 1);
    }

    public static float clampUnit(float value) {
        return Math.clamp(value, 0, 1);
    }

    public static double clampUnit(double value) {
        return Math.clamp(value, 0, 1);
    }

    public static int clampToNonNegativeInt(long value) {
        return Math.clamp(value, 0, Integer.MAX_VALUE);
    }
}
