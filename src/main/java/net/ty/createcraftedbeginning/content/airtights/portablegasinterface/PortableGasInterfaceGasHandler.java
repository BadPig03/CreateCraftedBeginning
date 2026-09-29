package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.gas.storage.GasPressureCompartmentView;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class PortableGasInterfaceGasHandler implements GasStorageHandler {
    private final PortableGasInterfaceBlockEntity gasInterface;
    private final GasStorageHandler wrapped;

    PortableGasInterfaceGasHandler(PortableGasInterfaceBlockEntity gasInterface, GasStorageHandler wrapped) {
        this.gasInterface = gasInterface;
        this.wrapped = wrapped;
    }

    @Override
    public GasPressureCompartment getPressureCompartment(int tankIndex) {
        return new PortablePressureCompartment(wrapped.getPressureCompartment(tankIndex));
    }

    @Override
    public boolean isGasValid(int tankIndex, GasStack gasStack) {
        return wrapped.isGasValid(tankIndex, gasStack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (!canAccessStorage()) {
            return GasStack.EMPTY;
        }

        GasStack drainedGas = wrapped.drain(resource, action);
        keepAliveIfTransferred(!drainedGas.isEmpty(), action);
        return drainedGas;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (!canAccessStorage()) {
            return GasStack.EMPTY;
        }

        GasStack drainedGas = wrapped.drain(maxDrain, action);
        keepAliveIfTransferred(!drainedGas.isEmpty(), action);
        return drainedGas;
    }

    @Override
    public GasStack getGasInTank(int tankIndex) {
        return wrapped.getGasInTank(tankIndex);
    }

    @Override
    public int getTanks() {
        return wrapped.getTanks();
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (!canAccessStorage()) {
            return 0;
        }

        long filledAmount = wrapped.fill(resource, action);
        keepAliveIfTransferred(filledAmount > 0, action);
        return filledAmount;
    }

    @Override
    public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
        boolean hasGasToFill = resources.stream().anyMatch(resource -> resource != null && !resource.isEmpty());
        if (!hasGasToFill) {
            return AtomicFillResult.SUCCESS;
        }

        if (!canAccessStorage()) {
            return AtomicFillResult.REJECTED;
        }

        AtomicFillResult result = wrapped.tryFillAtomicallyFromPressure(resources, sourcePressurePa, action);
        keepAliveIfTransferred(result.isSuccess(), action);
        return result;
    }

    @Override
    public long getTankVolume(int tankIndex) {
        return wrapped.getTankVolume(tankIndex);
    }

    @Override
    public long getTankPressurePa(int tankIndex) {
        return wrapped.getTankPressurePa(tankIndex);
    }

    @Override
    public long getTankMaxPressurePa(int tankIndex) {
        return wrapped.getTankMaxPressurePa(tankIndex);
    }

    @Override
    public long getTankMaxAmount(int tankIndex) {
        return wrapped.getTankMaxAmount(tankIndex);
    }

    @Override
    public PressureModel getTankPressureModel(int tankIndex) {
        return wrapped.getTankPressureModel(tankIndex);
    }

    private boolean canAccessStorage() {
        return gasInterface.canAccessGasStorage(this);
    }

    private void keepAliveIfTransferred(boolean didTransfer, GasAction action) {
        if (!didTransfer || !action.execute()) {
            return;
        }

        gasInterface.onGasContentTransferred();
    }

    private final class PortablePressureCompartment extends GasPressureCompartmentView {
        private PortablePressureCompartment(GasPressureCompartment wrapped) {
            super(wrapped);
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (!canAccessStorage()) {
                return GasStack.EMPTY;
            }

            GasStack drainedGas = super.drain(resource, action);
            keepAliveIfTransferred(!drainedGas.isEmpty(), action);
            return drainedGas;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (!canAccessStorage()) {
                return GasStack.EMPTY;
            }

            GasStack drainedGas = super.drain(maxDrain, action);
            keepAliveIfTransferred(!drainedGas.isEmpty(), action);
            return drainedGas;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (!canAccessStorage()) {
                return 0;
            }

            long filledAmount = super.fill(resource, action);
            keepAliveIfTransferred(filledAmount > 0, action);
            return filledAmount;
        }

        @Override
        public long restoreDrainedGas(GasStack resource, GasAction action) {
            long restoredAmount = super.restoreDrainedGas(resource, action);
            keepAliveIfTransferred(restoredAmount > 0, action);
            return restoredAmount;
        }
    }

}
