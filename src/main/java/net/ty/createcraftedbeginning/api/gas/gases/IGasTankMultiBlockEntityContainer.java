package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;

public interface IGasTankMultiBlockEntityContainer extends IMultiBlockEntityContainer {
    interface iGas extends IGasTankMultiBlockEntityContainer {
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
