package net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal;

public class PressurizedEnergizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.1f, 0.1f, 0.1f, 0.1f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
