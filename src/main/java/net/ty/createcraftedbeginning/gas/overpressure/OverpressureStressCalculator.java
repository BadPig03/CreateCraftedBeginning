package net.ty.createcraftedbeginning.gas.overpressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OverpressureStressCalculator {
    public static final int TICKS_PER_SECOND = 20;
    public static final int HARD_LIMIT_FAILURE_TICKS = 5 * TICKS_PER_SECOND;
    public static final int FULL_RECOVERY_TICKS = 15 * TICKS_PER_SECOND;
    public static final int STABILIZATION_GRACE_TICKS = 40;

    public static final double MIN_STRESS = 0;
    public static final double MAX_STRESS = 1;

    private static final double HARD_LIMIT_STRESS_PER_TICK = MAX_STRESS / HARD_LIMIT_FAILURE_TICKS;
    private static final double RECOVERY_PER_TICK = MAX_STRESS / FULL_RECOVERY_TICKS;
    private static final long OVERPRESSURE_RANGE_PA = GasPressureLimits.HARD_PRESSURE_PA - GasPressureLimits.SAFE_PRESSURE_PA;

    private OverpressureStressCalculator() {
    }

    public static double normalizedOverpressure(long pressurePa) {
        if (pressurePa <= GasPressureLimits.SAFE_PRESSURE_PA) {
            return 0;
        }

        if (pressurePa >= GasPressureLimits.HARD_PRESSURE_PA) {
            return 1;
        }

        return (double) (pressurePa - GasPressureLimits.SAFE_PRESSURE_PA) / OVERPRESSURE_RANGE_PA;
    }

    public static double stressIncreasePerTick(long pressurePa) {
        double normalized = normalizedOverpressure(pressurePa);
        return normalized * normalized * HARD_LIMIT_STRESS_PER_TICK;
    }

    @SuppressWarnings("unused")
    public static double stressRecoveryPerTick() {
        return RECOVERY_PER_TICK;
    }

    public static double advance(double currentStress, long pressurePa) {
        double stress = sanitizeStress(currentStress);
        if (pressurePa > GasPressureLimits.SAFE_PRESSURE_PA) {
            return sanitizeStress(stress + stressIncreasePerTick(pressurePa));
        }

        return sanitizeStress(stress - RECOVERY_PER_TICK);
    }

    public static double sanitizeStress(double stress) {
        if (!Double.isFinite(stress) || stress <= MIN_STRESS) {
            return MIN_STRESS;
        }

        return Math.min(MAX_STRESS, stress);
    }
}
