package net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm;

public class PressurizedUltrawarmAirArmorsHandler extends UltrawarmAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.51f, 0.51f, 0.51f, 0.51f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 1.5f;
    }
}
