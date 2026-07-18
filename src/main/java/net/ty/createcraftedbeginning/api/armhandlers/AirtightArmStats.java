package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptionUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightArmStats(float gasConsumptionMultiplier, float increasedBlockInteractionRange, float increasedEntityInteractionRange, float increasedKnockback) implements AirtightArmHandler {
    /**
     * Creates a new {@code AirtightArmStats} instance.
     *
     * @param gasConsumptionMultiplier        the gas consumption multiplier value to use
     * @param increasedBlockInteractionRange  the increased block interaction range value to use
     * @param increasedEntityInteractionRange the increased entity interaction range value to use
     * @param increasedKnockback              the increased knockback value to use
     */
    public AirtightArmStats {
        requireNonNegativeFinite("gas consumption multiplier", gasConsumptionMultiplier);
        requireNonNegativeFinite("block interaction range bonus", increasedBlockInteractionRange);
        requireNonNegativeFinite("entity interaction range bonus", increasedEntityInteractionRange);
        requireNonNegativeFinite("attack knockback bonus", increasedKnockback);
    }

    private static void requireNonNegativeFinite(String name, float value) {
        if (GasConsumptionUtils.isNonNegativeFinite(value)) {
            return;
        }

        throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getGasConsumptionMultiplier() {
        return gasConsumptionMultiplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getIncreasedBlockInteractionRange() {
        return increasedBlockInteractionRange;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getIncreasedEntityInteractionRange() {
        return increasedEntityInteractionRange;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getIncreasedKnockback() {
        return increasedKnockback;
    }
}
