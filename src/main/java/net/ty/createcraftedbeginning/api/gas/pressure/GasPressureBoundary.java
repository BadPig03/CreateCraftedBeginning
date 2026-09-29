package net.ty.createcraftedbeginning.api.gas.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasPressureBoundary extends GasHandler {
    default long getDrainPressurePa(int tank, GasStack gas) {
        return GasPressure.REFERENCE_PRESSURE_PA;
    }

    default long getFillPressurePa(int tank, GasStack gas) {
        return GasPressure.VACUUM_PA;
    }

    @SuppressWarnings("unused")
    default double getDrainFlowResistanceFactor(int tank, GasStack gas) {
        return 0;
    }

    default double getFillFlowResistanceFactor(int tank, GasStack gas) {
        return 0;
    }

    default long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
        return fill(resource, action);
    }

    default boolean supportsExactDrainRecovery(int tank) {
        return false;
    }

    default long restoreDrainedGas(int tank, GasStack resource, GasAction action) {
        return fill(resource, action);
    }
}
