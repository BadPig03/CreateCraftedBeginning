package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class PortableGasInterfaceDisplay {
    private final PortableGasInterfaceBlockEntity gasInterface;

    PortableGasInterfaceDisplay(PortableGasInterfaceBlockEntity gasInterface) {
        this.gasInterface = gasInterface;
    }

    float getExtensionDistance(float partialTicks) {
        return Mth.square(gasInterface.getConnectionAnimationValue(partialTicks)) * gasInterface.getDistance() * 0.5f;
    }

    int getMaxValue() {
        return GasAmounts.toWholeBucketsClamped(gasInterface.getGasCapability().getTankCapacity(0));
    }

    int getCurrentValue() {
        return GasAmounts.toWholeBucketsClamped(gasInterface.getGasCapability().getGasInTank(0).getAmount());
    }

    MutableComponent format(int bucketAmount) {
        return GasAmounts.formatWholeBuckets(bucketAmount);
    }
}
