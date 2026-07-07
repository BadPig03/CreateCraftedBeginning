package net.ty.createcraftedbeginning.api.drillhandlers.natural;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedNaturalAirDrillHandler extends NaturalAirDrillHandler {
    @Override
    public float getConsumptionMultiplier() {
        return 0.65f;
    }
}
