package net.ty.createcraftedbeginning.api.gas.drillhandlers.creative;

import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;

public class CreativeDrillHandler implements AirtightHandheldDrillHandler {
    @Override
    public int getDamageAddition() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0;
    }
}
