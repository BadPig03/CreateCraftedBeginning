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
@SuppressWarnings("unused")
public class GasTank implements IGasHandler, IGasTank {
    private static final String COMPOUND_KEY_GAS = "Gas";

    protected Predicate<GasStack> validator;
    protected GasStack gas = GasStack.EMPTY;
    protected long capacity;

    /**
     * Creates a new {@code GasTank} instance.
     *
     * @param capacity the capacity to use
     */
    public GasTank(long capacity) {
        this(capacity, e -> true);
    }

    /**
     * Creates a new {@code GasTank} instance.
     *
     * @param capacity  the capacity to use
     * @param validator the predicate used to validate inserted gas stacks
     */
    public GasTank(long capacity, Predicate<GasStack> validator) {
        this.capacity = Math.max(0, capacity);
        this.validator = validator;
    }

    /**
     * Sets the validator.
     *
     * @param predicate the predicate used to select matching values
     * @return this builder for chaining
     */
    public GasTank setValidator(Predicate<GasStack> predicate) {
        validator = predicate;
        return this;
    }

    /**
     * Reads this object's state from the supplied serialized data.
     *
     * @param provider    the provider used to resolve the requested value
     * @param compoundTag the NBT compound to read from or write to
     */
    public void read(Provider provider, CompoundTag compoundTag) {
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return;
        }

        gas = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS));
    }

    /**
     * Writes this object's state to the supplied serialized data.
     *
     * @param provider    the provider used to resolve the requested value
     * @param compoundTag the NBT compound to read from or write to
     * @return the resulting compound tag
     */
    public CompoundTag write(Provider provider, CompoundTag compoundTag) {
        compoundTag.put(COMPOUND_KEY_GAS, gas.saveOptional(provider));
        return compoundTag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return isEmpty() || isGasValid(stack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, gas) ? GasStack.EMPTY : drain(resource.getAmount(), action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        long drained = Math.min(maxDrain, gas.getAmount());
        GasStack stack = gas.copyWithAmount(drained);
        if (action.execute() && drained > 0) {
            gas.shrink(drained);
            onContentsChanged();
        }
        return stack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack getGasInTank(int tank) {
        return gas;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTanks() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
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
        if (amountToTransfer > 0) {
            onContentsChanged();
        }
        return amountToTransfer;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGasValid(GasStack stack) {
        return validator.test(stack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack getGasStack() {
        return gas;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCapacity() {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getGasAmount() {
        return gas.getAmount();
    }

    /**
     * Sets the capacity.
     *
     * @param newCapacity the replacement tank capacity
     * @return this builder for chaining
     */
    public GasTank setCapacity(long newCapacity) {
        capacity = Math.max(0, newCapacity);
        return this;
    }

    /**
     * Sets the gas stack.
     *
     * @param stack the stack to inspect or process
     */
    public void setGasStack(GasStack stack) {
        gas = stack;
    }

    protected void onContentsChanged() {
    }

    /**
     * Checks whether this value is empty.
     *
     * @return {@code true} if this value is empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        return gas.isEmpty();
    }

    /**
     * Returns the space.
     *
     * @return the space
     */
    public long getSpace() {
        return Math.max(0, capacity - gas.getAmount());
    }
}
