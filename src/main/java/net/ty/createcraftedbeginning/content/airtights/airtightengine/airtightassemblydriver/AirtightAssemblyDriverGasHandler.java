package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightAssemblyDriverGasHandler implements IGasHandler {
    private final AirtightAssemblyDriverFlowMeter flowMeter;

    AirtightAssemblyDriverGasHandler(AirtightAssemblyDriverFlowMeter flowMeter) {
        this.flowMeter = flowMeter;
    }

    @Override
    public boolean isGasValid(int tank, GasStack gasStack) {
        if (gasStack.isEmpty()) {
            return false;
        }

        AirtightEngineHandler handler = AirtightEngineHandlerUtils.of(gasStack);
        double workFactor = handler.getWorkFactor();
        return Double.isFinite(workFactor) && workFactor > 0 && handler.getMaxLevel() > 0;
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
        return isGasValid(0, resource) ? flowMeter.fill(resource, action) : 0;
    }

    @Override
    public long getTankCapacity(int tank) {
        return CCBConfig.server().airtights.maxAirtightTankCapacityPerBlock.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }
}
