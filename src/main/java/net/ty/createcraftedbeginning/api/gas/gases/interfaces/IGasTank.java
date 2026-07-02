package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasTank {
    boolean isGasValid(GasStack stack);

    GasStack drain(GasStack resource, GasAction action);

    GasStack drain(long maxDrain, GasAction action);

    GasStack getGasStack();

    long fill(GasStack resource, GasAction action);

    long getCapacity();

    long getGasAmount();

}
