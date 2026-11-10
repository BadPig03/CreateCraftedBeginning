package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/** Replaces supplied values and retains the other values from the registered arm stats. */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightArmHandlerPatch(@Nullable Float consumptionMultiplier, @Nullable Float blockRangeBonus, @Nullable Float entityRangeBonus, @Nullable Float knockbackBonus) {
    public AirtightArmHandlerPatch {
        if (consumptionMultiplier == null && blockRangeBonus == null && entityRangeBonus == null && knockbackBonus == null) {
            throw new IllegalArgumentException("An airtight arm handler patch must change at least one field.");
        }

        requireMultiplier("consumptionMultiplier", consumptionMultiplier);
        requireMultiplier("blockRangeBonus", blockRangeBonus);
        requireMultiplier("entityRangeBonus", entityRangeBonus);
        requireMultiplier("knockbackBonus", knockbackBonus);
    }

    private static void requireMultiplier(String field, @Nullable Float value) {
        if (value == null || GasConsumptionMath.isNonNegativeFinite(value)) {
            return;
        }

        throw new IllegalArgumentException("Airtight arm field '" + field + "' must be finite and non-negative, got " + value + '.');
    }

    AirtightArmStats applyTo(AirtightArmHandler original) {
        float consumption = original.getGasConsumptionMultiplier();
        float blockRange = original.getIncreasedBlockInteractionRange();
        float entityRange = original.getIncreasedEntityInteractionRange();
        float knockback = original.getIncreasedKnockback();
        if (consumptionMultiplier != null) {
            consumption = consumptionMultiplier;
        }
        if (blockRangeBonus != null) {
            blockRange = blockRangeBonus;
        }
        if (entityRangeBonus != null) {
            entityRange = entityRangeBonus;
        }
        if (knockbackBonus != null) {
            knockback = knockbackBonus;
        }

        return new AirtightArmStats(consumption, blockRange, entityRange, knockback);
    }
}
