package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalHeatState {
    public static final int MAX_HEAT_LEVEL = 2;
    public static final int HEATING_THRESHOLD = 100;
    public static final int MAX_HEAT_PROGRESS = 300;

    private static final String COMPOUND_KEY_HEAT_PROGRESS = "HeatProgress";
    private static final int HEATING_POWER_LP = 16;
    private static final int HIGH_HEATING_POWER_LP = 32;
    private static final int SUPERHEATING_POWER_LP = 48;
    private static final int THERMAL_TRANSITION_TICKS = 100;
    private static final int WARMUP_PER_TICK = MAX_HEAT_PROGRESS / THERMAL_TRANSITION_TICKS;
    private static final int COOLDOWN_PER_TICK = MAX_HEAT_PROGRESS / THERMAL_TRANSITION_TICKS;

    private int heatProgress;

    public static int getTargetHeatProgress(int receivedPowerLp) {
        if (receivedPowerLp < HEATING_POWER_LP) {
            return 0;
        }

        if (receivedPowerLp < HIGH_HEATING_POWER_LP) {
            return HEATING_THRESHOLD;
        }

        if (receivedPowerLp < SUPERHEATING_POWER_LP) {
            return HEATING_THRESHOLD * 2;
        }

        return MAX_HEAT_PROGRESS;
    }

    public void tick(int receivedPowerLp) {
        int target = getTargetHeatProgress(receivedPowerLp);
        if (heatProgress < target) {
            heatProgress = Math.min(target, heatProgress + WARMUP_PER_TICK);
            return;
        }

        heatProgress = Math.max(target, heatProgress - COOLDOWN_PER_TICK);
    }

    public int getHeatLevel() {
        if (heatProgress < HEATING_THRESHOLD) {
            return 0;
        }

        if (heatProgress < MAX_HEAT_PROGRESS) {
            return 1;
        }

        return MAX_HEAT_LEVEL;
    }

    public int getHeatProgress() {
        return heatProgress;
    }

    public void write(CompoundTag compoundTag) {
        compoundTag.putInt(COMPOUND_KEY_HEAT_PROGRESS, heatProgress);
    }

    public void read(CompoundTag compoundTag) {
        heatProgress = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_HEAT_PROGRESS), 0, MAX_HEAT_PROGRESS);
    }
}
