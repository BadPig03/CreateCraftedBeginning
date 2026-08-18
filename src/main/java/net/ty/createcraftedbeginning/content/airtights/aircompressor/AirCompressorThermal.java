package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandlerUtils;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirCompressorThermal {
    private AirCompressorThermal() {
    }

    static int getNextOverheatThreshold() {
        return Mth.clamp(CCBConfig.server().airtights.nextOverheatThreshold.get(), 1, Integer.MAX_VALUE / OverheatState.MELTDOWN.ordinal());
    }

    static int clampStoredHeat(int storedHeat) {
        return Math.clamp(storedHeat, 0, getMaxStoredHeat());
    }

    static int getNextStateHeat(OverheatState overheatState) {
        if (overheatState == OverheatState.MELTDOWN) {
            return getMaxStoredHeat();
        }
        return clampStoredHeat((overheatState.ordinal() + 1) * getNextOverheatThreshold());
    }

    static OverheatState getOverheatState(int storedHeat) {
        return OverheatState.fromStoredHeat(storedHeat, getNextOverheatThreshold());
    }

    static int updateStoredHeat(int storedHeat, float speed, boolean isOperating, CoolantEfficiency coolantEfficiency, Level level) {
        int netHeatChange = getHeatAdded(speed, isOperating) - coolantEfficiency.getHeatReduced(level);
        long updatedStoredHeat = storedHeat + netHeatChange;
        return (int) Mth.clamp(0, updatedStoredHeat, getMaxStoredHeat());
    }

    static boolean isMeltdownPreventedByCoolant(int storedHeat, float speed, boolean isOperating, CoolantEfficiency coolantEfficiency, Level level) {
        if (!isOperating) {
            return false;
        }

        int passiveHeatReduced = CoolantEfficiency.NONE.getHeatReduced(level);
        int coolantHeatReduced = coolantEfficiency.getHeatReduced(level);
        if (coolantHeatReduced <= passiveHeatReduced) {
            return false;
        }

        int heatAdded = getHeatAdded(speed, true);
        long heatWithoutCoolant = storedHeat + heatAdded - passiveHeatReduced;
        long heatWithCoolant = storedHeat + heatAdded - coolantHeatReduced;
        int maxStoredHeat = getMaxStoredHeat();
        return heatWithoutCoolant >= maxStoredHeat && heatWithCoolant < maxStoredHeat;
    }

    static CoolantEfficiency getCoolantEfficiency(Level level, BlockPos coolantPos) {
        BlockState coolantState = level.getBlockState(coolantPos);
        return AirtightCoolantHandlerUtils.of(coolantState.getBlock()).getCoolantEfficiency(level, coolantPos, coolantState);
    }

    static CoolantEfficiency tickCoolant(ServerLevel level, BlockPos coolantPos, boolean shouldConsumeCoolant, RandomSource random) {
        BlockState coolantState = level.getBlockState(coolantPos);
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandlerUtils.of(coolantState.getBlock());
        CoolantEfficiency coolantEfficiency = coolantHandler.getCoolantEfficiency(level, coolantPos, coolantState);
        float coolantConsumptionChance = Mth.clamp(CCBConfig.server().airtights.coolantConsumptionChance.getF(), 0, 1);
        if (coolantEfficiency == CoolantEfficiency.NONE || !shouldConsumeCoolant || random.nextFloat() >= coolantConsumptionChance) {
            return coolantEfficiency;
        }

        BlockState meltedCoolantState = coolantHandler.getMeltBlockState(level, coolantPos, coolantState);
        if (meltedCoolantState == null || meltedCoolantState.equals(coolantState)) {
            return getCoolantEfficiency(level, coolantPos);
        }

        if (meltedCoolantState.isAir()) {
            level.removeBlock(coolantPos, false);
        }
        else {
            level.setBlockAndUpdate(coolantPos, meltedCoolantState);
        }
        return getCoolantEfficiency(level, coolantPos);
    }

    private static int getMaxStoredHeat() {
        return getNextOverheatThreshold() * OverheatState.MELTDOWN.ordinal();
    }

    private static int getHeatAdded(float speed, boolean isOperating) {
        if (!isOperating) {
            return 0;
        }

        float mediumSpeed = Math.max(1, SpeedLevel.MEDIUM.getSpeedValue());
        float maximumSpeed = Math.max(mediumSpeed, AllConfigs.server().kinetics.maxRotationSpeed.get());
        float speedProgress = maximumSpeed == mediumSpeed ? 1 : Mth.clamp((Mth.abs(speed) - mediumSpeed) / (maximumSpeed - mediumSpeed), 0, 1);
        return Mth.floor(Mth.lerp(speedProgress, 3, 5) + 0.5f);
    }
}
