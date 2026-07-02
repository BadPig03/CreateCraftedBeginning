package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasHandler {
    boolean isGasValid(int tank, GasStack stack);

    GasStack drain(GasStack resource, GasAction action);

    GasStack drain(long maxDrain, GasAction action);

    GasStack getGasInTank(int tank);

    int getTanks();

    long fill(GasStack resource, GasAction action);

    long getTankCapacity(int tank);
}
