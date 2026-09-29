package net.ty.createcraftedbeginning.gas.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasTransportEdgeProperties(long resistanceUnits, GasTransportPressureDrive pressureDrive, long flowRateLimit, long flowLimitReferencePressurePa, double conductancePerPascal, boolean sharedFlowBudget, boolean dynamic) {
    public GasTransportEdgeProperties {
        resistanceUnits = Math.max(GasFlowResistance.MIN_RESISTANCE_UNITS, resistanceUnits);
        flowRateLimit = Math.max(0, flowRateLimit);
        flowLimitReferencePressurePa = Math.max(GasPressure.VACUUM_PA, flowLimitReferencePressurePa);
        conductancePerPascal = Double.isFinite(conductancePerPascal) && conductancePerPascal > 0 ? conductancePerPascal : Double.NaN;
    }

    public static GasTransportEdgeProperties passive(long resistanceUnits) {
        return new GasTransportEdgeProperties(resistanceUnits, GasTransportPressureDrive.pressureBoost(GasPressure.VACUUM_PA), Long.MAX_VALUE, GasPressure.VACUUM_PA, Double.NaN, false, false);
    }
}
