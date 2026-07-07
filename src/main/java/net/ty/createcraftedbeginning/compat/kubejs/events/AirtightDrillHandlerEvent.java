package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Drill Handlers for gases.
 * <p>
 * This event allows scripts to associate a gas with Airtight Handheld Drill behavior,
 * including the additional drill damage and gas consumption multiplier used by
 * the drill.
 * <p>
 * Example usage in KubeJS:
 * <pre>{@code
 * CCBEvents.airtightDrillHandler((event) => {
 *     event.add('kubejs:oxygen', 2, 1.0)
 * })
 * }</pre>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightDrillHandlerEvent implements KubeEvent {
    /**
     * Registers Airtight Drill behavior values for the given gas type.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal
     * Java-side usage. The gas is converted to its {@link ResourceLocation}, then
     * delegated to
     * {@link AirtightDrillHandlerUtils#register(ResourceLocation, int, float)}.
     *
     * @param gasType     the gas type to register
     * @param damage      the additional damage value to assign
     * @param consumption the gas consumption multiplier to assign
     * @see Gas#getResourceLocation()
     * @see AirtightDrillHandlerUtils#register(ResourceLocation, int, float)
     */
    @HideFromJS
    public static void add(Gas gasType, int damage, float consumption) {
        AirtightDrillHandlerUtils.register(gasType.getResourceLocation(), damage, consumption);
    }

    /**
     * Registers a custom Airtight Drill Handler for the given gas type.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal
     * Java-side usage. The gas is converted to its {@link ResourceLocation}, then
     * delegated to
     * {@link AirtightDrillHandlerUtils#register(ResourceLocation, AirtightDrillHandler)}.
     *
     * @param gasType the gas type to register
     * @param handler the Airtight Drill Handler to associate with the gas
     * @see Gas#getResourceLocation()
     * @see AirtightDrillHandlerUtils#register(ResourceLocation, AirtightDrillHandler)
     */
    @HideFromJS
    public static void add(Gas gasType, AirtightDrillHandler handler) {
        AirtightDrillHandlerUtils.register(gasType.getResourceLocation(), handler);
    }

    /**
     * Registers Airtight Drill behavior values for the gas identified by the given
     * resource location.
     * <p>
     * The provided damage value is returned by
     * {@link AirtightDrillHandler#getDamageAddition()}, and the provided
     * consumption value is returned by
     * {@link AirtightDrillHandler#getConsumptionMultiplier()}.
     * <p>
     * In KubeJS, the location can usually be provided as a string, for example
     * {@code "createcraftedbeginning:natural_air"} or {@code "kubejs:oxygen"}.
     *
     * @param location    the resource location of the gas to register
     * @param damage      the additional damage value to assign
     * @param consumption the gas consumption multiplier to assign
     * @see AirtightDrillHandlerUtils#register(ResourceLocation, int, float)
     */
    public void add(ResourceLocation location, int damage, float consumption) {
        AirtightDrillHandlerUtils.register(location, damage, consumption);
    }
}
