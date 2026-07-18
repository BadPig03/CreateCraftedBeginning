package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlerUtils;

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
