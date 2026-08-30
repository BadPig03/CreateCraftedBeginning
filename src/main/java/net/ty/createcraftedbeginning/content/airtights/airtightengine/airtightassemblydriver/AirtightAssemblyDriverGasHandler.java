package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
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

        AirtightEngineHandler engineHandler = AirtightEngineHandlerUtils.of(gasStack);
        double workFactor = engineHandler.getWorkFactor();
        return GasConsumptions.isFinite(workFactor) && workFactor > 0 && engineHandler.getMaxLevel() > 0;
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
        if (!isGasValid(0, resource)) {
            return 0;
        }
        return flowMeter.fill(resource, action);
    }

    @Override
    public long getTankCapacity(int tank) {
        return CCBConfig.server().airtights.maxAirtightTankCapacityPerBlock.get() * GasAmounts.MILLIBUCKETS_PER_BUCKET;
    }
}
