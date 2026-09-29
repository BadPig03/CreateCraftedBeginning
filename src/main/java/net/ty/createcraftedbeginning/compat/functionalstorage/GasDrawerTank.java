package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerTransfer.VoidTarget;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerTank extends GasTank implements VoidTarget {
    private final GasDrawerBlockEntity owner;

    public GasDrawerTank(GasTankLimits limits, GasDrawerBlockEntity owner, Predicate<GasStack> validator) {
        super(limits, validator);
        this.owner = owner;
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (!owner.isCreative()) {
            return super.drain(resource, action);
        }

        GasStack storedGas = getStoredStack();
        if (resource.isEmpty() || storedGas.isEmpty() || !GasStack.isSameGasSameComponents(resource, storedGas)) {
            return GasStack.EMPTY;
        }

        return resource.copy();
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (!owner.isCreative()) {
            return super.drain(maxDrain, action);
        }

        GasStack storedGas = getStoredStack();
        if (maxDrain <= 0 || storedGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        return storedGas.copyWithAmount(maxDrain);
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

        GasStack storedGas = getStoredStack();
        boolean sameStoredIdentity = !storedGas.isEmpty() && GasStack.isSameGasSameComponents(storedGas, resource);
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
    public GasStack getGasStack() {
        return getVisibleStack();
    }

    @Override
    public long getPressurePa() {
        if (!owner.isCreative() || getStoredStack().isEmpty()) {
            return super.getPressurePa();
        }

        return getMaxPressurePa();
    }

    @Override
    public long getStoredAmount() {
        if (owner.isCreative() && !getStoredStack().isEmpty()) {
            return Long.MAX_VALUE;
        }

        return super.getStoredAmount();
    }

    @Override
    public PressureModel getPressureModel() {
        if (owner.isCreative()) {
            return PressureModel.FIXED;
        }

        return PressureModel.VARIABLE;
    }

    @Override
    public boolean canVoid(GasStack resource) {
        if (resource.isEmpty() || owner.isCreative() || !owner.isVoid() || !isGasValid(resource)) {
            return false;
        }

        GasStack storedGas = getStoredStack();
        if (!storedGas.isEmpty()) {
            return GasStack.isSameGasSameComponents(storedGas, resource);
        }

        return owner.isLocked();
    }

    @Override
    protected void onStateChanged() {
        owner.onGasChanged();
    }

    public GasStack getStoredStack() {
        return super.getGasStack();
    }

    private GasStack getVisibleStack() {
        GasStack storedGas = getStoredStack();
        if (storedGas.isEmpty() || !owner.isCreative()) {
            return storedGas;
        }

        return storedGas.copyWithAmount(Long.MAX_VALUE);
    }

    private long fillCreative(GasStack resource, GasAction action) {
        GasStack storedGas = getStoredStack();
        if (!storedGas.isEmpty() && !GasStack.isSameGasSameComponents(storedGas, resource)) {
            return 0;
        }

        if (storedGas.isEmpty() && action.execute()) {
            tryReplaceContents(resource.copyWithAmount(1)).requireAccepted();
        }
        return resource.getAmount();
    }
}
