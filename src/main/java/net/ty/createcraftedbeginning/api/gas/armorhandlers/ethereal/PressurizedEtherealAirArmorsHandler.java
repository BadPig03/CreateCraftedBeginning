package net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.37f, 0.37f, 0.37f, 0.37f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
