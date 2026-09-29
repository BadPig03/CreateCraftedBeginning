package net.ty.createcraftedbeginning.gas.storage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPressureCompartmentView implements GasPressureCompartment {
    protected final GasPressureCompartment wrapped;

    public GasPressureCompartmentView(GasPressureCompartment wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Object getCompartmentIdentity() {
        return wrapped.getCompartmentIdentity();
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
    public long fill(GasStack resource, GasAction action) {
        return wrapped.fill(resource, action);
    }

    @Override
    public boolean isGasValid(GasStack stack) {
        return wrapped.isGasValid(stack);
    }

    @Override
    public GasStack getGasStack() {
        return wrapped.getGasStack();
    }

    @Override
    public boolean supportsExactDrainRecovery() {
        return wrapped.supportsExactDrainRecovery();
    }

    @Override
    public long restoreDrainedGas(GasStack resource, GasAction action) {
        return wrapped.restoreDrainedGas(resource, action);
    }

    @Override
    public long getVolume() {
        return wrapped.getVolume();
    }

    @Override
    public long getPressurePa() {
        return wrapped.getPressurePa();
    }

    @Override
    public long getMaxPressurePa() {
        return wrapped.getMaxPressurePa();
    }

    @Override
    public long getMaxAmount() {
        return wrapped.getMaxAmount();
    }

    @Override
    public long getStoredAmount() {
        return wrapped.getStoredAmount();
    }

    @Override
    public PressureModel getPressureModel() {
        return wrapped.getPressureModel();
    }
}
