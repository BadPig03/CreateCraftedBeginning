package net.ty.createcraftedbeginning.api.gas.drillhandlers.moist;

import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;

public class MoistAirDrillHandler implements AirtightHandheldDrillHandler {
    @Override
    public int getDamageAddition() {
        return 0;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 1;
    }
}
