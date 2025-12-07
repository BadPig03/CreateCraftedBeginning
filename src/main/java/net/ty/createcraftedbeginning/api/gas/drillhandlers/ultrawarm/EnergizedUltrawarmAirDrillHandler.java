package net.ty.createcraftedbeginning.api.gas.drillhandlers.ultrawarm;

public class EnergizedUltrawarmAirDrillHandler extends UltrawarmAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 1;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.72f;
    }
}
