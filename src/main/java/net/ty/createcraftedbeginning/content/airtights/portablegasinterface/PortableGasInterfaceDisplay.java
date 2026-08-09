package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class PortableGasInterfaceDisplay {
    private final PortableGasInterfaceBlockEntity gasInterface;

    PortableGasInterfaceDisplay(PortableGasInterfaceBlockEntity gasInterface) {
        this.gasInterface = gasInterface;
    }

    float getExtensionDistance(float partialTicks) {
        float animation = gasInterface.getConnectionAnimationValue(partialTicks);
        return Mth.square(animation) * gasInterface.getDistance() * 0.5f;
    }

    int getMaxValue() {
        return GasAmountUtils.toWholeBucketsClamped(gasInterface.getGasCapability().getTankCapacity(0));
    }

    int getCurrentValue() {
        return GasAmountUtils.toWholeBucketsClamped(gasInterface.getGasCapability().getGasInTank(0).getAmount());
    }

    MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }
}
