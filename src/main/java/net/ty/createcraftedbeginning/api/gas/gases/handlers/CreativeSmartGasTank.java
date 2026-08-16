package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeSmartGasTank extends SmartGasTank {
    public static final Codec<CreativeSmartGasTank> CODEC = RecordCodecBuilder.create(instance -> instance.group(GasStack.OPTIONAL_CODEC.fieldOf("gas").forGetter(GasTank::getGasStack), Codec.LONG.fieldOf("capacity").forGetter(GasTank::getCapacity)).apply(instance, (gas, capacity) -> {
        CreativeSmartGasTank tank = new CreativeSmartGasTank(capacity, stack -> {});
        tank.setGasStack(gas);
        return tank;
    }));

    public CreativeSmartGasTank(long capacity, Consumer<GasStack> updateCallback) {
        super(capacity, updateCallback);
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
    public long fill(GasStack resource, GasAction action) {
        return resource.getAmount();
    }

    @Override
    public long getGasAmount() {
        if (getGasStack().isEmpty()) {
            return 0;
        }
        return getTankCapacity(0);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        return AtomicFillResult.SUCCESS;
    }

    public void setContainedGas(GasStack gasStack) {
        gas = gasStack.copy();
        if (!gasStack.isEmpty()) {
            gas.setAmount(getTankCapacity(0));
        }
        onContentsChanged();
    }
}
