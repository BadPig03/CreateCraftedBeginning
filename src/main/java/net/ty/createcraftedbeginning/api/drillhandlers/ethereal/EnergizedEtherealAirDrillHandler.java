package net.ty.createcraftedbeginning.api.drillhandlers.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedEtherealAirDrillHandler extends EtherealAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 2;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.64f;
    }
}
