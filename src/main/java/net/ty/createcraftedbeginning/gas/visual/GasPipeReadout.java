package net.ty.createcraftedbeginning.gas.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public sealed interface GasPipeReadout {
    record Pressure(long minPressurePa, long maxPressurePa, long displayMaxPressurePa, long displayDifferencePa) implements GasPipeReadout {
        public Pressure(long minPressurePa, long maxPressurePa) {
            this(minPressurePa, maxPressurePa, maxPressurePa, difference(minPressurePa, maxPressurePa));
        }

        private static long difference(long minimum, long maximum) {
            if (minimum < GasPressure.VACUUM_PA || maximum < minimum) {
                return GasPressure.VACUUM_PA;
            }

            return maximum - minimum;
        }

        public boolean hasReading() {
            return minPressurePa >= GasPressure.VACUUM_PA && maxPressurePa >= minPressurePa;
        }

        public long differencePa() {
            return difference(minPressurePa, maxPressurePa);
        }
    }

    record Flow(long flowRate, long displayFlowRate) implements GasPipeReadout {
        public Flow(long flowRate) {
            this(flowRate, flowRate);
        }
    }

    record PumpConfiguration(long maxPressureBoostPa) implements GasPipeReadout {
    }

    record RegulatorConfiguration(long maxPressureRisePa, long outletSetPressurePa) implements GasPipeReadout {
    }

    record ValveState(boolean open) implements GasPipeReadout {
    }
}
