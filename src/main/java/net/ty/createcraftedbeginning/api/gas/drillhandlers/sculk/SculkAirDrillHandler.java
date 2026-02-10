package net.ty.createcraftedbeginning.api.gas.drillhandlers.sculk;

import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;

public class SculkAirDrillHandler implements AirtightHandheldDrillHandler {
    @Override
    public int getDamageAddition() {
        return 0;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 1;
    }
}
