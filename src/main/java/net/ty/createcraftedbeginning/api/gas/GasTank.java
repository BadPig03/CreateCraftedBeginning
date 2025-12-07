package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasTank;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@SuppressWarnings("unused")
public class GasTank implements IGasHandler, IGasTank {
    private static final String COMPOUND_KEY_GAS = "Gas";

    protected Predicate<GasStack> validator;
    protected GasStack gas = GasStack.EMPTY;
    protected long capacity;

    public GasTank(long capacity) {
        this(capacity, e -> true);
    }

    public GasTank(long capacity, Predicate<GasStack> validator) {
        this.capacity = capacity;
        this.validator = validator;
    }

    public GasTank setValidator(Predicate<GasStack> predicate) {
        if (predicate != null) {
            validator = predicate;
        }
        return this;
    }

    public void read(Provider lookupProvider, @NotNull CompoundTag compoundTag) {
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return;
        }

        gas = GasStack.parseOptional(lookupProvider, compoundTag.getCompound(COMPOUND_KEY_GAS));
    }

    public CompoundTag write(Provider lookupProvider, @NotNull CompoundTag compoundTag) {
        compoundTag.put(COMPOUND_KEY_GAS, gas.saveOptional(lookupProvider));
        return compoundTag;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return gas;
    }

    @Override
    public long getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return isEmpty() || isGasValid(stack);
    }

    @Override
    public long fill(@NotNull GasStack resource, GasAction action) {
        if (resource.isEmpty() || !isGasValid(resource)) {
            return 0;
        }

        if (action.simulate()) {
            if (gas.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }

            return GasStack.isSameGas(gas, resource) ? Math.min(capacity - gas.getAmount(), resource.getAmount()) : 0;
        }

        if (gas.isEmpty()) {
            long amountToAdd = Math.min(capacity, resource.getAmount());
            gas = resource.copyWithAmount(amountToAdd);
            onContentsChanged();
            return amountToAdd;
        }

        if (!GasStack.isSameGas(gas, resource)) {
            return 0;
        }

        long remainingSpace = capacity - gas.getAmount();
        long amountToTransfer = Math.min(remainingSpace, resource.getAmount());
        gas.grow(amountToTransfer);
        if (amountToTransfer > 0) {
            onContentsChanged();
        }

        return amountToTransfer;
    }

    @Override
    public GasStack drain(@NotNull GasStack resource, GasAction action) {
        return resource.isEmpty() || !GasStack.isSameGas(resource, gas) ? GasStack.EMPTY : drain(resource.getAmount(), action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        long drained = maxDrain;
        if (gas.getAmount() < drained) {
            drained = gas.getAmount();
        }
        GasStack stack = gas.copyWithAmount(drained);
        if (action.execute() && drained > 0) {
            gas.shrink(drained);
            onContentsChanged();
        }

        return stack;
    }

    @Override
    public GasStack getGasStack() {
        return gas;
    }

    @Override
    public long getGasAmount() {
        return gas.getAmount();
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public boolean isGasValid(GasStack stack) {
        return validator.test(stack);
    }

    public GasTank setCapacity(long newCapacity) {
        capacity = newCapacity;
        return this;
    }

    public void setGasStack(GasStack stack) {
        gas = stack;
    }

    protected void onContentsChanged() {
    }

    public boolean isEmpty() {
        return gas.isEmpty();
    }

    public long getSpace() {
        return Math.max(0, capacity - gas.getAmount());
    }
}
