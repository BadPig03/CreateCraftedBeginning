package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchDisplay {
    private final AirtightHatchBlockEntity hatch;

    AirtightHatchDisplay(AirtightHatchBlockEntity hatch) {
        this.hatch = hatch;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (hatch.isEmpty()) {
            return false;
        }

        SmartGasTank gasTank = hatch.getGasTankBehaviour().getPrimaryHandler();
        GasUnitsTooltips.addContainer(tooltip, gasTank, hatch.isCreative());
        return true;
    }

    int getMaxValue() {
        if (hatch.isEmpty()) {
            return 0;
        }

        return GasUnits.toKilo(hatch.getGasTankBehaviour().getPrimaryHandler().getMaxAmount());
    }

    int getCurrentValue() {
        if (hatch.isEmpty()) {
            return 0;
        }

        return GasUnits.toKilo(hatch.getHatchGasContent().getAmount());
    }

    MutableComponent format(int value) {
        return GasUnitFormat.formatKilo(value);
    }
}
