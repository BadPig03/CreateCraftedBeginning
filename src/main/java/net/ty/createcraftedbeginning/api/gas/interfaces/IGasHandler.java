package net.ty.createcraftedbeginning.api.gas.interfaces;

import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

@SuppressWarnings("unused")
public interface IGasHandler {
    int getTanks();

    GasStack getGasInTank(int tank);

    long getTankCapacity(int tank);

    boolean isGasValid(int tank, GasStack stack);

    long fill(GasStack resource, GasAction action);

    GasStack drain(GasStack resource, GasAction action);

    GasStack drain(long maxDrain, GasAction action);
}
