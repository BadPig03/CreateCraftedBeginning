package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlers;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightAssemblyDriverGasHandler implements GasPressureBoundary {
    private final AirtightAssemblyDriverFlowMeter flowMeter;

    AirtightAssemblyDriverGasHandler(AirtightAssemblyDriverFlowMeter flowMeter) {
        this.flowMeter = flowMeter;
    }

    private static boolean isUsable(AirtightEngineHandler engineHandler) {
        double workFactor = engineHandler.getWorkFactor();
        return GasConsumptionMath.isFinite(workFactor) && workFactor > 0 && engineHandler.getMaxLevel() > 0;
    }

    @Override
    public boolean isGasValid(int tank, GasStack gasStack) {
        return tank == 0 && !gasStack.isEmpty() && GameplayPressureProfiles.orderedProfiles().stream().anyMatch(profile -> isUsable(AirtightEngineHandlers.resolve(gasStack, profile)));
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return GasStack.EMPTY;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return GasStack.EMPTY;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return GasStack.EMPTY;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return fillFromPressure(resource, GasPressure.REFERENCE_PRESSURE_PA, action);
    }

    @Override
    public long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
        if (resource.isEmpty() || !isUsable(AirtightEngineHandlers.resolve(resource, sourcePressurePa))) {
            return 0;
        }

        return flowMeter.fill(resource, sourcePressurePa, action);
    }
}
