package net.ty.createcraftedbeginning.api.gas.armhandlers.sculk;

import net.ty.createcraftedbeginning.api.gas.armhandlers.AirtightExtendArmHandler;

public class SculkAirArmHandler implements AirtightExtendArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 1;
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
