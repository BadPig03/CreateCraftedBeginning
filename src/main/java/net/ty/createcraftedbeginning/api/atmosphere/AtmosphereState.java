package net.ty.createcraftedbeginning.api.atmosphere;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AtmosphereState(Gas gas, long pressurePa) {
    public AtmosphereState {
        pressurePa = Math.max(GasPressure.VACUUM_PA, pressurePa);
    }
}
