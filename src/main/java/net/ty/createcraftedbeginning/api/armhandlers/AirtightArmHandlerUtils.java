package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmHandlerUtils {
    private static final AirtightArmHandler DEFAULT_HANDLER = DefaultArmHandler.INSTANCE;

    private AirtightArmHandlerUtils() {
    }

    public static AirtightArmHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    public static AirtightArmHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightArmHandler armHandler = AirtightArmHandler.REGISTRY.get(gasType);
        if (armHandler == null) {
            return DEFAULT_HANDLER;
        }
        return armHandler;
    }

    public static void register(ResourceLocation location, float consumption, float blockRange, float entityRange, float knockback) {
        register(location, new AirtightArmHandler() {
            @Override
            public float getGasConsumptionMultiplier() {
                return consumption;
            }

            @Override
            public float getIncreasedBlockInteractionRange() {
                return blockRange;
            }

            @Override
            public float getIncreasedEntityInteractionRange() {
                return entityRange;
            }

            @Override
            public float getIncreasedKnockback() {
                return knockback;
            }
        });
    }

    public static void register(ResourceLocation location, AirtightArmHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register Airtight Arm Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightArmHandler armHandler = AirtightArmHandler.REGISTRY.get(gasType);
        if (armHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Arm Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        float consumptionMultiplier = handler.getGasConsumptionMultiplier();
        if (!GasConsumptions.isNonNegativeFinite(consumptionMultiplier)) {
            CCBAPI.LOGGER.error("Failed to register Airtight Arm Handler for gas '{}': consumption multiplier must be finite and non-negative, got {}.", location, consumptionMultiplier);
            return;
        }

        float blockRange = handler.getIncreasedBlockInteractionRange();
        if (!GasConsumptions.isNonNegativeFinite(blockRange)) {
            CCBAPI.LOGGER.error("Failed to register Airtight Arm Handler for gas '{}': block interaction range bonus must be finite and non-negative, got {}.", location, blockRange);
            return;
        }

        float entityRange = handler.getIncreasedEntityInteractionRange();
        if (!GasConsumptions.isNonNegativeFinite(entityRange)) {
            CCBAPI.LOGGER.error("Failed to register Airtight Arm Handler for gas '{}': entity interaction range bonus must be finite and non-negative, got {}.", location, entityRange);
            return;
        }

        float knockback = handler.getIncreasedKnockback();
        if (!GasConsumptions.isNonNegativeFinite(knockback)) {
            CCBAPI.LOGGER.error("Failed to register Airtight Arm Handler for gas '{}': attack knockback bonus must be finite and non-negative, got {}.", location, knockback);
            return;
        }

        AirtightArmStats stats = handler instanceof AirtightArmStats existing ? existing : new AirtightArmStats(consumptionMultiplier, blockRange, entityRange, knockback);
        AirtightArmHandler.REGISTRY.register(gasType, stats);
    }
}
