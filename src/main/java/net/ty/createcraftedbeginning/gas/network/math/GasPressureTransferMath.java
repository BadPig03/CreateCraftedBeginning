package net.ty.createcraftedbeginning.gas.network.math;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.foundation.BoundedMath;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureTransferMath {
    private GasPressureTransferMath() {
    }

    public static long maxDrainAmount(long currentAmount, long volume, double currentPressurePa, double manifoldPressurePa) {
        if (manifoldPressurePa >= currentPressurePa) {
            return 0;
        }

        long minimumRemainingAmount = minimumAmountAtOrAbovePressure(volume, manifoldPressurePa);
        return Math.max(0, BoundedMath.saturatedSubtract(currentAmount, minimumRemainingAmount));
    }

    public static long maxFillAmount(long currentAmount, long maxAmount, long volume, double currentPressurePa, double manifoldPressurePa, long maxPressurePa) {
        double pressureCeilingPa = Math.min(manifoldPressurePa, maxPressurePa);
        if (pressureCeilingPa <= currentPressurePa) {
            return 0;
        }

        long maximumStoredAmount = Math.min(maxAmount, maximumAmountAtOrBelowPressure(volume, pressureCeilingPa));
        return Math.max(0, BoundedMath.saturatedSubtract(maximumStoredAmount, currentAmount));
    }

    private static long minimumAmountAtOrAbovePressure(long volume, double pressurePa) {
        if (volume <= 0 || Double.isNaN(pressurePa) || pressurePa <= GasPressure.VACUUM_PA) {
            return 0;
        }

        if (!Double.isFinite(pressurePa)) {
            return Long.MAX_VALUE;
        }

        long candidate = floorNonNegative(GasPressure.amount(volume, pressurePa));
        while (candidate < Long.MAX_VALUE && GasPressure.pressureExact(candidate, volume) < pressurePa) {
            candidate++;
        }
        while (candidate > 0 && GasPressure.pressureExact(candidate - 1, volume) >= pressurePa) {
            candidate--;
        }
        return candidate;
    }

    private static long maximumAmountAtOrBelowPressure(long volume, double pressurePa) {
        if (volume <= 0 || Double.isNaN(pressurePa) || pressurePa <= GasPressure.VACUUM_PA) {
            return 0;
        }

        if (!Double.isFinite(pressurePa)) {
            return Long.MAX_VALUE;
        }

        long candidate = floorNonNegative(GasPressure.amount(volume, pressurePa));
        while (candidate > 0 && GasPressure.pressureExact(candidate, volume) > pressurePa) {
            candidate--;
        }
        while (candidate < Long.MAX_VALUE && GasPressure.pressureExact(candidate + 1, volume) <= pressurePa) {
            candidate++;
        }
        return candidate;
    }

    private static long floorNonNegative(double amount) {
        if (Double.isNaN(amount) || amount <= 0) {
            return 0;
        }

        if (amount >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Mth.lfloor(amount);
    }
}
