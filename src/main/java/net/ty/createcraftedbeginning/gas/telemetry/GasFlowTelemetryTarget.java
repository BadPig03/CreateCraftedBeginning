package net.ty.createcraftedbeginning.gas.telemetry;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasFlowTelemetryTarget {
    void acceptFlowTelemetry(long flowRate);
}
