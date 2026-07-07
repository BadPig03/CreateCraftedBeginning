package net.ty.createcraftedbeginning.api.drillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility methods for querying and registering Airtight Drill Handlers.
 * <p>
 * An {@link AirtightDrillHandler} defines drill-related behavior for a specific
 * {@link Gas}, including the additional damage value and gas consumption
 * multiplier used by the Airtight Handheld Drill.
 * <p>
 * This class acts as the common entry point for resolving handlers from gases or
 * gas stacks, and for registering custom airtight drill behavior values.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightDrillHandlerUtils {
    private AirtightDrillHandlerUtils() {
    }

    /**
     * Gets the Airtight Drill Handler for the gas type contained in the given
     * {@link GasStack}.
     * <p>
     * This method delegates to {@link #of(Gas)} using
     * {@link GasStack#getGasType()}.
     *
     * @param gasStack the gas stack whose gas type should be used
     * @return the registered Airtight Drill Handler for the gas type, or a
     * {@link DefaultDrillHandler} if no custom handler is registered
     * @throws IllegalArgumentException if the gas stack contains an empty gas type
     */
    public static AirtightDrillHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Gets the Airtight Drill Handler registered for the given gas type.
     * <p>
     * If the gas type has no custom handler registered in
     * {@link AirtightDrillHandler#REGISTRY}, this method returns a new
     * {@link DefaultDrillHandler}.
     *
     * @param gasType the gas type to resolve the drill handler for
     * @return the registered Airtight Drill Handler, or a default handler if no
     * custom handler exists
     * @throws IllegalArgumentException if the given gas type is empty
     */
    public static AirtightDrillHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightDrillHandler drillHandler = AirtightDrillHandler.REGISTRY.get(gasType);
        if (drillHandler == null) {
            return new DefaultDrillHandler();
        }
        return drillHandler;
    }

    /**
     * Registers Airtight Drill behavior values for the gas identified by the given
     * resource location.
     * <p>
     * The provided values are wrapped in an {@link AirtightDrillHandler} that
     * always returns the given additional damage value and gas consumption
     * multiplier.
     * <p>
     * This method delegates to
     * {@link #register(ResourceLocation, AirtightDrillHandler)}, which performs
     * validation for gas existence and duplicate handlers.
     *
     * @param location    the resource location of the gas to register a drill handler for
     * @param damage      the additional damage value to use
     * @param consumption the gas consumption multiplier to use
     * @see AirtightDrillHandler
     * @see #register(ResourceLocation, AirtightDrillHandler)
     */
    public static void register(ResourceLocation location, int damage, float consumption) {
        register(location, new AirtightDrillHandler() {
            @Override
            public int getDamageAddition() {
                return damage;
            }

            @Override
            public float getConsumptionMultiplier() {
                return consumption;
            }
        });
    }

    /**
     * Registers a custom Airtight Drill Handler for the gas identified by the given
     * resource location.
     * <p>
     * The gas is resolved using {@link Gas#getGasTypeByName(ResourceLocation)}. If
     * the given {@link ResourceLocation} does not point to an existing gas,
     * registration is skipped and an error is logged.
     * <p>
     * This method does not override existing handlers. If the target gas already
     * has an {@link AirtightDrillHandler} registered in
     * {@link AirtightDrillHandler#REGISTRY}, registration is skipped and an error
     * is logged.
     *
     * @param location the resource location of the gas to register a drill handler for
     * @param handler  the Airtight Drill Handler to associate with the gas
     * @see AirtightDrillHandler
     * @see AirtightDrillHandler#REGISTRY
     * @see Gas#getGasTypeByName(ResourceLocation)
     */
    public static void register(ResourceLocation location, AirtightDrillHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drill Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightDrillHandler drillHandler = AirtightDrillHandler.REGISTRY.get(gasType);
        if (drillHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drill Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        AirtightDrillHandler.REGISTRY.register(gasType, handler);
    }
}
