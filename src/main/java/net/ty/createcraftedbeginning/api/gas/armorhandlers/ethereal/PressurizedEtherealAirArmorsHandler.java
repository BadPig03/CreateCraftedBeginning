package net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal;

public class PressurizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.37f, 0.37f, 0.37f, 0.37f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
