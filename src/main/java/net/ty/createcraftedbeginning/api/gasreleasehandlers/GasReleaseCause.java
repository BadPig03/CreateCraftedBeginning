package net.ty.createcraftedbeginning.api.gasreleasehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasReleaseCause {
    ATMOSPHERIC_OUTLET,
    TANK_REMOVAL,
    MANUAL_VENT,
    OVERFLOW,
    RUPTURE
}
