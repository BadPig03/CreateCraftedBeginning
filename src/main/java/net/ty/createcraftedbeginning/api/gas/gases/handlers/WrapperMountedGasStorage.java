package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class WrapperMountedGasStorage<T extends IGasHandler> extends MountedGasStorage {
    protected final T wrapped;

    protected WrapperMountedGasStorage(MountedGasStorageType<?> type, T wrapped) {
        super(type);
        this.wrapped = wrapped;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return wrapped.isGasValid(tank, stack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return wrapped.drain(resource, action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return wrapped.drain(maxDrain, action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasStack getGasInTank(int tank) {
        return wrapped.getGasInTank(tank);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTanks() {
        return wrapped.getTanks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long fill(GasStack resource, GasAction action) {
        return wrapped.fill(resource, action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        return wrapped.tryFillAtomically(resources, action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTankCapacity(int tank) {
        return wrapped.getTankCapacity(tank);
    }
}
