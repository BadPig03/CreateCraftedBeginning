package net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm;

public class PressurizedEnergizedUltrawarmAirArmorsHandler extends UltrawarmAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.3f, 0.3f, 0.3f, 0.3f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
