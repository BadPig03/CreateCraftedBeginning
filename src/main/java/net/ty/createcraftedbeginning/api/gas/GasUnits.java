package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.foundation.BoundedMath;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasUnits {
    public static final long GU_PER_KGU = 1000;
    public static final long GU_PER_MGU = GU_PER_KGU * 1000;
    public static final long GU_PER_GGU = GU_PER_MGU * 1000;
    public static final long LITERS_PER_KILOLITER = 1000;

    private GasUnits() {
    }

    public static int toInt(long gasUnits) {
        return BoundedMath.clampToNonNegativeInt(gasUnits);
    }

    public static int toKilo(long gasUnits) {
        return BoundedMath.clampToNonNegativeInt(gasUnits / GU_PER_KGU);
    }
}
