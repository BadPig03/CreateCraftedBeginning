package net.ty.createcraftedbeginning.api.gas.drillhandlers.ethereal;

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
