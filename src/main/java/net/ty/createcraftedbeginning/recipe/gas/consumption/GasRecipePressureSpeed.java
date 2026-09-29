package net.ty.createcraftedbeginning.recipe.gas.consumption;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRecipePressureSpeed {
    public static final float MAXIMUM_MULTIPLIER = 2;
    public static final long HALF_BONUS_PRESSURE_PA = GasPressure.pascals(5);

    private GasRecipePressureSpeed() {
    }

    public static float multiplier(GasConsumptionPlan plan) {
        return multiplierForHeadroom(plan.minimumPostConsumptionPressureHeadroomPa());
    }

    public static float multiplierForHeadroom(double pressureHeadroomPa) {
        if (Double.isNaN(pressureHeadroomPa) || pressureHeadroomPa <= GasPressure.VACUUM_PA) {
            return 1;
        }

        if (Double.isInfinite(pressureHeadroomPa)) {
            return MAXIMUM_MULTIPLIER;
        }

        double bonusFraction = pressureHeadroomPa / (pressureHeadroomPa + HALF_BONUS_PRESSURE_PA);
        return (float) (1.0 + (MAXIMUM_MULTIPLIER - 1.0) * bonusFraction);
    }
}
