package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasTags;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBGasTags;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionFanCost {

    private GasInjectionFanCost() {
    }

    public static int getFanProcessingPressureEfficiencyDivisor(long sourcePressurePa) {
        if (GameplayPressureProfiles.isAtLeast(sourcePressurePa, GameplayPressureProfiles.HIGH_PRESSURE)) {
            return 2;
        }

        return 1;
    }

    static long getFanProcessingGasCost(GasStack gas, long sourcePressurePa, int itemCount) {
        if (gas.isEmpty() || itemCount <= 0 || GasTags.isTag(gas, CCBGasTags.CREATIVE.tag)) {
            return 0;
        }

        int efficiencyDivisor = 1;
        if (GasTags.isTag(gas, CCBGasTags.ENERGIZED.tag)) {
            efficiencyDivisor *= 5;
        }
        efficiencyDivisor *= getFanProcessingPressureEfficiencyDivisor(sourcePressurePa);
        long baseCost = (long) CCBConfig.server().machines.gasInjectionChamber.fanProcessingGasPerItem.get() * itemCount;
        if (baseCost % efficiencyDivisor == 0) {
            return baseCost / efficiencyDivisor;
        }

        return baseCost / efficiencyDivisor + 1;
    }

    static int getMaxFanProcessingBatchSize(GasStack gas, long sourcePressurePa, int desiredCount, long gasBudget) {
        if (gas.isEmpty() || desiredCount <= 0) {
            return 0;
        }

        if (GasTags.isTag(gas, CCBGasTags.CREATIVE.tag) || CCBConfig.server().machines.gasInjectionChamber.fanProcessingGasPerItem.get() <= 0) {
            return desiredCount;
        }

        if (gasBudget <= 0) {
            return 0;
        }

        int minimumBatchSize = 0;
        int maximumBatchSize = desiredCount;
        while (minimumBatchSize < maximumBatchSize) {
            int candidateBatchSize = minimumBatchSize + (maximumBatchSize - minimumBatchSize + 1) / 2;
            if (getFanProcessingGasCost(gas, sourcePressurePa, candidateBatchSize) > gasBudget) {
                maximumBatchSize = candidateBatchSize - 1;
                continue;
            }

            minimumBatchSize = candidateBatchSize;
        }
        return minimumBatchSize;
    }
}
