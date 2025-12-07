package net.ty.createcraftedbeginning.api.gas.drillhandlers.natural;

public class PressurizedEnergizedNaturalAirDrillHandler extends NaturalAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 1;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.5f;
    }
}
