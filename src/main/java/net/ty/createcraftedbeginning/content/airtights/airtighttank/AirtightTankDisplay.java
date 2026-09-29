package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankDisplay {
    private final AirtightTankBlockEntity owner;

    AirtightTankDisplay(AirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        AirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return false;
        }

        if (controller.getCore().addToGoggleTooltip(tooltip)) {
            tooltip.add(Component.empty());
        }
        GasStorageHandler gasHandler = controller.getTankInventory();
        GasUnitsTooltips.addContainer(tooltip, gasHandler, false, controller);
        OverpressureTooltips.addStatus(tooltip, controller.getOverpressureBehaviour(), gasHandler);
        return true;
    }

    int getMaxValue() {
        AirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return 0;
        }

        return GasUnits.toKilo(controller.getCapability().getTankMaxAmount(0));
    }

    int getCurrentValue() {
        AirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return 0;
        }

        GasStorageHandler gasHandler = controller.getCapability();
        long totalAmount = 0;
        for (int tank = 0; tank < gasHandler.getTanks(); tank++) {
            GasStack gasStack = gasHandler.getGasInTank(tank);
            if (gasStack.isEmpty()) {
                continue;
            }

            totalAmount += gasStack.getAmount();
        }
        return GasUnits.toKilo(totalAmount);
    }

    MutableComponent format(int value) {
        return GasUnitFormat.formatKilo(value);
    }
}
