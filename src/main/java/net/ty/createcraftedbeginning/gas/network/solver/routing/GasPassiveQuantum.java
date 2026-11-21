package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigInteger;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPassiveQuantum {
    private GasPassiveQuantum() {
    }

    public static long limit(PressureState source, PressureState target, long capacity) {
        if (source.fixedPressure() && target.fixedPressure()) {
            if (source.pressurePa() > target.pressurePa()) {
                return capacity;
            }

            return 0;
        }
        if (source.fixedPressure()) {
            return nearest(GasPressure.round(source.pressurePa()), target.volume(), target.currentAmount(), GasPressure.REFERENCE_PRESSURE_PA, GasPressure.REFERENCE_PRESSURE_PA, 0, capacity);
        }
        if (target.fixedPressure()) {
            return nearest(source.currentAmount(), GasPressure.REFERENCE_PRESSURE_PA, GasPressure.round(target.pressurePa()), source.volume(), GasPressure.REFERENCE_PRESSURE_PA, 0, capacity);
        }
        return nearest(source.currentAmount(), target.volume(), target.currentAmount(), source.volume(), source.volume(), target.volume(), capacity);
    }

    private static long nearest(long a, long b, long c, long d, long firstWeight, long secondWeight, long capacity) {
        long left = a * b;
        long right = c * d;
        long weight = firstWeight + secondWeight;
        if (left >= 0 && right >= 0 && weight > 0 && Math.multiplyHigh(a, b) == 0 && Math.multiplyHigh(c, d) == 0) {
            if (left <= right) {
                return 0;
            }

            long difference = left - right;
            long remainder = difference % weight;
            return Math.min(capacity, difference / weight + (remainder > weight - remainder ? 1 : 0));
        }

        BigInteger difference = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).subtract(BigInteger.valueOf(c).multiply(BigInteger.valueOf(d)));
        if (difference.signum() <= 0) {
            return 0;
        }

        BigInteger divisor = BigInteger.valueOf(firstWeight).add(BigInteger.valueOf(secondWeight));
        BigInteger[] parts = difference.divideAndRemainder(divisor);
        BigInteger amount = parts[1].shiftLeft(1).compareTo(divisor) > 0 ? parts[0].add(BigInteger.ONE) : parts[0];
        return amount.min(BigInteger.valueOf(capacity)).longValueExact();
    }
}
