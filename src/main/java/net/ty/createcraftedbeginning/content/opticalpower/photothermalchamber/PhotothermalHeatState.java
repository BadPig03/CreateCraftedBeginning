package net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.opticalpower.energy.OpticalEnergyStorage;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalIllumination;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalHeatState {
    static final double CAPACITY_LUMEN_SECONDS = 480;
    static final int MAX_HEAT_PROGRESS = 300;
    private static final int HEATING_THRESHOLD = 100;
    private static final double HEATED_INPUT_LM = 12;
    private static final double SUPERHEATED_INPUT_LM = 36;
    private static final int INPUT_CONFIRMATION_TICKS = 20;
    private static final int SUPERHEATED_EXIT_PROGRESS = 200;
    private static final int WARMING_PER_TICK = 3;
    private static final int COOLING_PER_TICK = 1;
    private static final String COMPOUND_KEY_HEAT_PROGRESS = "HeatProgress";
    private static final String COMPOUND_KEY_THERMAL_LEVEL = "ThermalLevel";
    private static final String COMPOUND_KEY_OUTPUT_LEVEL = "OutputLevel";
    private static final double HEATED_COST_LUMEN_SECONDS = 0.6;
    private static final double SUPERHEATED_COST_LUMEN_SECONDS = 1.8;
    private static final double HEATED_RESERVE_LUMEN_SECONDS = 12;
    private static final double SUPERHEATED_RESERVE_LUMEN_SECONDS = 36;

    private final OpticalEnergyStorage storage = new OpticalEnergyStorage(CAPACITY_LUMEN_SECONDS);
    private int heatProgress;
    private int thermalLevel;
    private int outputLevel;
    private int heatedInputTicks;
    private int superheatedInputTicks;

    static int resolveTargetHeatProgress(OpticalIllumination illumination) {
        List<PhotothermalChamberPort> ports = PhotothermalChamberPort.getPorts();
        for (int tier = MAX_HEAT_PROGRESS / HEATING_THRESHOLD; tier > 0; tier--) {
            double thresholdLx = tier * OpticalIllumination.GROWTH_ILLUMINANCE_LX;
            for (PhotothermalChamberPort port : ports) {
                if (illumination.getQualifiedFluxLm(port.ordinal(), thresholdLx) > 0) {
                    return tier * HEATING_THRESHOLD;
                }
            }
        }
        return 0;
    }

    public void tick(int target, double incomingLumenSeconds, double availableFluxLm) {
        if (target < 0 || target > MAX_HEAT_PROGRESS || target % HEATING_THRESHOLD != 0) {
            throw new IllegalArgumentException("Thermal target must be 0, 100, 200 or 300; got " + target + '.');
        }

        storage.receive(incomingLumenSeconds);
        heatedInputTicks = availableFluxLm >= HEATED_INPUT_LM ? Math.min(INPUT_CONFIRMATION_TICKS, heatedInputTicks + 1) : 0;
        superheatedInputTicks = availableFluxLm >= SUPERHEATED_INPUT_LM ? Math.min(INPUT_CONFIRMATION_TICKS, superheatedInputTicks + 1) : 0;
        if (heatProgress < target) {
            heatProgress = Math.min(target, heatProgress + WARMING_PER_TICK);
        }
        else if (heatProgress > target) {
            heatProgress = Math.max(target, heatProgress - COOLING_PER_TICK);
        }
        if (heatProgress == 0) {
            thermalLevel = 0;
        }
        else if (heatProgress == MAX_HEAT_PROGRESS) {
            thermalLevel = 2;
        }
        else if (thermalLevel == 2 && heatProgress <= SUPERHEATED_EXIT_PROGRESS || thermalLevel == 0 && heatProgress >= HEATING_THRESHOLD) {
            thermalLevel = 1;
        }

        if (thermalLevel == 2 && storage.hasAtLeast(SUPERHEATED_COST_LUMEN_SECONDS) && (outputLevel == 2 || superheatedInputTicks == INPUT_CONFIRMATION_TICKS && storage.hasAtLeast(SUPERHEATED_RESERVE_LUMEN_SECONDS))) {
            outputLevel = 2;
            storage.extract(SUPERHEATED_COST_LUMEN_SECONDS);
            return;
        }

        if (thermalLevel >= 1 && storage.hasAtLeast(HEATED_COST_LUMEN_SECONDS) && (outputLevel >= 1 || heatedInputTicks == INPUT_CONFIRMATION_TICKS && storage.hasAtLeast(HEATED_RESERVE_LUMEN_SECONDS))) {
            outputLevel = 1;
            storage.extract(HEATED_COST_LUMEN_SECONDS);
            return;
        }

        outputLevel = 0;
    }

    public int getHeatLevel() {
        return outputLevel;
    }

    public void write(CompoundTag compoundTag) {
        storage.write(compoundTag);
        compoundTag.putInt(COMPOUND_KEY_HEAT_PROGRESS, heatProgress);
        compoundTag.putInt(COMPOUND_KEY_THERMAL_LEVEL, thermalLevel);
        compoundTag.putInt(COMPOUND_KEY_OUTPUT_LEVEL, outputLevel);
    }

    int getThermalLevel() {
        return thermalLevel;
    }

    int getHeatProgress() {
        return heatProgress;
    }

    double getQuantityLumenSeconds() {
        return storage.getQuantityLumenSeconds();
    }

    void readSaved(CompoundTag compoundTag, BlockPos pos) {
        storage.readSaved(compoundTag, pos);
        restoreTemperature(compoundTag);
    }

    void restore(CompoundTag compoundTag, double quantityLumenSeconds) {
        storage.restoreQuantity(quantityLumenSeconds);
        restoreTemperature(compoundTag);
    }

    private void restoreTemperature(CompoundTag compoundTag) {
        heatedInputTicks = 0;
        superheatedInputTicks = 0;
        heatProgress = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_HEAT_PROGRESS), 0, MAX_HEAT_PROGRESS);
        thermalLevel = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_THERMAL_LEVEL), 0, 2);
        if (heatProgress == 0) {
            thermalLevel = 0;
        }
        else if (thermalLevel == 2 && heatProgress <= SUPERHEATED_EXIT_PROGRESS) {
            thermalLevel = 1;
        }
        outputLevel = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_OUTPUT_LEVEL), 0, thermalLevel);
    }
}
