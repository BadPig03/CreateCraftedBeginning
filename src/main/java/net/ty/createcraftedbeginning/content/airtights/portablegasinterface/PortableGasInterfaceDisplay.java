package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PortableGasInterfaceDisplay {
    private final PortableGasInterfaceBlockEntity gasInterface;

    public PortableGasInterfaceDisplay(PortableGasInterfaceBlockEntity gasInterface) {
        this.gasInterface = gasInterface;
    }

    public float getExtensionDistance(float partialTicks) {
        float animation = gasInterface.getConnectionAnimationValue(partialTicks);
        return Mth.square(animation) * gasInterface.getDistance() * 0.5f;
    }

    public int getMaxValue() {
        return GasAmounts.toWholeBucketsClamped(gasInterface.getGasCapability().getTankCapacity(0));
    }

    public int getCurrentValue() {
        return GasAmounts.toWholeBucketsClamped(gasInterface.getGasCapability().getGasInTank(0).getAmount());
    }

    public MutableComponent format(int value) {
        return GasAmounts.formatWholeBuckets(value);
    }
}
