package net.ty.createcraftedbeginning.api.gas.drillhandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedUltrawarmAirDrillHandler extends UltrawarmAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 1;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.3f;
    }
}
