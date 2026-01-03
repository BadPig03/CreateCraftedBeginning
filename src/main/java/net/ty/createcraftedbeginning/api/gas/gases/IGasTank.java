package net.ty.createcraftedbeginning.api.gas.gases;

@SuppressWarnings("unused")
public interface IGasTank {
    GasStack getGasStack();

    long getGasAmount();

    long getCapacity();

    boolean isGasValid(GasStack stack);

    long fill(GasStack resource, GasAction action);

    GasStack drain(long maxDrain, GasAction action);

    GasStack drain(GasStack resource, GasAction action);
}
