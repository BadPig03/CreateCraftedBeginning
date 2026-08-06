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

    /**
     * Creates a new {@code CreativeSmartGasTank} instance.
     *
     * @param capacity       the capacity to use
     * @param updateCallback the callback invoked when the stored gas changes
     */
    public CreativeSmartGasTank(long capacity, Consumer<GasStack> updateCallback) {
        super(capacity, updateCallback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return super.drain(resource, GasAction.SIMULATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return super.drain(maxDrain, GasAction.SIMULATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long fill(GasStack resource, GasAction action) {
        return resource.getAmount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getGasAmount() {
        return getGasStack().isEmpty() ? 0 : getTankCapacity(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        return AtomicFillResult.SUCCESS;
    }

    /**
     * Sets the contained gas.
     *
     * @param gasStack the gas stack to inspect or process
     */
    public void setContainedGas(GasStack gasStack) {
        gas = gasStack.copy();
        if (!gasStack.isEmpty()) {
            gas.setAmount(getTankCapacity(0));
        }
        onContentsChanged();
    }
}
