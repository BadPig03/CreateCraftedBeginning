package net.ty.createcraftedbeginning.gas.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureTier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureDisplayScale {
    private static final long[] BREAKPOINTS_PA = {
            GasPressure.VACUUM_PA,
            GasPressureTier.LOW.maximumPressurePa(),
            GasPressureTier.MEDIUM.maximumPressurePa(),
            GasPressureTier.HIGH.maximumPressurePa(),
            GasPressureLimits.SAFE_PRESSURE_PA,
            GasPressureLimits.HARD_PRESSURE_PA
    };
    private static final int INTERVALS = BREAKPOINTS_PA.length - 1;

    private GasPressureDisplayScale() {
    }

    public static float fractionForPressure(long pressurePa) {
        long normalizedPressurePa = GasPressureLimits.clampToHardLimit(pressurePa);
        if (normalizedPressurePa <= BREAKPOINTS_PA[0]) {
            return 0;
        }

        for (int upperIndex = 1; upperIndex < BREAKPOINTS_PA.length; upperIndex++) {
            long upperPressurePa = BREAKPOINTS_PA[upperIndex];
            if (normalizedPressurePa > upperPressurePa) {
                continue;
            }

            long lowerPressurePa = BREAKPOINTS_PA[upperIndex - 1];
            double intervalFraction = (double) (normalizedPressurePa - lowerPressurePa) / (upperPressurePa - lowerPressurePa);
            double scalePosition = upperIndex - 1 + intervalFraction;
            return Mth.clamp((float) (scalePosition / INTERVALS), 0, 1);
        }
        return 1;
    }
}
