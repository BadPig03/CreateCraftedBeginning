package net.ty.createcraftedbeginning.api.gas;

import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
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
        return wrapped.getTanks();
    }

    @Override
    @NotNull
    public GasStack getGasInTank(int tank) {
        return wrapped.getGasInTank(tank);
    }

    @Override
    public long getTankCapacity(int tank) {
        return wrapped.getTankCapacity(tank);
    }

    @Override
    public boolean isGasValid(int tank, @NotNull GasStack stack) {
        return wrapped.isGasValid(tank, stack);
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return wrapped.fill(resource, action);
    }

    @Override
    @NotNull
    public GasStack drain(GasStack resource, GasAction action) {
        return wrapped.drain(resource, action);
    }

    @Override
    @NotNull
    public GasStack drain(long maxDrain, GasAction action) {
        return wrapped.drain(maxDrain, action);
    }
}
