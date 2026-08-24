package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AirtightReactorKettleGasPortHandler(IGasHandler input, IGasHandler output) implements IGasHandler {
    @Override
    public int getTanks() {
        return input.getTanks() + output.getTanks();
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return getHandler(tank).getGasInTank(getLocalTank(tank));
    }

    @Override
    public long getTankCapacity(int tank) {
        return getHandler(tank).getTankCapacity(getLocalTank(tank));
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        getHandler(tank);
        return tank < input.getTanks() && input.isGasValid(tank, stack);
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return input.fill(resource, action);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        return input.tryFillAtomically(resources, action);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return output.drain(resource, action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return output.drain(maxDrain, action);
    }

    private IGasHandler getHandler(int tank) {
        if (tank < 0 || tank >= getTanks()) {
            throw new IndexOutOfBoundsException("Tank " + tank + " not in valid range [0," + getTanks() + ')');
        }

        if (tank < input.getTanks()) {
            return input;
        }
        return output;
    }

    private int getLocalTank(int tank) {
        return tank < input.getTanks() ? tank : tank - input.getTanks();
    }
}
