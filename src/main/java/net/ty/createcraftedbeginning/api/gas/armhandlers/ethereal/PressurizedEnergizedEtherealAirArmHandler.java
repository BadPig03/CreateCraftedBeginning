package net.ty.createcraftedbeginning.api.gas.armhandlers.ethereal;

public class PressurizedEnergizedEtherealAirArmHandler extends EtherealAirArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.1f;
    }

    @Override
    public float getIncreasedBlockInteractionRange() {
        return 8;
    }

    @Override
    public float getIncreasedEntityInteractionRange() {
        return 8;
    }

    @Override
    public float getIncreasedKnockback() {
        return 2;
    }
}
