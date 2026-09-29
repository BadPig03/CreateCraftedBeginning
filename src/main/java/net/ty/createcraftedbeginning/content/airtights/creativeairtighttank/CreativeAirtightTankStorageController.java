package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockPart;
import net.ty.createcraftedbeginning.gas.storage.CreativeGasReservoir;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CreativeAirtightTankStorageController {
    private final CreativeAirtightTankBlockEntity owner;

    CreativeAirtightTankStorageController(CreativeAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    void resetReservoirLimits() {
        tank().reconfigure(new GasTankLimits(CreativeAirtightTankBlockEntity.getVolumePerBlock(), CreativeAirtightTankPressureBehaviour.MAX_PRESSURE_PA));
    }

    void setContainedGas(GasStack gasStack) {
        tank().setContainedGas(gasStack);
    }

    void mergeTankStateFrom(GasTankMultiblockPart source) {
        if (!source.hasTank()) {
            return;
        }

        GasStack sourceGas = source.getGas(0);
        CreativeGasReservoir tank = tank();
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
        resetReservoirLimits();
        return owner.getGas(0);
    }

    private CreativeGasReservoir tank() {
        return owner.getTankInventory();
    }
}
