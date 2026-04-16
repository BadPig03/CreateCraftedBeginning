package net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm;

public class EnergizedUltrawarmAirArmorsHandler extends UltrawarmAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.72f, 0.72f, 0.72f, 0.72f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 2;
    }
}
