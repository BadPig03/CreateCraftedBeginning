package net.ty.createcraftedbeginning.api.gas.drillhandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedUltrawarmAirDrillHandler extends UltrawarmAirDrillHandler {
    @Override
    public float getConsumptionMultiplier() {
        return 0.51f;
    }
}
