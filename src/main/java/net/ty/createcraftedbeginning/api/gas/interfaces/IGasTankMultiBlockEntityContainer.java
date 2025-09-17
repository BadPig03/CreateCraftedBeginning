package net.ty.createcraftedbeginning.api.gas.interfaces;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.ty.createcraftedbeginning.api.gas.GasStack;

public interface IGasTankMultiBlockEntityContainer extends IMultiBlockEntityContainer {
    interface Gas extends IGasTankMultiBlockEntityContainer {
        default boolean hasTank() {
            return false;
        }

        default long getTankSize(int tank) {
            return 0;
        }

        default void setTankSize(int tank, int blocks) {
        }

        default IGasTank getTank(int tank) {
            return null;
        }

        default GasStack getGas(int tank) {
            return GasStack.EMPTY;
        }
    }
}
