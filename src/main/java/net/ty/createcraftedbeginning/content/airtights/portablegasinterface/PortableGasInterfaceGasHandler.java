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
    public boolean isGasValid(int tank, GasStack stack) {
        return wrapped.isGasValid(tank, stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (!canAccessStorage()) {
            return GasStack.EMPTY;
        }

        GasStack drained = wrapped.drain(resource, action);
        keepAliveIfTransferred(!drained.isEmpty(), action);
        return drained;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (!canAccessStorage()) {
            return GasStack.EMPTY;
        }

        GasStack drained = wrapped.drain(maxDrain, action);
        keepAliveIfTransferred(!drained.isEmpty(), action);
        return drained;
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
        if (!canAccessStorage()) {
            return 0;
        }

        long filled = wrapped.fill(resource, action);
        keepAliveIfTransferred(filled > 0, action);
        return filled;
    }

    @Override
    public long getTankCapacity(int tank) {
        return wrapped.getTankCapacity(tank);
    }

    private boolean canAccessStorage() {
        return gasInterface.canAccessGasStorage(this);
    }

    private void keepAliveIfTransferred(boolean transferred, GasAction action) {
        if (!transferred || !action.execute()) {
            return;
        }

        gasInterface.onGasContentTransferred();
    }
}
