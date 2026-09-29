package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.gas.storage.handler.GasInventorySummary;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class PortableGasInterfaceDisplay {
    private final PortableGasInterfaceBlockEntity gasInterface;

    PortableGasInterfaceDisplay(PortableGasInterfaceBlockEntity gasInterface) {
        this.gasInterface = gasInterface;
    }

    float getExtensionDistance(float partialTicks) {
        return Mth.square(gasInterface.getConnectionAnimationValue(partialTicks)) * gasInterface.getDistance() * 0.5F;
    }

    int getMaxValue() {
        return GasUnits.toKilo(getInventorySummary().capacity());
    }

    int getCurrentValue() {
        return GasUnits.toKilo(getInventorySummary().amount());
    }

    MutableComponent format(int kiloGasUnits) {
        return GasUnitFormat.formatKilo(kiloGasUnits);
    }

    private GasInventorySummary getInventorySummary() {
        return GasInventorySummary.finiteStorage(gasInterface.getGasCapability());
    }
}
