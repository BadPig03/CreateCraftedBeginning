package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Arm Handlers for gases.
 * <p>
 * This event is exposed through {@code CCBEvents.airtightArmHandler} and
 * allows scripts to associate a gas with Airtight Extended Arm behaviour values.
 * <p>
 * Example usage in KubeJS:
 *
 * <pre>{@code
 * CCBEvents.airtightArmHandler(event => {
 *     event.add('kubejs:oxygen', 0.8, 2.4, 2.4, 0.6)
 * })
 * }</pre>
 * <p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightArmHandlerEvent implements KubeEvent {
    /**
     * Registers Airtight Arm behaviour values for the gas identified by the given
     * resource location.
     * <p>
     * The provided {@link ResourceLocation} is passed to
     * {@link AirtightArmHandlerUtils#register(ResourceLocation, float, float, float, float)},
     * which performs validation for gas existence and duplicate handlers.
     * <p>
     * In KubeJS, the location can usually be provided as a string, for example
     * {@code "createcraftedbeginning:natural_air"} or {@code "kubejs:oxygen"}.
     *
     * @param location    the resource location of the gas to register
     * @param consumption the gas consumption multiplier to assign
     * @param blockRange  the increased block interaction range to assign
     * @param entityRange the increased entity interaction range to assign
     * @param knockback   the increased knockback value to assign
     * @see AirtightArmHandlerUtils#register(ResourceLocation, float, float, float, float)
     */
    public void add(ResourceLocation location, float consumption, float blockRange, float entityRange, float knockback) {
        AirtightArmHandlerUtils.register(location, consumption, blockRange, entityRange, knockback);
    }
}
