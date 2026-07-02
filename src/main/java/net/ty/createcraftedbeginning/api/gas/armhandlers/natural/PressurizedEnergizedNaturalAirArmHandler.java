package net.ty.createcraftedbeginning.api.gas.armhandlers.natural;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
