package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
    public GasStack getGasInTank(int tank) {
        return wrapped.getGasInTank(tank);
    }

    @Override
    public long getTankCapacity(int tank) {
        return wrapped.getTankCapacity(tank);
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return wrapped.isGasValid(tank, stack);
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return wrapped.fill(resource, action);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return wrapped.drain(resource, action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return wrapped.drain(maxDrain, action);
    }
}
