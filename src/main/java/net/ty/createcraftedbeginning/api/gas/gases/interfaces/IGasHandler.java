package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasHandler {
    /**
     * Checks whether the supplied gas stack is valid for the specified tank.
     *
     * @param tank  the zero-based tank index
     * @param stack the stack to inspect or process
     * @return {@code true} if the supplied gas stack is valid for the specified tank; otherwise {@code false}
     */
    boolean isGasValid(int tank, GasStack stack);

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
     * Returns the gas stored in the specified tank.
     *
     * @param tank the zero-based tank index
     * @return the gas stored in the specified tank
     */
    GasStack getGasInTank(int tank);

    /**
     * Returns the number of gas tanks exposed by this handler.
     *
     * @return the number of available tanks
     */
    int getTanks();

    /**
     * Attempts to insert the supplied gas into this handler.
     *
     * @param resource the gas resource to insert or extract
     * @param action   the action that determines whether the operation is simulated or executed
     * @return the amount of gas that was accepted
     */
    long fill(GasStack resource, GasAction action);

    /**
     * Attempts to insert one of the supplied gas stacks as a single atomic operation.
     *
     * @param resources the gas resources to inspect or process
     * @param action    the action that determines whether the operation is simulated or executed
     * @return the result of the atomic fill attempt
     */
    default AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty()) {
                continue;
            }

            return AtomicFillResult.UNSUPPORTED;
        }
        return AtomicFillResult.SUCCESS;
    }

    /**
     * Returns the capacity of the specified gas tank.
     *
     * @param tank the zero-based tank index
     * @return the capacity of the specified tank
     */
    long getTankCapacity(int tank);

    enum AtomicFillResult {
        SUCCESS,
        REJECTED,
        UNSUPPORTED;

        /**
         * Checks whether this result represents a successful operation.
         *
         * @return {@code true} if this result represents a successful operation; otherwise {@code false}
         */
        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
}
