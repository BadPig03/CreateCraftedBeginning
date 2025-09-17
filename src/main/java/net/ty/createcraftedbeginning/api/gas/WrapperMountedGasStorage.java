package net.ty.createcraftedbeginning.api.gas;

import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.jetbrains.annotations.NotNull;

public abstract class WrapperMountedGasStorage<T extends IGasHandler> extends MountedGasStorage {
    protected final T wrapped;

    protected WrapperMountedGasStorage(MountedGasStorageType<?> type, T wrapped) {
        super(type);
        this.wrapped = wrapped;
    }

    @Override
    public int getTanks() {
        return this.wrapped.getTanks();
    }

    @Override
    @NotNull
    public GasStack getGasInTank(int tank) {
        return this.wrapped.getGasInTank(tank);
    }

    @Override
    public long getTankCapacity(int tank) {
        return this.wrapped.getTankCapacity(tank);
    }

    @Override
    public boolean isGasValid(int tank, @NotNull GasStack stack) {
        return this.wrapped.isGasValid(tank, stack);
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return this.wrapped.fill(resource, action);
    }

    @Override
    @NotNull
    public GasStack drain(GasStack resource, GasAction action) {
        return this.wrapped.drain(resource, action);
    }

    @Override
    @NotNull
    public GasStack drain(long maxDrain, GasAction action) {
        return this.wrapped.drain(maxDrain, action);
    }
}
