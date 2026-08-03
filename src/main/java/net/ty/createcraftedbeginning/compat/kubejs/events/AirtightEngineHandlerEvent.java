package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Engine Handlers for gases.
 * <p>
 * This event is exposed through {@code CCBEvents.airtightEngineHandler} and
 * allows scripts to associate a gas with an Airtight Engine work factor and
 * optional maximum operating level.
 * <p>
 * Example usage in KubeJS:
 *
 * <pre>{@code
 * CCBEvents.airtightEngineHandler(event => {
 *     event.add('kubejs:oxygen', 1.5, 8)
 * })
 * }</pre>
 * <p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineHandlerEvent implements KubeEvent {
    /**
     * Registers an Airtight Engine work factor for a gas with the normal
     * maximum operating level.
     *
     * @param location   the resource location of the gas to register
     * @param workFactor the effective supply contributed by each unit of gas
     * @see AirtightEngineHandlerUtils#register(ResourceLocation, double)
     */
    public void add(ResourceLocation location, double workFactor) {
        AirtightEngineHandlerUtils.register(location, workFactor);
    }

    /**
     * Registers an Airtight Engine work factor and maximum level for a gas.
     *
     * @param location   the resource location of the gas to register
     * @param workFactor the effective supply contributed by each unit of gas
     * @param maxLevel   the highest airtight engine level the gas can sustain
     * @see AirtightEngineHandlerUtils#register(ResourceLocation, double, int)
     */
    public void add(ResourceLocation location, double workFactor, int maxLevel) {
        AirtightEngineHandlerUtils.register(location, workFactor, maxLevel);
    }
}
