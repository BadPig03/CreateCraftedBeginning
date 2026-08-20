package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
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
    public void read(Provider provider, CompoundTag compoundTag) {
        super.read(provider, compoundTag);
        normalizeStoredGas();
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
    public GasStack getGasInTank(int tank) {
        return getGasStack();
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return resource.getAmount();
    }

    @Override
    public GasStack getGasStack() {
        return gas.copy();
    }

    @Override
    public long getGasAmount() {
        if (gas.isEmpty()) {
            return 0;
        }
        return getTankCapacity(0);
    }

    @Override
    public CreativeSmartGasTank setCapacity(long newCapacity) {
        super.setCapacity(newCapacity);
        normalizeStoredGas();
        return this;
    }

    @Override
    public void setGasStack(GasStack stack) {
        gas = normalizedCopy(stack);
        onContentsChanged();
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty()) {
                continue;
            }

            if (fill(resource, GasAction.SIMULATE) != resource.getAmount()) {
                return AtomicFillResult.REJECTED;
            }
        }
        return AtomicFillResult.SUCCESS;
    }

    public void setContainedGas(GasStack gasStack) {
        setGasStack(gasStack);
    }

    private void normalizeStoredGas() {
        gas = normalizedCopy(gas);
    }

    private GasStack normalizedCopy(GasStack stack) {
        long normalizedAmount = getCapacity();
        if (stack.isEmpty() || normalizedAmount <= 0) {
            return GasStack.EMPTY;
        }
        return stack.copyWithAmount(normalizedAmount);
    }
}
