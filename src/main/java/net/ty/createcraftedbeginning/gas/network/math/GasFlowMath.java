package net.ty.createcraftedbeginning.gas.network.math;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFlowMath {
    public static final double FLOW_RATE_EPSILON = 1.0E-9;
    private static final double WHOLE_AMOUNT_EPSILON = 1.0E-8;

    private GasFlowMath() {
    }

    public static boolean hasFlow(double flowRate) {
        return !Double.isNaN(flowRate) && flowRate > FLOW_RATE_EPSILON;
    }

    public static long toWholeAmount(double continuousAmount) {
        if (!hasFlow(continuousAmount)) {
            return 0;
        }

        if (continuousAmount >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.max(1, Mth.lfloor(continuousAmount + WHOLE_AMOUNT_EPSILON));
    }

    public static long amountForTickFraction(double flowRate, double tickFraction) {
        if (!hasFlow(flowRate) || tickFraction <= 0 || Double.isNaN(tickFraction)) {
            return 0;
        }

        return toWholeAmount(flowRate * Math.min(1, tickFraction));
    }

    public static long amountForScale(double flowRate, double scale) {
        if (!hasFlow(flowRate) || scale <= 0 || Double.isNaN(scale)) {
            return 0;
        }

        return toWholeAmount(flowRate * scale);
    }

    public static long nextSmallerQuantizedAmount(long amount) {
        if (amount <= 1) {
            return 0;
        }

        return Math.max(1, amount / 2);
    }
}
