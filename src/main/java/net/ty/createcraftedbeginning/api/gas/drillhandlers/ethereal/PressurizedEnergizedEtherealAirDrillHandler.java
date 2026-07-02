package net.ty.createcraftedbeginning.api.gas.drillhandlers.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedEtherealAirDrillHandler extends EtherealAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 2;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.1f;
    }
}
