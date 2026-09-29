package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.gas.storage.GasPressureCompartmentView;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AirtightReactorKettleGasPortHandler(GasStorageHandler input, GasStorageHandler output) implements GasStorageHandler {
    @Override
    public GasPressureCompartment getPressureCompartment(int tank) {
        GasStorageHandler handler = getHandler(tank);
        GasPressureCompartment compartment = handler.getPressureCompartment(getLocalTank(tank));
        return new PortPressureCompartment(compartment, tank < input.getTanks(), output);
    }

    @Override
    public long getTankVolume(int tank) {
        return getHandler(tank).getTankVolume(getLocalTank(tank));
    }

    @Override
    public long getTankPressurePa(int tank) {
        return getHandler(tank).getTankPressurePa(getLocalTank(tank));
    }

    @Override
    public long getTankMaxPressurePa(int tank) {
        return getHandler(tank).getTankMaxPressurePa(getLocalTank(tank));
    }

    @Override
    public long getTankMaxAmount(int tank) {
        return getHandler(tank).getTankMaxAmount(getLocalTank(tank));
    }

    @Override
    public PressureModel getTankPressureModel(int tank) {
        return getHandler(tank).getTankPressureModel(getLocalTank(tank));
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        getHandler(tank);
        return tank < input.getTanks() && input.isGasValid(tank, stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        GasStack drained = output.drain(resource, action);
        if (!drained.isEmpty()) {
            return drained;
        }

        return input.drain(resource, action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        GasStack drained = output.drain(maxDrain, action);
        if (!drained.isEmpty()) {
            return drained;
        }

        return input.drain(maxDrain, action);
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return getHandler(tank).getGasInTank(getLocalTank(tank));
    }

    @Override
    public int getTanks() {
        return input.getTanks() + output.getTanks();
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return input.fill(resource, action);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        return input.tryFillAtomically(resources, action);
    }

    @Override
    public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
        return input.tryFillAtomicallyFromPressure(resources, sourcePressurePa, action);
    }

    private GasStorageHandler getHandler(int tank) {
        if (tank < 0 || tank >= getTanks()) {
            throw new IndexOutOfBoundsException("Tank index must be in [0, " + getTanks() + "); got " + tank + '.');
        }

        if (tank < input.getTanks()) {
            return input;
        }

        return output;
    }

    private int getLocalTank(int tank) {
        if (tank < input.getTanks()) {
            return tank;
        }

        return tank - input.getTanks();
    }

    private static final class PortPressureCompartment extends GasPressureCompartmentView {
        private final boolean input;
        private final GasStorageHandler output;

        private PortPressureCompartment(GasPressureCompartment wrapped, boolean input, GasStorageHandler output) {
            super(wrapped);
            this.input = input;
            this.output = output;
        }

        @Override
        public @Nullable PredictedTransferLimits predictTransferLimits(GasStack gas, long storedAmount) {
            if (input && hasMatchingOutput(gas)) {
                return new PredictedTransferLimits(0, 0);
            }

            if (!input && storedAmount <= 0 && !getGasStack().isEmpty()) {
                return null;
            }

            PredictedTransferLimits limits = wrapped.predictTransferLimits(gas, storedAmount);
            if (limits == null) {
                return null;
            }

            if (!canFill(gas)) {
                return new PredictedTransferLimits(limits.drainLimit(), 0);
            }

            return new PredictedTransferLimits(limits.drainLimit(), limits.fillLimit());
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (input && hasMatchingOutput(resource)) {
                return GasStack.EMPTY;
            }

            return super.drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            GasStack stored = getGasStack();
            if (input && hasMatchingOutput(stored)) {
                return GasStack.EMPTY;
            }

            return super.drain(maxDrain, action);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (!canFill(resource)) {
                return 0;
            }

            return super.fill(resource, action);
        }

        @Override
        public boolean isGasValid(GasStack stack) {
            return canFill(stack) && super.isGasValid(stack);
        }

        private boolean canFill(GasStack resource) {
            return input && !resource.isEmpty() && !hasMatchingOutput(resource);
        }

        private boolean hasMatchingOutput(GasStack resource) {
            if (resource.isEmpty()) {
                return false;
            }

            for (int tank = 0; tank < output.getTanks(); tank++) {
                GasStack stored = output.getGasInTank(tank);
                if (!stored.isEmpty() && GasStack.isSameGasSameComponents(stored, resource)) {
                    return true;
                }
            }
            return false;
        }
    }

}
