package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum EmptyGasHandler implements IGasHandler {
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return GasStack.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return GasStack.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack getGasInTank(int tank) {
        return GasStack.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTanks() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long fill(GasStack resource, GasAction action) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTankCapacity(int tank) {
        return 0;
    }
}
