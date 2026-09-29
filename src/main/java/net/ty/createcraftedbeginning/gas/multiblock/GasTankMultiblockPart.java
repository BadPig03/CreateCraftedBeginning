package net.ty.createcraftedbeginning.gas.multiblock;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.logistics.GasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasTankMultiblockPart extends IMultiBlockEntityContainer, GasInventoryIdentifierProvider {
    @Override
    default InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        return new Single(getController());
    }

    GasPressureCompartment getTank(int tank);

    void setTankBlockCount(int tank, int blocks);

    default void mergeTankStateFrom(GasTankMultiblockPart source) {
        if (!hasTank() || !source.hasTank()) {
            return;
        }

        GasStack sourceGas = source.getGas(0);
        if (sourceGas.isEmpty()) {
            return;
        }

        GasPressureCompartment targetTank = getTank(0);
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
        GasPressureCompartment gasTank = getTank(tank);
        gasTank.drain(gasTank.getStoredAmount(), GasAction.EXECUTE);
    }

    default GasStack prepareTankStateForSplit(int tank, boolean controllerRemoved) {
        GasStack state = getGas(tank);
        if (!state.isEmpty()) {
            state = getTank(tank).drain(state.getAmount(), GasAction.EXECUTE);
        }
        setTankBlockCount(tank, 1);
        return state;
    }

    default void applySplitTankState(int tank, GasStack state) {
        if (!hasTank() || state.isEmpty()) {
            return;
        }

        GasPressureCompartment gasTank = getTank(tank);
        long amount = Math.min(gasTank.getMaxAmount(), state.getAmount());
        long accepted = gasTank.fill(state.copyWithAmount(amount), GasAction.EXECUTE);
        state.shrink(accepted);
    }

    default void applySplitTankState(int tank, GasStack state, int remainingTankCount) {
        applySplitTankState(tank, state);
    }

    default boolean hasTank() {
        return false;
    }

    default GasStack getGas(int tank) {
        return GasStack.EMPTY;
    }
}
