package net.ty.createcraftedbeginning.gas.storage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasTankLimits(long volumeLiters, long maxPressurePa) {
    public GasTankLimits {
        if (volumeLiters < 0) {
            throw new IllegalArgumentException("Gas tank volume must be non-negative; got " + volumeLiters + " L.");
        }

        if (maxPressurePa < 0) {
            throw new IllegalArgumentException("Gas tank maximum pressure must be non-negative; got " + maxPressurePa + " Pa.");
        }

        if (maxPressurePa > GasPressureLimits.HARD_PRESSURE_PA) {
            throw new IllegalArgumentException("Gas tank maximum pressure must not exceed " + GasPressureLimits.HARD_PRESSURE_PA + " Pa; got " + maxPressurePa + " Pa.");
        }
    }

    public static GasTankLimits atReferencePressure(long volumeLiters) {
        return new GasTankLimits(volumeLiters, GasPressure.REFERENCE_PRESSURE_PA);
    }

    public long maxAmount() {
        return GasPressure.amount(volumeLiters, maxPressurePa);
    }
}
