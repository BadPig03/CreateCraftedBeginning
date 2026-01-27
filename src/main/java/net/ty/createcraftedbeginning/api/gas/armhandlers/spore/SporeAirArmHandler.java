package net.ty.createcraftedbeginning.api.gas.armhandlers.spore;

import net.ty.createcraftedbeginning.api.gas.armhandlers.AirtightExtendArmHandler;

public class SporeAirArmHandler implements AirtightExtendArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.95f;
    }

    @Override
    public float getIncreasedBlockInteractionRange() {
        return 2;
    }

    @Override
    public float getIncreasedEntityInteractionRange() {
        return 2;
    }

    @Override
    public float getIncreasedKnockback() {
        return 0.5f;
    }
}
