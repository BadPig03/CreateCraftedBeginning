package net.ty.createcraftedbeginning.gas.telemetry;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasPressureTelemetryTarget {
    void acceptPressureTelemetry(long minPressurePa, long maxPressurePa);

    void clearPressureTelemetry();
}
