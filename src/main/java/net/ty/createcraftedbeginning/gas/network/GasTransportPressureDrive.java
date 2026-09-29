package net.ty.createcraftedbeginning.gas.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.OutletPressureTarget;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.PressureBoost;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public sealed interface GasTransportPressureDrive permits PressureBoost, OutletPressureTarget {
    static PressureBoost pressureBoost(long pressureBoostPa) {
        return new PressureBoost(pressureBoostPa);
    }

    static OutletPressureTarget outletPressureTarget(long targetPressurePa, long maxPressureBoostPa) {
        return new OutletPressureTarget(targetPressurePa, maxPressureBoostPa);
    }

    double drivenOutletPressurePa(double inletPressurePa);

    Linearization linearize(double inletPressurePa, double conductancePerPascal);

    default double drivePressurePa(double inletPressurePa, double outletPressurePa) {
        return drivenOutletPressurePa(inletPressurePa) - Math.max(GasPressure.VACUUM_PA, outletPressurePa);
    }

    record Linearization(double fromPressureConductance, double toPressureConductance, double constantFlowRate) {
        public Linearization {
            fromPressureConductance = nonNegativeFinite(fromPressureConductance);
            toPressureConductance = nonNegativeFinite(toPressureConductance);
            constantFlowRate = nonNegativeFinite(constantFlowRate);
        }

        private static double nonNegativeFinite(double value) {
            if (!Double.isFinite(value) || value <= 0) {
                return 0;
            }

            return value;
        }
    }

    record PressureBoost(long pressureBoostPa) implements GasTransportPressureDrive {
        public PressureBoost {
            pressureBoostPa = GasPressureLimits.clampToHardLimit(pressureBoostPa);
        }

        @Override
        public double drivenOutletPressurePa(double inletPressurePa) {
            double inletPressure = GasPressureLimits.clampToHardLimit(inletPressurePa);
            return Math.min(GasPressureLimits.HARD_PRESSURE_PA, inletPressure + pressureBoostPa);
        }

        @Override
        public Linearization linearize(double inletPressurePa, double conductancePerPascal) {
            double conductance = Math.max(0, conductancePerPascal);
            double inletPressure = GasPressureLimits.clampToHardLimit(inletPressurePa);
            if (inletPressure + pressureBoostPa > GasPressureLimits.HARD_PRESSURE_PA) {
                return new Linearization(0, conductance, conductance * GasPressureLimits.HARD_PRESSURE_PA);
            }

            return new Linearization(conductance, conductance, conductance * pressureBoostPa);
        }
    }

    record OutletPressureTarget(long targetPressurePa, long maxPressureBoostPa) implements GasTransportPressureDrive {
        public OutletPressureTarget {
            targetPressurePa = GasPressureLimits.clampToHardLimit(targetPressurePa);
            maxPressureBoostPa = GasPressureLimits.clampToHardLimit(maxPressureBoostPa);
        }

        @Override
        public double drivenOutletPressurePa(double inletPressurePa) {
            double inletPressure = GasPressureLimits.clampToHardLimit(inletPressurePa);
            return Math.min(targetPressurePa, inletPressure + maxPressureBoostPa);
        }

        @Override
        public Linearization linearize(double inletPressurePa, double conductancePerPascal) {
            double conductance = Math.max(0, conductancePerPascal);
            if (targetLimited(inletPressurePa)) {
                return new Linearization(0, conductance, conductance * targetPressurePa);
            }

            return new Linearization(conductance, conductance, conductance * maxPressureBoostPa);
        }

        public boolean targetLimited(double inletPressurePa) {
            double inletPressure = GasPressureLimits.clampToHardLimit(inletPressurePa);
            return targetPressurePa <= inletPressure + maxPressureBoostPa;
        }
    }
}
