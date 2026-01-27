package net.ty.createcraftedbeginning.api.gas.armhandlers.ethereal;

public class PressurizedEtherealAirArmHandler extends EtherealAirArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.37f;
    }

    @Override
    public float getIncreasedBlockInteractionRange() {
        return 4;
    }

    @Override
    public float getIncreasedEntityInteractionRange() {
        return 4;
    }

    @Override
    public float getIncreasedKnockback() {
        return 1;
    }
}
