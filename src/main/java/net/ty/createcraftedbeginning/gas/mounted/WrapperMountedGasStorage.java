package net.ty.createcraftedbeginning.gas.mounted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class WrapperMountedGasStorage<T extends GasStorageHandler> extends MountedGasStorage {
    protected final T wrapped;

    protected WrapperMountedGasStorage(MountedGasStorageType<?> type, T wrapped) {
        super(type);
        this.wrapped = wrapped;
    }

    @Override
    public GasPressureCompartment getPressureCompartment(int tank) {
        return wrapped.getPressureCompartment(tank);
    }

    @Override
    public long getTankVolume(int tank) {
        return wrapped.getTankVolume(tank);
    }

    @Override
    public long getTankPressurePa(int tank) {
        return wrapped.getTankPressurePa(tank);
    }

    @Override
    public long getTankMaxPressurePa(int tank) {
        return wrapped.getTankMaxPressurePa(tank);
    }

    @Override
    public long getTankMaxAmount(int tank) {
        return wrapped.getTankMaxAmount(tank);
    }

    @Override
    public PressureModel getTankPressureModel(int tank) {
        return wrapped.getTankPressureModel(tank);
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return wrapped.isGasValid(tank, stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return wrapped.drain(resource, action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return wrapped.drain(maxDrain, action);
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return wrapped.getGasInTank(tank);
    }

    @Override
    public int getTanks() {
        return wrapped.getTanks();
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return wrapped.fill(resource, action);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        return wrapped.tryFillAtomically(resources, action);
    }

    @Override
    public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
        return wrapped.tryFillAtomicallyFromPressure(resources, sourcePressurePa, action);
    }
}
