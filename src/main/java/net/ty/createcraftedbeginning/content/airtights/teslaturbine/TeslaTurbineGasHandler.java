package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class TeslaTurbineGasHandler implements IGasHandler {
    private final TeslaTurbineFlowMeter flowMeter;
    private final boolean clockwise;

    public TeslaTurbineGasHandler(TeslaTurbineFlowMeter flowMeter, boolean clockwise) {
        this.flowMeter = flowMeter;
        this.clockwise = clockwise;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return !stack.isEmpty() && AirtightTurbineHandlerUtils.of(stack).getMaxLevel() > 0;
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
        return isGasValid(0, resource) ? flowMeter.fill(resource, action, clockwise) : 0;
    }

    @Override
    public long getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }
}
