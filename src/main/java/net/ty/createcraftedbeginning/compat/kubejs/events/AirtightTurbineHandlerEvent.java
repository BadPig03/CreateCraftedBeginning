package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Turbine Handlers for gases.
 * <p>
 * This event is exposed through {@code CCBEvents.airtightTurbineHandler} and
 * allows scripts to associate a gas with a Tesla Turbine efficiency value.
 * <p>
 * Example usage in KubeJS:
 *
 * <pre>{@code
 * CCBEvents.airtightTurbineHandler(event => {
 *     event.add('kubejs:oxygen', 4)
 * })
 * }</pre>
 * <p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTurbineHandlerEvent implements KubeEvent {
    /**
     * Registers an Airtight Turbine efficiency value for the gas identified by
     * the given resource location.
     * <p>
     * The provided {@link ResourceLocation} is passed to
     * {@link AirtightTurbineHandlerUtils#register(ResourceLocation, int)}, which
     * performs validation for gas existence, duplicate handlers, and efficiency
     * range.
     * <p>
     * In KubeJS, the location can usually be provided as a string, for example
     * {@code "createcraftedbeginning:natural_air"} or {@code "kubejs:oxygen"}.
     *
     * @param location   the resource location of the gas to register
     * @param efficiency the airtight turbine efficiency value to assign
     * @see AirtightTurbineHandlerUtils#register(ResourceLocation, int)
     */
    public void add(ResourceLocation location, int efficiency) {
        AirtightTurbineHandlerUtils.register(location, efficiency);
    }
}
