package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberGasHandler implements IGasHandler {
    private final IGasHandler delegate;
    private final BooleanSupplier locked;
    private final Supplier<GasStack> operationGas;

    GasInjectionChamberGasHandler(IGasHandler delegate, BooleanSupplier locked, Supplier<GasStack> operationGas) {
        this.delegate = delegate;
        this.locked = locked;
        this.operationGas = operationGas;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return (!locked.getAsBoolean() || GasStack.isSameGasSameComponents(stack, operationGas.get())) && delegate.isGasValid(tank, stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return locked.getAsBoolean() ? GasStack.EMPTY : delegate.drain(resource, action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return locked.getAsBoolean() ? GasStack.EMPTY : delegate.drain(maxDrain, action);
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return delegate.getGasInTank(tank);
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (locked.getAsBoolean() && !GasStack.isSameGasSameComponents(resource, operationGas.get())) {
            return 0;
        }
        return delegate.fill(resource, action);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        if (!locked.getAsBoolean()) {
            return delegate.tryFillAtomically(resources, action);
        }

        GasStack lockedGas = operationGas.get();
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty() || GasStack.isSameGasSameComponents(resource, lockedGas)) {
                continue;
            }

            return AtomicFillResult.REJECTED;
        }
        return delegate.tryFillAtomically(resources, action);
    }

    @Override
    public long getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }
}
