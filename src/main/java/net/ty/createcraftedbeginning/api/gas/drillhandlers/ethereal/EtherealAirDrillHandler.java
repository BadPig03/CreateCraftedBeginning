package net.ty.createcraftedbeginning.api.gas.drillhandlers.ethereal;

import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;

public class EtherealAirDrillHandler implements AirtightHandheldDrillHandler {
    @Override
    public int getDamageAddition() {
        return 1;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.8f;
    }
}
