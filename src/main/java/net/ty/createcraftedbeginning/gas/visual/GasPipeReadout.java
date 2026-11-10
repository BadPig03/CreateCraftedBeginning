package net.ty.createcraftedbeginning.gas.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public sealed interface GasPipeReadout {
    record Pressure(long minPressurePa, long maxPressurePa) implements GasPipeReadout {
        public boolean hasReading() {
            return minPressurePa >= GasPressure.VACUUM_PA && maxPressurePa >= minPressurePa;
        }

        public long differencePa() {
            if (!hasReading()) {
                return GasPressure.VACUUM_PA;
            }

            return maxPressurePa - minPressurePa;
        }
    }

    record Flow(long flowRate) implements GasPipeReadout {
    }

    record PumpConfiguration(long maxPressureBoostPa) implements GasPipeReadout {
    }

    record RegulatorConfiguration(long maxPressureRisePa, long outletSetPressurePa) implements GasPipeReadout {
    }

    record ValveState(boolean open) implements GasPipeReadout {
    }
}
