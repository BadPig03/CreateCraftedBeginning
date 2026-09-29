package net.ty.createcraftedbeginning.api.gas.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GameplayPressureProfile(ResourceLocation id, long minimumPressurePa) {
    public GameplayPressureProfile {
        if (minimumPressurePa < GasPressure.VACUUM_PA) {
            throw new IllegalArgumentException("Minimum gameplay pressure must be non-negative; got " + minimumPressurePa + " Pa.");
        }
    }
}
