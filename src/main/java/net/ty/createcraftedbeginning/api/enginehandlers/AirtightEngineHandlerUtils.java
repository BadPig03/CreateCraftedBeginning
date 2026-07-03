package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility methods for querying and registering Airtight Engine Handlers.
 * <p>
 * An {@link AirtightEngineHandler} defines the engine efficiency provided by a
 * specific {@link Gas}. This class acts as the common entry point for resolving
 * handlers from gases or gas stacks, and for registering custom gas efficiency
 * values.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightEngineHandlerUtils {
    private AirtightEngineHandlerUtils() {
    }

    /**
     * Gets the Airtight Engine Handler for the gas type contained in the given
     * {@link GasStack}.
     * <p>
     * This method delegates to {@link #of(Gas)} using
     * {@link GasStack#getGasType()}.
     *
     * @param gasStack the gas stack whose gas type should be used
     * @return the registered Airtight Engine Handler for the gas type, or a
     * {@link DefaultEngineHandler} if no custom handler is registered
     * @throws IllegalArgumentException if the gas stack contains an empty gas type
     */
    public static AirtightEngineHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Gets the Airtight Engine Handler registered for the given gas type.
     * <p>
     * If the gas type has no custom handler registered in
     * {@link AirtightEngineHandler#REGISTRY}, this method returns a new
     * {@link DefaultEngineHandler}.
     *
     * @param gasType the gas type to resolve the engine handler for
     * @return the registered Airtight Engine Handler, or a default handler if
     * no custom handler exists
     * @throws IllegalArgumentException if the given gas type is empty
     */
    public static AirtightEngineHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightEngineHandler engineHandler = AirtightEngineHandler.REGISTRY.get(gasType);
        if (engineHandler == null) {
            return new DefaultEngineHandler();
        }
        return engineHandler;
    }

    /**
     * Registers an Airtight Engine Handler for the gas identified by the given resource location.
     * <p>
     * The gas is resolved from {@link CCBGasRegistries#GAS_REGISTRY}. If the given
     * {@link ResourceLocation} does not point to an existing gas, registration is skipped
     * and an error is logged.
     * <p>
     * This method does not override existing handlers. If the target gas already has
     * an {@link AirtightEngineHandler} registered in {@link AirtightEngineHandler#REGISTRY},
     * registration is skipped and an error is logged.
     * <p>
     * The efficiency value must be in the inclusive range {@code [0, 16]}. Values
     * outside this range are rejected and will not be registered.
     * <p>
     * When registration succeeds, the gas is associated with an
     * {@link AirtightEngineHandler} that always returns the provided efficiency value.
     *
     * @param location   the resource location of the gas to register an engine handler for
     * @param efficiency the Airtight Engine efficiency value, in the inclusive range {@code [0, 16]}
     * @see AirtightEngineHandler
     * @see CCBGasRegistries#GAS_REGISTRY
     * @see Gas#EMPTY_GAS_HOLDER
     */
    public static void register(ResourceLocation location, int efficiency) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Engine Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightEngineHandler engineHandler = AirtightEngineHandler.REGISTRY.get(gasType);
        if (engineHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Engine Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (efficiency < 0 || efficiency > 16) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Engine Handler for gas '{}': efficiency is out of range! Valid range is [0, 16].", location);
            return;
        }

        AirtightEngineHandler.REGISTRY.register(gasType, () -> efficiency);
    }
}
