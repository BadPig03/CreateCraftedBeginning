package net.ty.createcraftedbeginning.api.armorhandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedUltrawarmAirArmorsHandler extends UltrawarmAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.51f, 0.51f, 0.51f, 0.51f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 1.5f;
    }
}
