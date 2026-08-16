package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasTank implements IGasHandler, IGasTank {
    private static final String COMPOUND_KEY_GAS = "Gas";

    protected Predicate<GasStack> validator;
    protected GasStack gas = GasStack.EMPTY;
    protected long capacity;

    public GasTank(long capacity) {
        this(capacity, e -> true);
    }

    public GasTank(long capacity, Predicate<GasStack> validator) {
        this.capacity = Math.max(0, capacity);
        this.validator = validator;
    }

    public GasTank setValidator(Predicate<GasStack> predicate) {
        validator = predicate;
        return this;
    }

    public void read(Provider provider, CompoundTag compoundTag) {
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return;
        }

        gas = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS));
    }

    public CompoundTag write(Provider provider, CompoundTag compoundTag) {
        compoundTag.put(COMPOUND_KEY_GAS, gas.saveOptional(provider));
        return compoundTag;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return isEmpty() || isGasValid(stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, gas) ? GasStack.EMPTY : drain(resource.getAmount(), action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        long drained = Math.min(maxDrain, gas.getAmount());
        GasStack stack = gas.copyWithAmount(drained);
        if (!action.execute() || drained <= 0) {
            return stack;
        }

        gas.shrink(drained);
        onContentsChanged();
        return stack;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return gas;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (resource.isEmpty() || !isGasValid(resource)) {
            return 0;
        }

        if (action.simulate()) {
            return getFillableAmount(resource);
        }

        if (gas.isEmpty()) {
            return fillEmptyTank(resource);
        }

        if (!GasStack.isSameGasSameComponents(gas, resource)) {
            return 0;
        }

        long remainingSpace = getSpace();
        long amountToTransfer = Math.min(remainingSpace, resource.getAmount());
        gas.grow(amountToTransfer);
        if (amountToTransfer <= 0) {
            return amountToTransfer;
        }

        onContentsChanged();
        return amountToTransfer;
    }

    @Override
    public long getTankCapacity(int tank) {
        return capacity;
    }

    private long getFillableAmount(GasStack resource) {
        if (gas.isEmpty()) {
            return Math.min(capacity, resource.getAmount());
        }

        if (!GasStack.isSameGasSameComponents(gas, resource)) {
            return 0;
        }
        return Math.min(getSpace(), resource.getAmount());
    }

    private long fillEmptyTank(GasStack resource) {
        long amount = Math.min(capacity, resource.getAmount());
        if (amount <= 0) {
            return 0;
        }

        gas = resource.copyWithAmount(amount);
        onContentsChanged();
        return amount;
    }

    @Override
    public boolean isGasValid(GasStack stack) {
        return validator.test(stack);
    }

    @Override
    public GasStack getGasStack() {
        return gas;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public long getGasAmount() {
        return gas.getAmount();
    }

    public GasTank setCapacity(long newCapacity) {
        capacity = Math.max(0, newCapacity);
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
