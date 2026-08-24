package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class PortableGasInterfaceGasHandler implements IGasHandler {
    private final PortableGasInterfaceBlockEntity gasInterface;
    private final IGasHandler wrapped;

    PortableGasInterfaceGasHandler(PortableGasInterfaceBlockEntity gasInterface, IGasHandler wrapped) {
        this.gasInterface = gasInterface;
        this.wrapped = wrapped;
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
    public long getTankCapacity(int tankIndex) {
        return wrapped.getTankCapacity(tankIndex);
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
}
