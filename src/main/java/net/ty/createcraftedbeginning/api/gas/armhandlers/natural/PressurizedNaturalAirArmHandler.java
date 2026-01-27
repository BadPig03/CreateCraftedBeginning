package net.ty.createcraftedbeginning.api.gas.armhandlers.natural;

public class PressurizedNaturalAirArmHandler extends NaturalAirArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.65f;
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
