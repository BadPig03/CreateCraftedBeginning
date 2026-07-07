package net.ty.createcraftedbeginning.api.drillhandlers.natural;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedNaturalAirDrillHandler extends NaturalAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 1;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.5f;
    }
}
