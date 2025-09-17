package net.ty.createcraftedbeginning.api.gas;

import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;

public class EmptyGasHandler implements IGasHandler {
    public static final EmptyGasHandler INSTANCE = new EmptyGasHandler();

    protected EmptyGasHandler() {
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return GasStack.EMPTY;
    }

    @Override
    public long getTankCapacity(int tank) {
        return 0;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return true;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return 0;
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return GasStack.EMPTY;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return GasStack.EMPTY;
    }
}
