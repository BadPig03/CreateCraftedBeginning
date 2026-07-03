package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Drainage Handlers for gases.
 * <p>
 * This event is exposed through {@code CCBEvents.airtightDrainageHandler} and
 * allows scripts to associate a gas with a custom drainage behavior.
 * <p>
 * Example usage in KubeJS:
 *
 * <pre>{@code
 * CCBEvents.airtightDrainageHandler(event => {
 *     event.add('kubejs:oxygen', 1.0, true, (level, pos, direction, gasType) => {
 *         // Custom drainage behavior
 *     })
 * })
 * }</pre>
 * <p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightDrainageHandlerEvent implements KubeEvent {
    /**
     * Registers an Airtight Drainage Handler for the given gas type.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal Java-side
     * usage. The gas is converted to its {@link ResourceLocation}, then delegated to
     * {@link AirtightDrainageHandlerUtils#register(ResourceLocation, float, boolean, DrainageHandler)}.
     *
     * @param gasType     the gas type to register
     * @param inflation   the inflation value to assign to the drainage handler
     * @param showOutline whether the drainage outline should be shown
     * @param handler     the drainage handler to execute
     * @see Gas#getResourceLocation()
     * @see AirtightDrainageHandlerUtils#register(ResourceLocation, float, boolean, DrainageHandler)
     */
    @HideFromJS
    public static void add(Gas gasType, float inflation, boolean showOutline, DrainageHandler handler) {
        AirtightDrainageHandlerUtils.register(gasType.getResourceLocation(), inflation, showOutline, handler);
    }

    /**
     * Registers an Airtight Drainage Handler for the given gas type.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal Java-side
     * usage. The gas is converted to its {@link ResourceLocation}, while the inflation
     * value is read from the provided {@link AirtightDrainageHandler}.
     *
     * @param gasType the gas type to register
     * @param handler the airtight drainage handler to register
     * @see Gas#getResourceLocation()
     * @see AirtightDrainageHandler#getInflation()
     * @see AirtightDrainageHandlerUtils#register(ResourceLocation, float, AirtightDrainageHandler)
     */
    @HideFromJS
    public static void add(Gas gasType, AirtightDrainageHandler handler) {
        AirtightDrainageHandlerUtils.register(gasType.getResourceLocation(), handler.getInflation(), handler);
    }

    /**
     * Registers an Airtight Drainage Handler for the gas identified by the given
     * resource location.
     * <p>
     * The provided {@link ResourceLocation} is passed to
     * {@link AirtightDrainageHandlerUtils#register(ResourceLocation, float, boolean, DrainageHandler)},
     * which registers the custom drainage behavior for the target gas.
     * <p>
     * In KubeJS, the location can usually be provided as a string, for example
     * {@code "createcraftedbeginning:natural_air"} or {@code "kubejs:oxygen"}.
     *
     * @param location      the resource location of the gas to register
     * @param inflation     the inflation value to assign to the drainage handler
     * @param shouldOutline whether the drainage outline should be shown
     * @param handler       the drainage handler to execute
     * @see AirtightDrainageHandlerUtils#register(ResourceLocation, float, boolean, DrainageHandler)
     */
    public void add(ResourceLocation location, float inflation, boolean shouldOutline, DrainageHandler handler) {
        AirtightDrainageHandlerUtils.register(location, inflation, shouldOutline, handler);
    }

    /**
     * Functional interface used by KubeJS scripts to define custom drainage logic.
     * <p>
     * The handler is called with the level, target block position, drainage direction,
     * and gas type involved in the drainage operation.
     */
    @FunctionalInterface
    public interface DrainageHandler {
        /**
         * Performs custom drainage behavior.
         *
         * @param level     the level where the drainage operation occurs
         * @param pos       the target block position of the drainage operation
         * @param direction the direction of the drainage operation
         * @param gasType   the gas type being drained
         */
        void apply(Level level, BlockPos pos, Direction direction, Gas gasType);
    }
}
