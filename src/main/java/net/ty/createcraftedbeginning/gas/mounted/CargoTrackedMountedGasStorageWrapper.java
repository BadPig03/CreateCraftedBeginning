package net.ty.createcraftedbeginning.gas.mounted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.gas.storage.GasPressureCompartmentView;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CargoTrackedMountedGasStorageWrapper extends MountedGasStorageWrapper {
    private final Runnable changeListener;

    public CargoTrackedMountedGasStorageWrapper(MountedGasStorageWrapper wrapped, Runnable changeListener) {
        super(wrapped.storages);
        this.changeListener = changeListener;
    }

    @Override
    public GasPressureCompartment getPressureCompartment(int tank) {
        return new CargoTrackedPressureCompartment(super.getPressureCompartment(tank), changeListener);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        GasStack drained = super.drain(resource, action);
        trackDrain(drained, action);
        return drained;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        GasStack drained = super.drain(maxDrain, action);
        trackDrain(drained, action);
        return drained;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        long filled = super.fill(resource, action);
        trackFill(filled, action);
        return filled;
    }

    private void trackDrain(GasStack drained, GasAction action) {
        if (!action.execute() || drained.isEmpty()) {
            return;
        }

        changeListener.run();
    }

    private void trackFill(long filled, GasAction action) {
        if (!action.execute() || filled <= 0) {
            return;
        }

        changeListener.run();
    }

    private static final class CargoTrackedPressureCompartment extends GasPressureCompartmentView {
        private final Runnable changeListener;

        private CargoTrackedPressureCompartment(GasPressureCompartment wrapped, Runnable changeListener) {
            super(wrapped);
            this.changeListener = changeListener;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            GasStack drained = super.drain(resource, action);
            trackDrain(drained, action);
            return drained;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            GasStack drained = super.drain(maxDrain, action);
            trackDrain(drained, action);
            return drained;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            long filled = super.fill(resource, action);
            trackFill(filled, action);
            return filled;
        }

        @Override
        public long restoreDrainedGas(GasStack resource, GasAction action) {
            long restored = super.restoreDrainedGas(resource, action);
            trackFill(restored, action);
            return restored;
        }

        private void trackDrain(GasStack drained, GasAction action) {
            if (!action.execute() || drained.isEmpty()) {
                return;
            }

            changeListener.run();
        }

        private void trackFill(long filled, GasAction action) {
            if (!action.execute() || filled <= 0) {
                return;
            }

            changeListener.run();
        }
    }
}
