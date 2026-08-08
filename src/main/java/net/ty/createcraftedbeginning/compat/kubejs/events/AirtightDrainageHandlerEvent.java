package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.compat.kubejs.CCBKubeJSHandlerUtils;

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
     * Registers an Airtight Drainage Handler for the gas identified by the given
     * resource location.
     * <p>
     * The provided {@link ResourceLocation} is passed to
     * the KubeJS compatibility adapter, which registers the custom drainage behaviour for the target gas.
     * <p>
     * In KubeJS, the location can usually be provided as a string, for example
     * {@code "createcraftedbeginning:natural_air"} or {@code "kubejs:oxygen"}.
     *
     * @param location      the resource location of the gas to register
     * @param inflation     the inflation value to assign to the drainage handler
     * @param shouldOutline whether the drainage outline should be shown
     * @param handler       the drainage handler to execute
     * @see CCBKubeJSHandlerUtils#registerDrainage(ResourceLocation, float, boolean, DrainageHandler)
     */
    public void add(ResourceLocation location, float inflation, boolean shouldOutline, DrainageHandler handler) {
        CCBKubeJSHandlerUtils.registerDrainage(location, inflation, shouldOutline, handler);
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
