package net.ty.createcraftedbeginning.content.airtights.balloon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonPackingLimits {
    private BalloonPackingLimits() {
    }

    public static long getBaseAmount() {
        return Math.max(0, CCBConfig.server().machines.gasPackager.referenceGasPerBalloon.get()) * GasUnits.GU_PER_KGU;
    }

    public static long getLocalPackingLimit(Level level, BlockPos pos) {
        return getLocalPackingLimit(BalloonPressureSemantics.ambientPressurePa(level, pos));
    }

    public static long getLocalPackingLimit(long ambientPressurePa) {
        long baseAmount = getBaseAmount();
        if (baseAmount <= 0 || ambientPressurePa <= GasPressure.VACUUM_PA) {
            return 0;
        }

        return GasPressure.amount(baseAmount, ambientPressurePa);
    }
}
