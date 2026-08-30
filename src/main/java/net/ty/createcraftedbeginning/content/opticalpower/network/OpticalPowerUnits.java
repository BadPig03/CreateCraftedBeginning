package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalPowerUnits {
    public static final int SU_PER_POWER_POINT = 256;

    private OpticalPowerUnits() {
    }

    public static int toPowerPoints(int su) {
        return Math.max(0, su) / SU_PER_POWER_POINT;
    }

    public static int toSu(int powerPoints) {
        long su = (long) Math.max(0, powerPoints) * SU_PER_POWER_POINT;
        return CCBMathUtils.clampToNonNegativeInt(su);
    }
}
