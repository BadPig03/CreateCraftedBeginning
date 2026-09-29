package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureHandlerRegistry;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmHandlers {
    private static final AirtightArmHandler DEFAULT_HANDLER = DefaultArmHandler.INSTANCE;
    private static final GameplayPressureHandlerRegistry<AirtightArmHandler> HANDLERS = GameplayPressureHandlerRegistry.create();

    private AirtightArmHandlers() {
    }

    /** Equipment uses the normal gas baseline; pressure-specific handlers remain available to other systems. */
    public static AirtightArmHandler resolveForEquipment(Gas gasType) {
        return resolve(gasType, GameplayPressureProfiles.NORMAL);
    }

    public static AirtightArmHandler resolve(GasStack gasStack, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightArmHandler resolve(GasStack gasStack, GameplayPressureProfile profile) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), profile);
    }

    public static AirtightArmHandler resolve(Gas gasType, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightArmHandler resolve(Gas gasType, GameplayPressureProfile profile) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException("Airtight arm handler resolution requires a non-empty gas.");
        }

        AirtightArmHandler armHandler = HANDLERS.get(gasType, profile);
        if (armHandler == null) {
            return DEFAULT_HANDLER;
        }

        return armHandler;
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, float consumption, float blockRange, float entityRange, float knockback) {
        register(location, profile, new AirtightArmStats(consumption, blockRange, entityRange, knockback));
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, AirtightArmHandler handler) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register airtight arm handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsExact(gasType, profile)) {
            CCBAPI.LOGGER.error("Failed to register airtight arm handler for gas '{}' and gameplay pressure profile '{}': a handler is already registered.", location, profile.id());
            return;
        }

        float consumptionMultiplier = handler.getGasConsumptionMultiplier();
        if (!GasConsumptionMath.isNonNegativeFinite(consumptionMultiplier)) {
            CCBAPI.LOGGER.error("Failed to register airtight arm handler for gas '{}': consumption multiplier must be finite and non-negative, got {}.", location, consumptionMultiplier);
            return;
        }

        float blockRange = handler.getIncreasedBlockInteractionRange();
        if (!GasConsumptionMath.isNonNegativeFinite(blockRange)) {
            CCBAPI.LOGGER.error("Failed to register airtight arm handler for gas '{}': block interaction range bonus must be finite and non-negative, got {}.", location, blockRange);
            return;
        }

        float entityRange = handler.getIncreasedEntityInteractionRange();
        if (!GasConsumptionMath.isNonNegativeFinite(entityRange)) {
            CCBAPI.LOGGER.error("Failed to register airtight arm handler for gas '{}': entity interaction range bonus must be finite and non-negative, got {}.", location, entityRange);
            return;
        }

        float knockback = handler.getIncreasedKnockback();
        if (!GasConsumptionMath.isNonNegativeFinite(knockback)) {
            CCBAPI.LOGGER.error("Failed to register airtight arm handler for gas '{}': attack knockback bonus must be finite and non-negative, got {}.", location, knockback);
            return;
        }

        AirtightArmStats stats = handler instanceof AirtightArmStats existing ? existing : new AirtightArmStats(consumptionMultiplier, blockRange, entityRange, knockback);
        HANDLERS.register(gasType, profile, stats);
    }

}
