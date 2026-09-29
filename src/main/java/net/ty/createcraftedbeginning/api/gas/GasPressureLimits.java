package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Global engineering pressure boundaries for Create: Crafted Beginning's gas system.
 *
 * <p>The rated pressure is the highest pressure considered normal operation. The hard pressure is
 * the absolute physical ceiling used by storage and transport infrastructure. The hard ceiling does
 * not create an additional recipe pressure tier.</p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureLimits {
    public static final int SAFE_PRESSURE_ATM = 16;
    public static final int HARD_PRESSURE_ATM = 24;

    public static final long SAFE_PRESSURE_PA = GasPressure.pascals(SAFE_PRESSURE_ATM);
    public static final long HARD_PRESSURE_PA = GasPressure.pascals(HARD_PRESSURE_ATM);

    private GasPressureLimits() {
    }

    public static long clampToHardLimit(long pressurePa) {
        return Math.clamp(pressurePa, GasPressure.VACUUM_PA, HARD_PRESSURE_PA);
    }

    public static double clampToHardLimit(double pressurePa) {
        if (Double.isNaN(pressurePa) || pressurePa <= GasPressure.VACUUM_PA) {
            return GasPressure.VACUUM_PA;
        }

        return Math.min(HARD_PRESSURE_PA, pressurePa);
    }

    public static boolean isOverpressure(long pressurePa) {
        return pressurePa > SAFE_PRESSURE_PA;
    }
}
