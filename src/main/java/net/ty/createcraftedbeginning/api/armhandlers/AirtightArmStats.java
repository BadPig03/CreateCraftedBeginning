package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightArmStats(float gasConsumptionMultiplier, float increasedBlockInteractionRange, float increasedEntityInteractionRange, float increasedKnockback) implements AirtightArmHandler {
    public AirtightArmStats {
        requireNonNegativeFinite("gas consumption multiplier", gasConsumptionMultiplier);
        requireNonNegativeFinite("block interaction range bonus", increasedBlockInteractionRange);
        requireNonNegativeFinite("entity interaction range bonus", increasedEntityInteractionRange);
        requireNonNegativeFinite("attack knockback bonus", increasedKnockback);
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return gasConsumptionMultiplier;
    }

    @Override
    public float getIncreasedBlockInteractionRange() {
        return increasedBlockInteractionRange;
    }

    @Override
    public float getIncreasedEntityInteractionRange() {
        return increasedEntityInteractionRange;
    }

    @Override
    public float getIncreasedKnockback() {
        return increasedKnockback;
    }

    private static void requireNonNegativeFinite(String name, float value) {
        if (GasConsumptionMath.isNonNegativeFinite(value)) {
            return;
        }

        throw new IllegalArgumentException("Airtight arm " + name + " must be finite and non-negative; got " + value + '.');
    }
}
