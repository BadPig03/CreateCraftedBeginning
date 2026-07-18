package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

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
