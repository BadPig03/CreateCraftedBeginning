package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility methods for querying and registering Airtight Arm Handlers.
 * <p>
 * An {@link AirtightArmHandler} defines the gas consumption multiplier and
 * interaction bonuses provided by a specific {@link Gas}. This class acts as
 * the common entry point for resolving handlers from gases or gas stacks, and
 * for registering custom airtight arm behavior values.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmHandlerUtils {
    private AirtightArmHandlerUtils() {
    }

    /**
     * Gets the Airtight Arm Handler for the gas type contained in the given
     * {@link GasStack}.
     * <p>
     * This method delegates to {@link #of(Gas)} using
     * {@link GasStack#getGasType()}.
     *
     * @param gasStack the gas stack whose gas type should be used
     * @return the registered Airtight Arm Handler for the gas type, or a
     * {@link DefaultArmHandler} if no custom handler is registered
     * @throws IllegalArgumentException if the gas stack contains an empty gas type
     */
    public static AirtightArmHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Gets the Airtight Arm Handler registered for the given gas type.
     * <p>
     * If the gas type has no custom handler registered in
     * {@link AirtightArmHandler#REGISTRY}, this method returns a new
     * {@link DefaultArmHandler}.
     *
     * @param gasType the gas type to resolve the arm handler for
     * @return the registered Airtight Arm Handler, or a default handler if
     * no custom handler exists
     * @throws IllegalArgumentException if the given gas type is empty
     */
    public static AirtightArmHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightArmHandler armHandler = AirtightArmHandler.REGISTRY.get(gasType);
        if (armHandler == null) {
            return new DefaultArmHandler();
        }
        return armHandler;
    }

    /**
     * Registers an Airtight Arm Handler for the gas identified by the given resource location.
     * <p>
     * The provided values are wrapped in an {@link AirtightArmHandler} that always
     * returns the given gas consumption multiplier, block interaction range bonus,
     * entity interaction range bonus, and knockback bonus.
     * <p>
     * This method delegates to {@link #register(ResourceLocation, AirtightArmHandler)},
     * which performs validation for gas existence and duplicate handlers.
     *
     * @param location    the resource location of the gas to register an arm handler for
     * @param consumption the gas consumption multiplier to use
     * @param blockRange  the increased block interaction range to use
     * @param entityRange the increased entity interaction range to use
     * @param knockback   the increased knockback value to use
     * @see AirtightArmHandler
     * @see #register(ResourceLocation, AirtightArmHandler)
     */
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

    /**
     * Registers a custom Airtight Arm Handler for the gas identified by the given resource location.
     * <p>
     * The gas is resolved using {@link Gas#getGasTypeByName(ResourceLocation)}. If the
     * given {@link ResourceLocation} does not point to an existing gas, registration is
     * skipped and an error is logged.
     * <p>
     * This method does not override existing handlers. If the target gas already has
     * an {@link AirtightArmHandler} registered in {@link AirtightArmHandler#REGISTRY},
     * registration is skipped and an error is logged.
     *
     * @param location the resource location of the gas to register an arm handler for
     * @param handler  the Airtight Arm Handler to associate with the gas
     * @see AirtightArmHandler
     * @see AirtightArmHandler#REGISTRY
     * @see Gas#getGasTypeByName(ResourceLocation)
     */
    public static void register(ResourceLocation location, AirtightArmHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Arm Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightArmHandler armHandler = AirtightArmHandler.REGISTRY.get(gasType);
        if (armHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Arm Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        AirtightArmHandler.REGISTRY.register(gasType, handler);
    }
}
