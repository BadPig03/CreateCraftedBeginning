package net.ty.createcraftedbeginning.api.gas.drillhandlers.spore;

import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;

public class SporeAirDrillHandler implements AirtightHandheldDrillHandler {
    @Override
    public int getDamageAddition() {
        return 0;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.95f;
    }
}
