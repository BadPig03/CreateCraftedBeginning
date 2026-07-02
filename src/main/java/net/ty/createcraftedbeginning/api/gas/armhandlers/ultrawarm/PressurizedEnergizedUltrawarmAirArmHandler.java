package net.ty.createcraftedbeginning.api.gas.armhandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedUltrawarmAirArmHandler extends UltrawarmAirArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.3f;
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
