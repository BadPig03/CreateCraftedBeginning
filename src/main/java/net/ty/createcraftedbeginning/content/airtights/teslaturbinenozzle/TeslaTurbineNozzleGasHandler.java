package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class TeslaTurbineNozzleGasHandler implements GasPressureBoundary {
    private final TeslaTurbineNozzleConnection connection;
    private final GasPressureBoundary delegate;

    TeslaTurbineNozzleGasHandler(TeslaTurbineNozzleConnection connection, GasPressureBoundary delegate) {
        this.connection = connection;
        this.delegate = delegate;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return connection.isCurrent(this) && delegate.isGasValid(tank, stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return GasStack.EMPTY;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return GasStack.EMPTY;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return GasStack.EMPTY;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (!connection.isCurrent(this)) {
            return 0;
        }

        return delegate.fill(resource, action);
    }

    @Override
    public long getFillPressurePa(int tank, GasStack gas) {
        if (!connection.isCurrent(this)) {
            return GasPressure.VACUUM_PA;
        }

        return delegate.getFillPressurePa(tank, gas);
    }

    @Override
    public double getFillFlowResistanceFactor(int tank, GasStack gas) {
        if (!connection.isCurrent(this)) {
            return 0;
        }

        return delegate.getFillFlowResistanceFactor(tank, gas);
    }

    @Override
    public long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
        if (!connection.isCurrent(this)) {
            return 0;
        }

        return delegate.fillFromPressure(resource, sourcePressurePa, action);
    }
}
