package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.enginehandlers.DefaultEngineHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility methods for querying and registering Airtight Turbine Handlers.
 * <p>
 * An {@link AirtightTurbineHandler} defines the turbine efficiency provided by a
 * specific {@link Gas}. This class acts as the common entry point for resolving
 * handlers from gases or gas stacks, and for registering custom gas efficiency
 * values.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTurbineHandlerUtils {
    private AirtightTurbineHandlerUtils() {
    }

    /**
     * Gets the Airtight Turbine Handler for the gas type contained in the given
     * {@link GasStack}.
     * <p>
     * This method delegates to {@link #of(Gas)} using
     * {@link GasStack#getGasType()}.
     *
     * @param gasStack the gas stack whose gas type should be used
     * @return the registered Airtight Turbine Handler for the gas type, or a
     * {@link DefaultEngineHandler} if no custom handler is registered
     * @throws IllegalArgumentException if the gas stack contains an empty gas type
     */
    public static AirtightTurbineHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Gets the Airtight Turbine Handler registered for the given gas type.
     * <p>
     * If the gas type has no custom handler registered in
     * {@link AirtightTurbineHandler#REGISTRY}, this method returns a new
     * {@link DefaultTurbineHandler}.
     *
     * @param gasType the gas type to resolve the turbine handler for
     * @return the registered Airtight Turbine Handler, or a default handler if
     * no custom handler exists
     * @throws IllegalArgumentException if the given gas type is empty
     */
    public static AirtightTurbineHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightTurbineHandler turbineHandler = AirtightTurbineHandler.REGISTRY.get(gasType);
        if (turbineHandler == null) {
            return new DefaultTurbineHandler();
        }
        return turbineHandler;
    }

    /**
     * Registers an Airtight Turbine Handler for the gas identified by the given resource location.
     * <p>
     * The gas is resolved from {@link CCBGasRegistries#GAS_REGISTRY}. If the given
     * {@link ResourceLocation} does not point to an existing gas, registration is skipped
     * and an error is logged.
     * <p>
     * This method does not override existing handlers. If the target gas already has
     * an {@link AirtightTurbineHandler} registered in {@link AirtightTurbineHandler#REGISTRY},
     * registration is skipped and an error is logged.
     * <p>
     * The efficiency value must be in the inclusive range {@code [0, 16]}. Values
     * outside this range are rejected and will not be registered.
     * <p>
     * When registration succeeds, the gas is associated with an
     * {@link AirtightTurbineHandler} that always returns the provided efficiency value.
     *
     * @param location   the resource location of the gas to register a turbine handler for
     * @param efficiency the Airtight Turbine efficiency value, in the inclusive range {@code [0, 16]}
     * @see AirtightTurbineHandler
     * @see CCBGasRegistries#GAS_REGISTRY
     * @see Gas#EMPTY_GAS_HOLDER
     */
    public static void register(ResourceLocation location, int efficiency) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Turbine Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightTurbineHandler turbineHandler = AirtightTurbineHandler.REGISTRY.get(gasType);
        if (turbineHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Turbine Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (efficiency < 0 || efficiency > 16) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Turbine Handler for gas '{}': efficiency is out of range! Valid range is [0, 16].", location);
            return;
        }

        AirtightTurbineHandler.REGISTRY.register(gasType, () -> efficiency);
    }
}
