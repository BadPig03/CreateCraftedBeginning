package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.minecraft.MethodsReturnNonnullByDefault;

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
}
