package net.ty.createcraftedbeginning.api.gas.armhandlers.natural;

public class PressurizedEnergizedNaturalAirArmHandler extends NaturalAirArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.5f;
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
