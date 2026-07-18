package net.ty.createcraftedbeginning.api.armhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightArmHandler {
    SimpleRegistry<Gas, AirtightArmHandler> REGISTRY = SimpleRegistry.create();

    /**
     * Returns the gas consumption multiplier.
     *
     * @return the gas consumption multiplier
     */
    float getGasConsumptionMultiplier();

    /**
     * Returns the increased block interaction range.
     *
     * @return the increased block interaction range
     */
    float getIncreasedBlockInteractionRange();

    /**
     * Returns the increased entity interaction range.
     *
     * @return the increased entity interaction range
     */
    float getIncreasedEntityInteractionRange();

    /**
     * Returns the increased knockback.
     *
     * @return the increased knockback
     */
    float getIncreasedKnockback();
}
