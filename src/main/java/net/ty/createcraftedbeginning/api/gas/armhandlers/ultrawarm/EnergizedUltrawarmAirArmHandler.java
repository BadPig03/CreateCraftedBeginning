package net.ty.createcraftedbeginning.api.gas.armhandlers.ultrawarm;

public class EnergizedUltrawarmAirArmHandler extends UltrawarmAirArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.8f;
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
