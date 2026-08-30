package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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

    @SuppressWarnings("unused")
    public GasTank setValidator(Predicate<GasStack> predicate) {
        validator = predicate;
        return this;
    }

    public void read(Provider provider, CompoundTag compoundTag) {
        gas = GasStack.EMPTY;
        if (!CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_GAS)) {
            return;
        }

        GasStack loadedGas = GasStack.parseOptional(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_GAS));
        if (!loadedGas.isEmpty() && !isGasValid(loadedGas)) {
            return;
        }
        gas = loadedGas;
    }

    public CompoundTag write(Provider provider, CompoundTag compoundTag) {
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_GAS, gas.saveOptional(provider));
        return compoundTag;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return isEmpty() || isGasValid(stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, gas)) {
            return GasStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
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
        return gas.copy();
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
        return gas.copy();
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public long getGasAmount() {
        return gas.getAmount();
    }

    /**
     * Changes the fill limit without truncating already stored gas. If the tank becomes over capacity,
     * {@link #getSpace()} remains zero so new gas is rejected while the existing contents can still be drained.
     */
    public GasTank setCapacity(long newCapacity) {
        capacity = Math.max(0, newCapacity);
        return this;
    }

    public void setGasStack(GasStack stack) {
        gas = stack.copy();
    }

    protected void onContentsChanged() {
    }

    public boolean isEmpty() {
        return gas.isEmpty();
    }

    public long getSpace() {
        return Math.max(0, CCBMathUtils.saturatedSubtract(capacity, gas.getAmount()));
    }
}
