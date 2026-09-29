package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlers;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class TeslaTurbineGasHandler implements GasPressureBoundary {
    private static final double NOZZLE_FLOW_RESISTANCE = 28;

    private final TeslaTurbineBlockEntity turbine;
    private final TeslaTurbineFlowMeter flowMeter;
    private final boolean clockwise;

    TeslaTurbineGasHandler(TeslaTurbineBlockEntity turbine, TeslaTurbineFlowMeter flowMeter, boolean clockwise) {
        this.turbine = turbine;
        this.flowMeter = flowMeter;
        this.clockwise = clockwise;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return tank == 0 && !stack.isEmpty() && GameplayPressureProfiles.orderedProfiles().stream().anyMatch(profile -> AirtightTurbineHandlers.resolve(stack, profile).getMaxLevel() > 0);
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
        return fillFromPressure(resource, GasPressure.REFERENCE_PRESSURE_PA, action);
    }

    @Override
    public long getFillPressurePa(int tank, GasStack gas) {
        if (tank != 0) {
            return GasPressure.VACUUM_PA;
        }

        Level level = turbine.getLevel();
        if (level == null) {
            return GasPressure.REFERENCE_PRESSURE_PA;
        }

        return AtmosphereStateResolver.resolvePressurePa(level, turbine.getBlockPos());
    }

    @Override
    public double getFillFlowResistanceFactor(int tank, GasStack gas) {
        if (tank != 0) {
            return 0;
        }

        return NOZZLE_FLOW_RESISTANCE;
    }

    @Override
    public long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
        if (resource.isEmpty() || AirtightTurbineHandlers.resolve(resource, sourcePressurePa).getMaxLevel() <= 0) {
            return 0;
        }

        return flowMeter.fill(resource, sourcePressurePa, action, clockwise);
    }
}
