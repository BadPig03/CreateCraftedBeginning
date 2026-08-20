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
    IGasTank getTank(int tank);

    void setTankSize(int tank, int blocks);

    default void mergeTankStateFrom(IGasTankMultiBlockEntityContainer source) {
        if (!hasTank() || !source.hasTank()) {
            return;
        }

        GasStack sourceGas = source.getGas(0);
        if (sourceGas.isEmpty()) {
            return;
        }

        IGasTank targetTank = getTank(0);
        long accepted = targetTank.fill(sourceGas, GasAction.SIMULATE);
        if (accepted != sourceGas.getAmount()) {
            return;
        }

        long transferred = targetTank.fill(sourceGas, GasAction.EXECUTE);
        if (transferred != sourceGas.getAmount()) {
            if (transferred > 0) {
                targetTank.drain(transferred, GasAction.EXECUTE);
            }
            return;
        }

        source.clearTankStateAfterMerge(0);
    }

    default void clearTankStateAfterMerge(int tank) {
        IGasTank gasTank = getTank(tank);
        gasTank.drain(gasTank.getCapacity(), GasAction.EXECUTE);
    }

    default GasStack prepareTankStateForSplit(int tank, boolean controllerRemoved) {
        GasStack state = getGas(tank);
        if (!state.isEmpty() && !controllerRemoved) {
            state.shrink(getTankSize(tank));
        }
        setTankSize(tank, 1);
        return state;
    }

    default void applySplitTankState(int tank, GasStack state) {
        if (!hasTank() || state.isEmpty()) {
            return;
        }

        IGasTank gasTank = getTank(tank);
        long amount = Math.min(gasTank.getCapacity(), state.getAmount());
        long accepted = gasTank.fill(state.copyWithAmount(amount), GasAction.EXECUTE);
        state.shrink(accepted);
    }

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
