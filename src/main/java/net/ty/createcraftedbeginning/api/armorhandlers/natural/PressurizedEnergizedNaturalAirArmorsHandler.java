package net.ty.createcraftedbeginning.api.armorhandlers.natural;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedNaturalAirArmorsHandler extends NaturalAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.5f, 0.5f, 0.5f, 0.5f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
