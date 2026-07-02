package net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedUltrawarmAirArmorsHandler extends UltrawarmAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.72f, 0.72f, 0.72f, 0.72f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 2;
    }
}
