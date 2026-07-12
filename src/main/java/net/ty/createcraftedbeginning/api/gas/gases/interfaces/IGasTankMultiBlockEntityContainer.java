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
    IGasTank getTank(int tank);

    void setTankSize(int tank, int blocks);

    default boolean hasTank() {
        return false;
    }

    default GasStack getGas(int tank) {
        return GasStack.EMPTY;
    }

    default long getTankSize(int tank) {
        return 0;
    }

    @Override
    default InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        return new Single(getController());
    }
}
