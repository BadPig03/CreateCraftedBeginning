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

    static int getMaxStoredHeat() {
        return getNextOverheatThreshold() * OverheatState.MELTDOWN.ordinal();
    }

    static int clampStoredHeat(int storedHeat) {
        return Math.clamp(storedHeat, 0, getMaxStoredHeat());
    }

    static int getNextStateHeat(OverheatState state) {
        if (state == OverheatState.MELTDOWN) {
            return getMaxStoredHeat();
        }
        return clampStoredHeat((state.ordinal() + 1) * getNextOverheatThreshold());
    }

    static OverheatState getOverheatState(int storedHeat) {
        return OverheatState.fromStoredHeat(storedHeat, getNextOverheatThreshold());
    }

    static int updateStoredHeat(int storedHeat, float speed, boolean operating, CoolantEfficiency coolantEfficiency, Level level) {
        int netHeat = getHeatAdded(speed, operating) - coolantEfficiency.getHeatReduced(level);
        long updatedHeat = (long) storedHeat + netHeat;
        return (int) Math.max(0, Math.min(updatedHeat, getMaxStoredHeat()));
    }

    static CoolantEfficiency getCoolantEfficiency(Level level, BlockPos coolantPos) {
        BlockState coolantState = level.getBlockState(coolantPos);
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandlerUtils.of(coolantState.getBlock());
        return coolantHandler.getCoolantEfficiency(level, coolantPos, coolantState);
    }

    static CoolantEfficiency tickCoolant(ServerLevel level, BlockPos coolantPos, boolean shouldConsume, RandomSource random) {
        BlockState coolantState = level.getBlockState(coolantPos);
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandlerUtils.of(coolantState.getBlock());
        CoolantEfficiency efficiency = coolantHandler.getCoolantEfficiency(level, coolantPos, coolantState);
        float consumeChance = Mth.clamp(CCBConfig.server().airtights.coolantConsumptionChance.getF(), 0, 1);
        if (efficiency == CoolantEfficiency.NONE || !shouldConsume || random.nextFloat() >= consumeChance) {
            return efficiency;
        }

        BlockState meltedState = coolantHandler.getMeltBlockState(level, coolantPos, coolantState);
        if (meltedState == null || meltedState.equals(coolantState)) {
            return getCoolantEfficiency(level, coolantPos);
        }

        if (meltedState.isAir()) {
            level.removeBlock(coolantPos, false);
        }
        else {
            level.setBlockAndUpdate(coolantPos, meltedState);
        }
        return getCoolantEfficiency(level, coolantPos);
    }

    private static int getHeatAdded(float speed, boolean operating) {
        if (!operating) {
            return 0;
        }

        float mediumSpeed = Math.max(1, SpeedLevel.MEDIUM.getSpeedValue());
        float maximumSpeed = Math.max(mediumSpeed, AllConfigs.server().kinetics.maxRotationSpeed.get());
        float progress = maximumSpeed == mediumSpeed ? 1 : Mth.clamp((Mth.abs(speed) - mediumSpeed) / (maximumSpeed - mediumSpeed), 0, 1);
        return Mth.floor(Mth.lerp(progress, 3, 5) + 0.5f);
    }
}
