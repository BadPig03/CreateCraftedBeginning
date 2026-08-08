package net.ty.createcraftedbeginning.content.airtights.gas.interfaces;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasTankMultiBlockEntityContainer extends IMultiBlockEntityContainer, IGasInventoryIdentifierProvider {
    /**
     * Returns the tank.
     *
     * @param tank the zero-based tank index
     * @return the tank
     */
    IGasTank getTank(int tank);

    /**
     * Sets the tank size.
     *
     * @param tank   the zero-based tank index
     * @param blocks the blocks to inspect or process
     */
    void setTankSize(int tank, int blocks);

    /**
     * Merges the source container's tank state into this container.
     *
     * @param source the source container whose state is being absorbed
     */
    default void mergeTankStateFrom(IGasTankMultiBlockEntityContainer source) {
        if (!hasTank() || !source.hasTank()) {
            return;
        }

        GasStack sourceGas = source.getGas(0);
        if (!sourceGas.isEmpty()) {
            getTank(0).fill(sourceGas, GasAction.EXECUTE);
        }
        source.clearTankStateAfterMerge(0);
    }

    /**
     * Clears tank state after it has been merged into another controller.
     *
     * @param tank the zero-based tank index
     */
    default void clearTankStateAfterMerge(int tank) {
        IGasTank gasTank = getTank(tank);
        gasTank.drain(gasTank.getCapacity(), GasAction.EXECUTE);
    }

    /**
     * Captures state for a multiblock split and resizes the controller to one block.
     *
     * @param tank              the zero-based tank index
     * @param controllerRemoved whether the old controller has already been removed
     * @return the remaining state to distribute to split parts
     */
    default GasStack prepareTankStateForSplit(int tank, boolean controllerRemoved) {
        GasStack state = getGas(tank);
        if (!state.isEmpty() && !controllerRemoved) {
            state.shrink(getTankSize(tank));
        }
        setTankSize(tank, 1);
        return state;
    }

    /**
     * Applies state from a former controller to a split multiblock part.
     *
     * @param tank  the zero-based tank index
     * @param state the mutable state still available for distribution
     */
    default void applySplitTankState(int tank, GasStack state) {
        if (!hasTank() || state.isEmpty()) {
            return;
        }

        IGasTank gasTank = getTank(tank);
        long amount = Math.min(gasTank.getCapacity(), state.getAmount());
        long accepted = gasTank.fill(state.copyWithAmount(amount), GasAction.EXECUTE);
        state.shrink(accepted);
    }

    /**
     * Checks whether this value has tank.
     *
     * @return {@code true} if this value has tank; otherwise {@code false}
     */
    default boolean hasTank() {
        return false;
    }

    /**
     * Returns the gas.
     *
     * @param tank the zero-based tank index
     * @return the gas
     */
    default GasStack getGas(int tank) {
        return GasStack.EMPTY;
    }

    /**
     * Returns the tank size.
     *
     * @param tank the zero-based tank index
     * @return the tank size
     */
    default long getTankSize(int tank) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        return new Single(getController());
    }
}
