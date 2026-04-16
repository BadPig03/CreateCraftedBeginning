package net.ty.createcraftedbeginning.api.gas.armorhandlers.natural;

public class EnergizedNaturalAirArmorsHandler extends NaturalAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.8f, 0.8f, 0.8f, 0.8f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 2;
    }
}
