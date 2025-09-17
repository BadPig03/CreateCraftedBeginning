package net.ty.createcraftedbeginning.api.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ty.createcraftedbeginning.data.CCBSerializerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CreativeSmartGasTank extends SmartGasTank {
    public static final Codec<CreativeSmartGasTank> CODEC = RecordCodecBuilder.create(i -> i.group(GasStack.OPTIONAL_CODEC.fieldOf("gas").forGetter(GasTank::getGas), CCBSerializerHelper.NON_NEGATIVE_LONG_CODEC.fieldOf("capacity").forGetter(GasTank::getCapacity)).apply(i, (gas, capacity) -> {
        CreativeSmartGasTank tank = new CreativeSmartGasTank(capacity, $ -> {
        });
        tank.setGas(gas);
        return tank;
    }));

    public CreativeSmartGasTank(long capacity, Consumer<GasStack> updateCallback) {
        super(capacity, updateCallback);
    }

    @Override
    public long getGasAmount() {
        return getGas().isEmpty() ? 0 : getTankCapacity(0);
    }

    @Override
    public long fill(@NotNull GasStack resource, GasAction action) {
        return resource.getAmount();
    }

    @Override
    public GasStack drain(@NotNull GasStack resource, GasAction action) {
        return super.drain(resource, GasAction.SIMULATE);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return super.drain(maxDrain, GasAction.SIMULATE);
    }

    public void setContainedGas(@NotNull GasStack gasStack) {
        gas = gasStack.copy();
        if (!gasStack.isEmpty()) {
            gas.setAmount(getTankCapacity(0));
        }
        onContentsChanged();
    }
}
