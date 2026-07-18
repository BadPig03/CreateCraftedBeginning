package net.ty.createcraftedbeginning.api.gascanisters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public interface IGasCanisterContainer {
    int NON_EMPTY_PACK = 1;
    int NON_EMPTY_CANISTER = 0;
    int EMPTY_PACK = -1;
    int EMPTY_CANISTER = -2;

    /**
     * Checks whether this value is empty.
     *
     * @return {@code true} if this value is empty; otherwise {@code false}
     */
    boolean isEmpty();

    /**
     * Checks whether this value is full.
     *
     * @return {@code true} if this value is full; otherwise {@code false}
     */
    boolean isFull();

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
     * @param tank     the zero-based tank index
     * @param resource the gas resource to insert or extract
     * @param action   the action that determines whether the operation is simulated or executed
     * @return the gas that was extracted
     */
    GasStack drain(int tank, GasStack resource, GasAction action);

    /**
     * Attempts to extract gas from this handler.
     *
     * @param tank     the zero-based tank index
     * @param maxDrain the maximum amount that may be extracted
     * @param action   the action that determines whether the operation is simulated or executed
     * @return the gas that was extracted
     */
    GasStack drain(int tank, long maxDrain, GasAction action);

    /**
     * Returns the gas stored in the specified tank.
     *
     * @param tank the zero-based tank index
     * @return the gas stored in the specified tank
     */
    GasStack getGasInTank(int tank);

    /**
     * Returns the priority.
     *
     * @return the priority
     */
    int getPriority();

    /**
     * Returns the number of gas tanks exposed by this handler.
     *
     * @return the number of available tanks
     */
    int getTanks();

    /**
     * Returns the container.
     *
     * @return the container
     */
    ItemStack getContainer();

    /**
     * Returns the virtual items.
     *
     * @return the virtual items
     */
    List<ItemStack> getVirtualItems();

    /**
     * Returns the machine filling strategy.
     *
     * @return the machine filling strategy
     */
    default MachineFillingStrategy getMachineFillingStrategy() {
        return MachineFillingStrategy.ALLOW;
    }

    /**
     * Attempts to insert the supplied gas into this handler.
     *
     * @param tank     the zero-based tank index
     * @param resource the gas resource to insert or extract
     * @param action   the action that determines whether the operation is simulated or executed
     * @return the amount of gas that was accepted
     */
    long fill(int tank, GasStack resource, GasAction action);

    /**
     * Returns the capacity of the specified gas tank.
     *
     * @param tank the zero-based tank index
     * @return the capacity of the specified tank
     */
    long getTankCapacity(int tank);

    /**
     * Serializes this object's state.
     */
    void save();

    /**
     * Sets the capacity.
     *
     * @param tank     the zero-based tank index
     * @param capacity the capacity to use
     */
    void setCapacity(int tank, long capacity);

    enum MachineFillingStrategy {
        ALLOW,
        DENY
    }
}
