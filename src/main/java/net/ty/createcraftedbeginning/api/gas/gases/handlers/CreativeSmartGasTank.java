package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeSmartGasTank extends SmartGasTank {
    public static final Codec<CreativeSmartGasTank> CODEC = RecordCodecBuilder.create(i -> i.group(GasStack.OPTIONAL_CODEC.fieldOf("gas").forGetter(GasTank::getGasStack), Codec.LONG.fieldOf("capacity").forGetter(GasTank::getCapacity)).apply(i, (gas, capacity) -> {
        CreativeSmartGasTank tank = new CreativeSmartGasTank(capacity, gasStack -> {});
        tank.setGasStack(gas);
        return tank;
    }));

    public CreativeSmartGasTank(long capacity, Consumer<GasStack> updateCallback) {
        super(capacity, updateCallback);
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return resource.getAmount();
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return super.drain(resource, GasAction.SIMULATE);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return super.drain(maxDrain, GasAction.SIMULATE);
    }

    @Override
    public long getGasAmount() {
        return getGasStack().isEmpty() ? 0 : getTankCapacity(0);
    }

    public void setContainedGas(GasStack gasStack) {
        gas = gasStack.copy();
        if (!gasStack.isEmpty()) {
            gas.setAmount(getTankCapacity(0));
        }
        onContentsChanged();
    }
}
