package net.ty.createcraftedbeginning.api.gas.armorhandlers.natural;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedNaturalAirArmorsHandler extends NaturalAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.8f, 0.8f, 0.8f, 0.8f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 2;
    }
}
