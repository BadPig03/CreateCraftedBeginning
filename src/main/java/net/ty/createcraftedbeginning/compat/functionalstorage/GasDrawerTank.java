package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerTank extends GasTank {
    private final GasDrawerBlockEntity owner;

    public GasDrawerTank(long capacity, GasDrawerBlockEntity owner, Predicate<GasStack> validator) {
        super(capacity, validator);
        this.owner = owner;
    }

    public GasStack getStoredStack() {
        return gas;
    }

    private GasStack getVisibleStack() {
        if (gas.isEmpty() || !owner.isCreative()) {
            return gas;
        }
        return gas.copyWithAmount(Long.MAX_VALUE);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (!owner.isCreative()) {
            return super.drain(resource, action);
        }
        if (resource.isEmpty() || gas.isEmpty() || !GasStack.isSameGasSameComponents(resource, gas)) {
            return GasStack.EMPTY;
        }
        return resource.copy();
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (!owner.isCreative()) {
            return super.drain(maxDrain, action);
        }
        if (maxDrain <= 0 || gas.isEmpty()) {
            return GasStack.EMPTY;
        }
        return gas.copyWithAmount(maxDrain);
    }

    @Override
    public GasStack getGasInTank(int ignoredTank) {
        return getVisibleStack();
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (resource.isEmpty() || !isGasValid(resource)) {
            return 0;
        }
        if (owner.isCreative()) {
            return fillCreative(resource, action);
        }

        boolean sameStoredIdentity = !gas.isEmpty() && GasStack.isSameGasSameComponents(gas, resource);
        boolean lockedIdentity = owner.isLocked() && isGasValid(resource);
        long acceptedAmount = super.fill(resource, action);
        if (!owner.isVoid()) {
            return acceptedAmount;
        }
        if (!sameStoredIdentity && !lockedIdentity && acceptedAmount <= 0) {
            return acceptedAmount;
        }
        return resource.getAmount();
    }

    @Override
    public long getTankCapacity(int ignoredTank) {
        if (owner.isCreative()) {
            return Long.MAX_VALUE;
        }
        return capacity;
    }

    @Override
    public GasStack getGasStack() {
        return getVisibleStack();
    }

    @Override
    public long getCapacity() {
        if (owner.isCreative()) {
            return Long.MAX_VALUE;
        }
        return capacity;
    }

    @Override
    public long getGasAmount() {
        if (!gas.isEmpty() && owner.isCreative()) {
            return Long.MAX_VALUE;
        }
        return gas.getAmount();
    }

    @Override
    protected void onContentsChanged() {
        owner.onGasChanged();
    }

    private long fillCreative(GasStack resource, GasAction action) {
        if (!gas.isEmpty() && !GasStack.isSameGasSameComponents(gas, resource)) {
            return 0;
        }

        if (gas.isEmpty() && action.execute()) {
            gas = resource.copyWithAmount(1);
            onContentsChanged();
        }
        return resource.getAmount();
    }
}
