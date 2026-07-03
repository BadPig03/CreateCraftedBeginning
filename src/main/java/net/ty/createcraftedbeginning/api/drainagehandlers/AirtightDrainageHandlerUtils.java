package net.ty.createcraftedbeginning.api.drainagehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrainageHandlerEvent.DrainageHandler;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility methods for querying and registering Airtight Drainage Handlers.
 * <p>
 * An {@link AirtightDrainageHandler} defines the effect applied when a specific
 * {@link Gas} is drained from an airtight pipe. This class acts as the common
 * entry point for resolving handlers from gases or gas stacks, and for
 * registering custom drainage behavior.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightDrainageHandlerUtils {
    private AirtightDrainageHandlerUtils() {
    }

    /**
     * Gets the Airtight Drainage Handler for the gas type contained in the given
     * {@link GasStack}.
     * <p>
     * This method delegates to {@link #of(Gas)} using
     * {@link GasStack#getGasType()}.
     *
     * @param gasStack the gas stack whose gas type should be used
     * @return the registered Airtight Drainage Handler for the gas type, or a
     * {@link DefaultDrainageHandler} if no custom handler is registered
     * @throws IllegalArgumentException if the gas stack contains an empty gas type
     */
    public static AirtightDrainageHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Gets the Airtight Drainage Handler registered for the given gas type.
     * <p>
     * If the gas type has no custom handler registered in
     * {@link AirtightDrainageHandler#REGISTRY}, this method returns a new
     * {@link DefaultDrainageHandler}.
     *
     * @param gasType the gas type to resolve the drainage handler for
     * @return the registered Airtight Drainage Handler, or a default handler if no
     * custom handler exists
     * @throws IllegalArgumentException if the given gas type is empty
     */
    public static AirtightDrainageHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightDrainageHandler drainageHandler = AirtightDrainageHandler.REGISTRY.get(gasType);
        if (drainageHandler == null) {
            return new DefaultDrainageHandler();
        }
        return drainageHandler;
    }

    /**
     * Registers an Airtight Drainage Handler for the gas identified by the given
     * resource location.
     * <p>
     * The supplied {@link DrainageHandler} is wrapped as an
     * {@link AirtightDrainageHandler}. The wrapped handler returns the provided
     * inflation value from {@link AirtightDrainageHandler#getInflation()} and
     * delegates its drainage behavior to the given KubeJS handler.
     * <p>
     * If {@code showOutline} is {@code true}, the wrapped handler shows the default
     * drainage outline before running the custom drainage behavior.
     *
     * @param location    the resource location of the gas to register a drainage
     *                    handler for
     * @param inflation   the non-negative drainage area inflation radius
     * @param showOutline whether the default drainage outline should be shown before
     *                    running the custom handler
     * @param handler     the custom drainage behavior to run
     * @see AirtightDrainageHandler
     */
    public static void register(ResourceLocation location, float inflation, boolean showOutline, DrainageHandler handler) {
        register(location, inflation, new AirtightDrainageHandler() {
            @Override
            public float getInflation() {
                return inflation;
            }

            @Override
            public void apply(Level level, BlockPos pos, Direction direction, Gas gasType) {
                if (showOutline) {
                    showOutline(level, pos, direction, inflation, gasType.getTint());
                }
                handler.apply(level, pos, direction, gasType);
            }
        });
    }

    /**
     * Registers an Airtight Drainage Handler for the gas identified by the given
     * resource location.
     * <p>
     * The gas is resolved from {@link CCBGasRegistries#GAS_REGISTRY}. If the given
     * {@link ResourceLocation} does not point to an existing gas, registration is
     * skipped and an error is logged.
     * <p>
     * This method does not override existing handlers. If the target gas already has
     * an {@link AirtightDrainageHandler} registered in
     * {@link AirtightDrainageHandler#REGISTRY}, registration is skipped and an error
     * is logged.
     * <p>
     * The inflation value must be non-negative. Negative values are rejected and
     * will not be registered.
     *
     * @param location  the resource location of the gas to register a drainage
     *                  handler for
     * @param inflation the non-negative drainage area inflation radius
     * @param handler   the Airtight Drainage Handler to register
     * @see AirtightDrainageHandler
     * @see Gas#EMPTY_GAS_HOLDER
     */
    public static void register(ResourceLocation location, float inflation, AirtightDrainageHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drainage Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightDrainageHandler drainageHandler = AirtightDrainageHandler.REGISTRY.get(gasType);
        if (drainageHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drainage Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (inflation < 0) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drainage Handler for gas '{}': inflation must be non-negative.", location);
            return;
        }

        AirtightDrainageHandler.REGISTRY.register(gasType, handler);
    }
}
