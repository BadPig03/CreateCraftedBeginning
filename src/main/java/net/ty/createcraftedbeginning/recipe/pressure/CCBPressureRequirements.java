package net.ty.createcraftedbeginning.recipe.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBPressureRequirements {
    public static final long PRESSURIZED_MINIMUM_PRESSURE_PA = GasPressure.pascals(10);
    public static final PressureRequirement PRESSURIZED = PressureRequirement.atLeast(PRESSURIZED_MINIMUM_PRESSURE_PA);

    private CCBPressureRequirements() {
    }
}
