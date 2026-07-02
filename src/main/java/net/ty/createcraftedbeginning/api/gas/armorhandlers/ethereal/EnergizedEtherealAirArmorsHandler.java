package net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.64f, 0.64f, 0.64f, 0.64f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 1.5f;
    }
}
