package net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal;

public class EnergizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.64f, 0.64f, 0.64f, 0.64f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 1.5f;
    }
}
