package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility methods for querying and registering Airtight Cannon Handlers.
 * <p>
 * An {@link AirtightCannonHandler} defines cannon-related behavior for a
 * specific {@link Gas}, including render icon, trail particles, explosion logic,
 * projectile texture, model animation, tooltip text, and gas consumption.
 * <p>
 * This class acts as the common entry point for resolving handlers from gases or
 * gas stacks, and for registering custom airtight cannon behavior.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonHandlerUtils {
    private AirtightCannonHandlerUtils() {
    }

    /**
     * Gets the Airtight Cannon Handler for the gas type contained in the given
     * {@link GasStack}.
     * <p>
     * This method delegates to {@link #of(Gas)} using
     * {@link GasStack#getGasType()}.
     *
     * @param gasStack the gas stack whose gas type should be used
     * @return the registered Airtight Cannon Handler for the gas type, or a
     * {@link DefaultCannonHandler} if no custom handler is registered
     * @throws IllegalArgumentException if the gas stack contains an empty gas type
     */
    public static AirtightCannonHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Gets the Airtight Cannon Handler registered for the given gas type.
     * <p>
     * If the gas type has no custom handler registered in
     * {@link AirtightCannonHandler#REGISTRY}, this method returns a new
     * {@link DefaultCannonHandler}.
     *
     * @param gasType the gas type to resolve the cannon handler for
     * @return the registered Airtight Cannon Handler, or a default handler if no
     * custom handler exists
     * @throws IllegalArgumentException if the given gas type is empty
     */
    public static AirtightCannonHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasType);
        if (cannonHandler == null) {
            return new DefaultCannonHandler();
        }
        return cannonHandler;
    }

    /**
     * Registers a custom Airtight Cannon Handler for the gas identified by the
     * given resource location.
     * <p>
     * The gas is resolved using {@link Gas#getGasTypeByName(ResourceLocation)}. If
     * the given {@link ResourceLocation} does not point to an existing gas,
     * registration is skipped and an error is logged.
     * <p>
     * This method does not override existing handlers. If the target gas already
     * has an {@link AirtightCannonHandler} registered in
     * {@link AirtightCannonHandler#REGISTRY}, registration is skipped and an error
     * is logged.
     *
     * @param location the resource location of the gas to register a cannon handler for
     * @param handler  the Airtight Cannon Handler to associate with the gas
     * @see AirtightCannonHandler
     * @see AirtightCannonHandler#REGISTRY
     * @see Gas#getGasTypeByName(ResourceLocation)
     */
    public static void register(ResourceLocation location, AirtightCannonHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Cannon Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasType);
        if (cannonHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Cannon Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        AirtightCannonHandler.REGISTRY.register(gasType, handler);
    }
}
