package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IVentingGasSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SteamOutletGasHandler implements IGasHandler, IVentingGasSource {
    private final BoilerSteamOutletBlockEntity be;
    private final IGasHandler delegate;

    public SteamOutletGasHandler(BoilerSteamOutletBlockEntity be, IGasHandler delegate) {
        this.be = be;
        this.delegate = delegate;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        be.ensureCurrentTick();
        return delegate.isGasValid(tank, stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        be.ensureCurrentTick();
        GasStack drained = delegate.drain(resource, action);
        be.recordExtraction(drained, action);
        return drained;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        be.ensureCurrentTick();
        GasStack drained = delegate.drain(maxDrain, action);
        be.recordExtraction(drained, action);
        return drained;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        be.ensureCurrentTick();
        return delegate.getGasInTank(tank);
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return delegate.fill(resource, action);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        return delegate.tryFillAtomically(resources, action);
    }

    @Override
    public long getTankCapacity(int tank) {
        be.ensureCurrentTick();
        return delegate.getTankCapacity(tank);
    }
}
