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
 * allows scripts to associate a gas with a maximum Tesla Turbine operating level.
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
     * Registers the maximum Tesla Turbine level for the gas identified by the
     * given resource location.
     *
     * @param location the resource location of the gas to register
     * @param maxLevel the maximum Tesla Turbine operating level unlocked by the gas
     * @see AirtightTurbineHandlerUtils#register(ResourceLocation, int)
     */
    public void add(ResourceLocation location, int maxLevel) {
        AirtightTurbineHandlerUtils.register(location, maxLevel);
    }
}
