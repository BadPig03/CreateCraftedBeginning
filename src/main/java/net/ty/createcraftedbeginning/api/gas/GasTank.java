package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasTank;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@SuppressWarnings("unused")
public class GasTank implements IGasHandler, IGasTank {
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

    public GasTank setValidator(Predicate<GasStack> validator) {
        if (validator != null) {
            this.validator = validator;
        }
        return this;
    }

    public GasStack getGas() {
        return gas;
    }

    public long getGasAmount() {
        return gas.getAmount();
    }

    public long getCapacity() {
        return capacity;
    }

    public boolean isGasValid(GasStack stack) {
        return validator.test(stack);
    }

    public GasTank setCapacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public void setGas(GasStack stack) {
        this.gas = stack;
    }

    public void readFromNBT(HolderLookup.Provider lookupProvider, @NotNull CompoundTag nbt) {
        gas = GasStack.parseOptional(lookupProvider, nbt.getCompound("Gas"));
    }

    public CompoundTag writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        if (!gas.isEmpty()) {
            nbt.put("Gas", gas.save(lookupProvider));
        }

        return nbt;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return getGas();
    }

    @Override
    public long getTankCapacity(int tank) {
        return getCapacity();
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        if (isEmpty()) {
            return true;
        }
        return isGasValid(stack);
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

            if (!GasStack.isSameGas(gas, resource)) {
                return 0;
            }
            return Math.min(capacity - gas.getAmount(), resource.getAmount());
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
        if (resource.isEmpty() || !GasStack.isSameGas(resource, gas)) {
            return GasStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
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

    protected void onContentsChanged() {
    }

    public boolean isEmpty() {
        return gas.isEmpty();
    }

    public long getSpace() {
        return Math.max(0, capacity - gas.getAmount());
    }
}
