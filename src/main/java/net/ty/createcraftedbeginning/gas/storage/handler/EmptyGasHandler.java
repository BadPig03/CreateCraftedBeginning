package net.ty.createcraftedbeginning.gas.storage.handler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum EmptyGasHandler implements GasHandler {
    INSTANCE;

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return true;
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
        return 0;
    }
}
