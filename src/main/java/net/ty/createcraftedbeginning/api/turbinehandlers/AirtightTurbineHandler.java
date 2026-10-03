package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightTurbineHandler {
    int MAX_LEVEL = 16;

    long BASE_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    long MAX_LEVEL_PRESSURE_PA = 10 * GasPressure.REFERENCE_PRESSURE_PA;

    float getBaseLevel();

    float getMaxLevel();

    default float getLevel(long sourcePressurePa) {
        float baseLevel = getBaseLevel();
        float maxLevel = getMaxLevel();
        double pressureProgress = Mth.clamp(((double) sourcePressurePa - BASE_PRESSURE_PA) / (MAX_LEVEL_PRESSURE_PA - BASE_PRESSURE_PA), 0, 1);
        return (float) (baseLevel + (maxLevel - baseLevel) * pressureProgress);
    }
}
