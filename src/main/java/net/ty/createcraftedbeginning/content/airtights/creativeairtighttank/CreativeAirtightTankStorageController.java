package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CreativeSmartGasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasTankMultiBlockEntityContainer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CreativeAirtightTankStorageController {
    private final CreativeAirtightTankBlockEntity owner;

    CreativeAirtightTankStorageController(CreativeAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    void resetCapacity() {
        owner.getTankInventory().setCapacity(CreativeAirtightTankBlockEntity.getCapacityPerTank());
    }

    void setContainedGas(GasStack gasStack) {
        tank().setContainedGas(gasStack);
    }

    void mergeTankStateFrom(IGasTankMultiBlockEntityContainer source) {
        if (!source.hasTank()) {
            return;
        }

        GasStack sourceGas = source.getGas(0);
        CreativeSmartGasTank tank = tank();
        if (tank.getGasStack().isEmpty() && !sourceGas.isEmpty()) {
            tank.setContainedGas(sourceGas);
        }
        source.clearTankStateAfterMerge(0);
    }

    void clearTankState() {
        tank().setContainedGas(GasStack.EMPTY);
    }

    void applySplitTankState(GasStack state) {
        tank().setContainedGas(state);
    }

    GasStack prepareTankStateForSplit() {
        resetCapacity();
        return owner.getGas(0);
    }

    private CreativeSmartGasTank tank() {
        return (CreativeSmartGasTank) owner.getTankInventory();
    }
}
