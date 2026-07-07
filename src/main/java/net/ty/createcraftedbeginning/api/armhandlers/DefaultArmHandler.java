package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultArmHandler implements AirtightArmHandler {
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
