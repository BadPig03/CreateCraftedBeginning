package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CreativeAirtightTankDisplay {
    private final CreativeAirtightTankBlockEntity owner;

    CreativeAirtightTankDisplay(CreativeAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (owner.getLevel() == null) {
            return false;
        }

        CreativeAirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return false;
        }

        GasStorageHandler gasHandler = controller.getCapability();
        GasUnitsTooltips.addContainer(tooltip, gasHandler, true, controller);
        return true;
    }

    int getMaxValue() {
        if (owner.getControllerBE() == null) {
            return 0;
        }

        return GasUnits.toKilo(CreativeAirtightTankBlockEntity.getVolumePerBlock());
    }

    int getCurrentValue() {
        CreativeAirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return 0;
        }

        GasStack gasStack = controller.getCapability().getGasInTank(0);
        if (gasStack.isEmpty()) {
            return 0;
        }

        return GasUnits.toKilo(CreativeAirtightTankBlockEntity.getVolumePerBlock());
    }

    MutableComponent format(int value) {
        return GasUnitFormat.formatKilo(value);
    }
}
