package net.ty.createcraftedbeginning.api.gas.interfaces;

import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;

@SuppressWarnings("unused")
public interface IGasTank {
    GasStack getGas();

    long getGasAmount();

    long getCapacity();

    boolean isGasValid(GasStack stack);

    long fill(GasStack resource, GasAction action);

    GasStack drain(long maxDrain, GasAction action);

    GasStack drain(GasStack resource, GasAction action);
}
