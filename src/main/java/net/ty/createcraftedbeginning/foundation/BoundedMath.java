package net.ty.createcraftedbeginning.foundation;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigInteger;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BoundedMath {
    private BoundedMath() {
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

    public static long saturatedSubtract(long first, long second) {
        if (second > 0 && first < Long.MIN_VALUE + second) {
            return Long.MIN_VALUE;
        }

        if (second < 0 && first > Long.MAX_VALUE + second) {
            return Long.MAX_VALUE;
        }

        return first - second;
    }

    public static long saturatedMultiply(long first, long second) {
        try {
            return Math.multiplyExact(first, second);
        }
        catch (ArithmeticException ignored) {
            if (first < 0 == second < 0) {
                return Long.MAX_VALUE;
            }

            return Long.MIN_VALUE;
        }
    }

    public static long sumNonNegative(long[] values) {
        long sum = 0;
        for (long value : values) {
            sum = saturatedAdd(sum, Math.max(0, value));
        }
        return sum;
    }

    public static long saturatedMultiplyDivideNonNegative(long first, long second, long divisor) {
        if (first < 0 || second < 0) {
            throw new IllegalArgumentException("Operands must be non-negative; got first=" + first + ", second=" + second + '.');
        }

        if (divisor <= 0) {
            throw new IllegalArgumentException("Divisor must be positive; got " + divisor + '.');
        }

        if (first == 0 || second == 0) {
            return 0;
        }

        if (first <= Long.MAX_VALUE / second) {
            return first * second / divisor;
        }

        BigInteger result = BigInteger.valueOf(first).multiply(BigInteger.valueOf(second)).divide(BigInteger.valueOf(divisor));
        if (result.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0) {
            return Long.MAX_VALUE;
        }

        return result.longValue();
    }

    public static long saturatedMultiplyDivideCeilNonNegative(long first, long second, long divisor) {
        if (first < 0 || second < 0) {
            throw new IllegalArgumentException("Operands must be non-negative; got first=" + first + ", second=" + second + '.');
        }

        if (divisor <= 0) {
            throw new IllegalArgumentException("Divisor must be positive; got " + divisor + '.');
        }

        if (first == 0 || second == 0) {
            return 0;
        }

        if (first <= Long.MAX_VALUE / second) {
            long product = first * second;
            long quotient = product / divisor;
            if (product % divisor == 0) {
                return quotient;
            }

            return quotient + 1;
        }

        BigInteger numerator = BigInteger.valueOf(first).multiply(BigInteger.valueOf(second));
        BigInteger[] division = numerator.divideAndRemainder(BigInteger.valueOf(divisor));
        BigInteger result = division[1].signum() == 0 ? division[0] : division[0].add(BigInteger.ONE);
        if (result.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0) {
            return Long.MAX_VALUE;
        }

        return result.longValue();
    }

    public static int clampMagnitude(int value, int maximumMagnitude) {
        return Mth.clamp(value, -maximumMagnitude, maximumMagnitude);
    }

    public static long clampMagnitude(long value, long maximumMagnitude) {
        return Mth.clamp(value, -maximumMagnitude, maximumMagnitude);
    }

    public static float clampMagnitude(float value, float maximumMagnitude) {
        return Mth.clamp(value, -maximumMagnitude, maximumMagnitude);
    }

    public static int clampToNonNegativeInt(long value) {
        return (int) Mth.clamp(value, 0, Integer.MAX_VALUE);
    }
}
