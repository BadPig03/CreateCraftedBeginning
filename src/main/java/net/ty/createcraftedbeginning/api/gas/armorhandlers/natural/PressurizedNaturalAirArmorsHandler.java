package net.ty.createcraftedbeginning.api.gas.armorhandlers.natural;

public class PressurizedNaturalAirArmorsHandler extends NaturalAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.65f, 0.65f, 0.65f, 0.65f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 1.5f;
    }
}