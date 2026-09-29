package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.foundation.BoundedMath;

import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigInteger;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasAmountDistribution {
    private GasAmountDistribution() {
    }

    public static long[] allocateByWeight(long total, long[] limits, long[] weights, int cursor) {
        if (limits.length != weights.length) {
            throw new IllegalArgumentException("Limits and weights must have the same length; got " + limits.length + " limits and " + weights.length + " weights.");
        }

        long[] allocations = new long[limits.length];
        if (total <= 0 || limits.length == 0) {
            return allocations;
        }

        long limitSum = BoundedMath.sumNonNegative(limits);
        long remaining = Math.min(total, limitSum);
        boolean[] active = new boolean[limits.length];
        for (int index = 0; index < limits.length; index++) {
            active[index] = limits[index] > 0 && weights[index] > 0;
        }

        while (remaining > 0) {
            ExactSum weightSum = new ExactSum();
            int activeCount = 0;
            for (int index = 0; index < active.length; index++) {
                if (!active[index]) {
                    continue;
                }

                weightSum.add(weights[index]);
                activeCount++;
            }
            if (activeCount == 0) {
                break;
            }

            long passBudget = remaining;
            long distributed = 0;
            for (int offset = 0; offset < limits.length; offset++) {
                int index = Math.floorMod((long) cursor + offset, limits.length);
                if (!active[index]) {
                    continue;
                }

                long headroom = BoundedMath.saturatedSubtract(limits[index], allocations[index]);
                if (headroom <= 0) {
                    active[index] = false;
                    continue;
                }

                long share = weightSum.share(passBudget, weights[index]);
                long give = Math.min(passBudget - distributed, Math.min(headroom, share));
                if (give <= 0) {
                    continue;
                }

                allocations[index] += give;
                distributed += give;
                if (allocations[index] < limits[index]) {
                    continue;
                }

                active[index] = false;
            }

            remaining -= distributed;
            if (remaining <= 0) {
                break;
            }

            if (distributed > 0) {
                continue;
            }

            boolean gaveRemainder = false;
            for (int offset = 0; offset < limits.length && remaining > 0; offset++) {
                int index = Math.floorMod((long) cursor + offset, limits.length);
                if (!active[index]) {
                    continue;
                }

                long headroom = BoundedMath.saturatedSubtract(limits[index], allocations[index]);
                if (headroom <= 0) {
                    active[index] = false;
                    continue;
                }

                allocations[index]++;
                remaining--;
                gaveRemainder = true;
                if (allocations[index] < limits[index]) {
                    continue;
                }

                active[index] = false;
            }
            if (gaveRemainder) {
                continue;
            }

            break;
        }
        return allocations;
    }

    public static long[] allocateProportionally(long total, long[] limits) {
        long[] allocations = new long[limits.length];
        if (total <= 0 || limits.length == 0) {
            return allocations;
        }

        ExactSum limitSum = new ExactSum();
        for (long limit : limits) {
            limitSum.add(Math.max(0, limit));
        }
        if (limitSum.saturatedValue() <= 0) {
            return allocations;
        }

        long budget = Math.min(total, limitSum.saturatedValue());
        long distributed = 0;
        for (int index = 0; index < limits.length; index++) {
            long limit = Math.max(0, limits[index]);
            if (limit <= 0) {
                continue;
            }

            long share = limitSum.share(budget, limit);
            allocations[index] = Math.min(budget - distributed, Math.min(limit, share));
            distributed += allocations[index];
        }

        long remaining = BoundedMath.saturatedSubtract(budget, distributed);
        for (int index = 0; index < limits.length && remaining > 0; index++) {
            long limit = Math.max(0, limits[index]);
            long headroom = BoundedMath.saturatedSubtract(limit, allocations[index]);
            if (headroom <= 0) {
                continue;
            }

            long give = Math.min(headroom, remaining);
            allocations[index] = BoundedMath.saturatedAdd(allocations[index], give);
            remaining = BoundedMath.saturatedSubtract(remaining, give);
        }
        return allocations;
    }

    private static final class ExactSum {
        private long value;
        private BigInteger largeValue;

        private void add(long amount) {
            if (largeValue != null) {
                largeValue = largeValue.add(BigInteger.valueOf(amount));
                return;
            }

            if (amount <= Long.MAX_VALUE - value) {
                value += amount;
                return;
            }

            largeValue = BigInteger.valueOf(value).add(BigInteger.valueOf(amount));
        }

        private long saturatedValue() {
            if (largeValue == null) {
                return value;
            }

            return Long.MAX_VALUE;
        }

        private long share(long total, long weight) {
            if (largeValue == null) {
                return BoundedMath.saturatedMultiplyDivideNonNegative(total, weight, value);
            }

            return BigInteger.valueOf(total).multiply(BigInteger.valueOf(weight)).divide(largeValue).longValueExact();
        }
    }

}
