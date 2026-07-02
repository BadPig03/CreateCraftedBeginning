package net.ty.createcraftedbeginning.api.gas.armhandlers.creative;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.armhandlers.AirtightExtendArmHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirArmHandler implements AirtightExtendArmHandler {
    @Override
    public float getGasConsumptionMultiplier() {
        return 0;
    }

    @Override
    public float getIncreasedBlockInteractionRange() {
        return 64;
    }

    @Override
    public float getIncreasedEntityInteractionRange() {
        return 64;
    }

    @Override
    public float getIncreasedKnockback() {
        return 5;
    }
}
