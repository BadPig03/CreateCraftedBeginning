package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasTank {
    /**
     * Checks whether the supplied gas stack is valid for the specified tank.
     *
     * @param stack the stack to inspect or process
     * @return {@code true} if the supplied gas stack is valid for the specified tank; otherwise {@code false}
     */
    boolean isGasValid(GasStack stack);

    /**
     * Attempts to extract gas from this handler.
     *
     * @param resource the gas resource to insert or extract
     * @param action   the action that determines whether the operation is simulated or executed
     * @return the gas that was extracted
     */
    GasStack drain(GasStack resource, GasAction action);

    /**
     * Attempts to extract gas from this handler.
     *
     * @param maxDrain the maximum amount that may be extracted
     * @param action   the action that determines whether the operation is simulated or executed
     * @return the gas that was extracted
     */
    GasStack drain(long maxDrain, GasAction action);

    /**
     * Returns the gas stack.
     *
     * @return the gas stack
     */
    GasStack getGasStack();

    /**
     * Attempts to insert the supplied gas into this handler.
     *
     * @param resource the gas resource to insert or extract
     * @param action   the action that determines whether the operation is simulated or executed
     * @return the amount of gas that was accepted
     */
    long fill(GasStack resource, GasAction action);

    /**
     * Returns the capacity.
     *
     * @return the capacity
     */
    long getCapacity();

    /**
     * Returns the gas amount.
     *
     * @return the gas amount
     */
    long getGasAmount();

}
