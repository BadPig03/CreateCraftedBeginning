package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Engine Handlers for gases.
 * <p>
 * This event is exposed through {@code CCBEvents.airtightEngineHandler} and
 * allows scripts to associate a gas with an airtight engine efficiency value.
 * <p>
 * Example usage in KubeJS:
 *
 * <pre>{@code
 * CCBEvents.airtightEngineHandler(event => {
 *     event.add('kubejs:oxygen', 4)
 * })
 * }</pre>
 * <p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineHandlerEvent implements KubeEvent {
    /**
     * Registers an Airtight Engine efficiency value for the given gas type.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal Java-side
     * usage. The gas is converted to its {@link ResourceLocation}, then delegated to
     * {@link AirtightEngineHandlerUtils#register(ResourceLocation, int)}.
     *
     * @param gasType    the gas type to register
     * @param efficiency the airtight engine efficiency value to assign
     * @see Gas#getResourceLocation()
     * @see AirtightEngineHandlerUtils#register(ResourceLocation, int)
     */
    @HideFromJS
    public static void add(Gas gasType, int efficiency) {
        AirtightEngineHandlerUtils.register(gasType.getResourceLocation(), efficiency);
    }

    /**
     * Registers an Airtight Engine efficiency value for the gas identified by
     * the given resource location.
     * <p>
     * The provided {@link ResourceLocation} is passed to
     * {@link AirtightEngineHandlerUtils#register(ResourceLocation, int)}, which
     * performs validation for gas existence, duplicate handlers, and efficiency
     * range.
     * <p>
     * In KubeJS, the location can usually be provided as a string, for example
     * {@code "createcraftedbeginning:natural_air"} or {@code "kubejs:oxygen"}.
     *
     * @param location   the resource location of the gas to register
     * @param efficiency the airtight engine efficiency value to assign
     * @see AirtightEngineHandlerUtils#register(ResourceLocation, int)
     */
    public void add(ResourceLocation location, int efficiency) {
        AirtightEngineHandlerUtils.register(location, efficiency);
    }
}
