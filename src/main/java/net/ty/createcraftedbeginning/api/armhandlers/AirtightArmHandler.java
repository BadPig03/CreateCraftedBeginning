package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightArmHandler {
    float getGasConsumptionMultiplier();

    float getIncreasedBlockInteractionRange();

    float getIncreasedEntityInteractionRange();

    float getIncreasedKnockback();
}
