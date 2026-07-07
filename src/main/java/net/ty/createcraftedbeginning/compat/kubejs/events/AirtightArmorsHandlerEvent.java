package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Armors Handlers for gases.
 * <p>
 * This event allows scripts to associate a gas with Airtight Armors behavior,
 * including effect curing logic, armor gas consumption multipliers, and the
 * elytra boosting multiplier.
 * <p>
 * Example usage in KubeJS:
 * <pre>{@code
 * CCBEvents.airtightArmorsHandler((event) => {
 *     event.add('kubejs:oxygen', (effectInstance) => {
 *         return false
 *     }, 1.0, 1.0, 1.0, 1.0, 0.75)
 * })
 * }</pre>
 * <p>
 * The four armor consumption multipliers are ordered as helmet, chestplate,
 * leggings, and boots.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightArmorsHandlerEvent implements KubeEvent {
    /**
     * Registers a custom Airtight Armors Handler for the given gas type.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal
     * Java-side usage. The gas is converted to its {@link ResourceLocation}, then
     * delegated to {@link AirtightArmorsHandlerUtils#register(ResourceLocation, AirtightArmorsHandler)}.
     *
     * @param gasType the gas type to register
     * @param handler the Airtight Armors Handler to associate with the gas
     * @see Gas#getResourceLocation()
     * @see AirtightArmorsHandlerUtils#register(ResourceLocation, AirtightArmorsHandler)
     */
    @HideFromJS
    public static void add(Gas gasType, AirtightArmorsHandler handler) {
        AirtightArmorsHandlerUtils.register(gasType.getResourceLocation(), handler);
    }

    /**
     * Registers Airtight Armors behavior values for the gas identified by the given
     * resource location.
     * <p>
     * The provided {@link ArmorsHandler} determines whether a specific
     * {@link MobEffectInstance} can be cured by the armor handler.
     * <p>
     * The consumption multipliers are stored in the following order:
     * helmet, chestplate, leggings, and boots. The elytra multiplier controls the
     * gas usage or strength multiplier used while boosting with an elytra.
     *
     * @param location   the resource location of the gas to register
     * @param handler    the effect curing handler to execute
     * @param helmet     the gas consumption multiplier for the helmet
     * @param chestplate the gas consumption multiplier for the chestplate
     * @param leggings   the gas consumption multiplier for the leggings
     * @param boots      the gas consumption multiplier for the boots
     * @param elytra     the multiplier used for boosting with an elytra
     * @see AirtightArmorsHandlerUtils#register(ResourceLocation, ArmorsHandler, float, float, float, float, float)
     */
    public void add(ResourceLocation location, ArmorsHandler handler, float helmet, float chestplate, float leggings, float boots, float elytra) {
        AirtightArmorsHandlerUtils.register(location, handler, helmet, chestplate, leggings, boots, elytra);
    }

    /**
     * Functional interface used by KubeJS scripts to define effect curing behavior.
     * <p>
     * The handler receives a {@link MobEffectInstance} and returns whether the
     * airtight armor handler is allowed to cure that effect.
     */
    @FunctionalInterface
    public interface ArmorsHandler {
        /**
         * Returns whether the given effect instance can be cured.
         *
         * @param effectInstance the effect instance to check
         * @return {@code true} if the effect can be cured, otherwise {@code false}
         */
        boolean apply(MobEffectInstance effectInstance);
    }
}
