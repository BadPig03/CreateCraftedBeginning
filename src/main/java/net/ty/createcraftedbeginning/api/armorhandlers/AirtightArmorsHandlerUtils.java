package net.ty.createcraftedbeginning.api.armorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmorsHandlerEvent.ArmorsHandler;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility methods for querying and registering Airtight Armors Handlers.
 * <p>
 * An {@link AirtightArmorsHandler} defines armor-related behavior for a specific
 * {@link Gas}, including whether effects can be cured, the gas consumption
 * multipliers for each armor slot, and the elytra boosting multiplier.
 * <p>
 * This class acts as the common entry point for resolving handlers from gases or
 * gas stacks, and for registering custom airtight armor behavior values.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmorsHandlerUtils {
    private AirtightArmorsHandlerUtils() {
    }

    /**
     * Gets the Airtight Armors Handler for the gas type contained in the given
     * {@link GasStack}.
     * <p>
     * This method delegates to {@link #of(Gas)} using
     * {@link GasStack#getGasType()}.
     *
     * @param gasStack the gas stack whose gas type should be used
     * @return the registered Airtight Armors Handler for the gas type, or a
     * {@link DefaultArmorsHandler} if no custom handler is registered
     * @throws IllegalArgumentException if the gas stack contains an empty gas type
     */
    public static AirtightArmorsHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Gets the Airtight Armors Handler registered for the given gas type.
     * <p>
     * If the gas type has no custom handler registered in
     * {@link AirtightArmorsHandler#REGISTRY}, this method returns a new
     * {@link DefaultArmorsHandler}.
     *
     * @param gasType the gas type to resolve the armors handler for
     * @return the registered Airtight Armors Handler, or a default handler if no
     * custom handler exists
     * @throws IllegalArgumentException if the given gas type is empty
     */
    public static AirtightArmorsHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return new DefaultArmorsHandler();
        }
        return armorsHandler;
    }

    /**
     * Registers Airtight Armors behavior values for the gas identified by the given
     * resource location.
     * <p>
     * The provided {@link ArmorsHandler} is used to determine whether an effect can
     * be cured. The four armor consumption multipliers are returned in the order
     * helmet, chestplate, leggings, and boots. The elytra value is returned by
     * {@link AirtightArmorsHandler#getMultiplierForBoostingElytra()}.
     * <p>
     * This method wraps the provided values in an {@link AirtightArmorsHandler} and
     * delegates to {@link #register(ResourceLocation, AirtightArmorsHandler)}, which
     * performs validation for gas existence and duplicate handlers.
     *
     * @param location   the resource location of the gas to register an armors handler for
     * @param handler    the effect curing handler to execute
     * @param helmet     the gas consumption multiplier for the helmet
     * @param chestplate the gas consumption multiplier for the chestplate
     * @param leggings   the gas consumption multiplier for the leggings
     * @param boots      the gas consumption multiplier for the boots
     * @param elytra     the multiplier used for boosting with an elytra
     * @see AirtightArmorsHandler
     * @see #register(ResourceLocation, AirtightArmorsHandler)
     */
    public static void register(ResourceLocation location, ArmorsHandler handler, float helmet, float chestplate, float leggings, float boots, float elytra) {
        register(location, new AirtightArmorsHandler() {
            @Override
            public boolean canCureEffect(MobEffectInstance effectInstance) {
                return handler.apply(effectInstance);
            }

            @Override
            public float[] getConsumptionMultiplier() {
                return new float[]{helmet, chestplate, leggings, boots};
            }

            @Override
            public float getMultiplierForBoostingElytra() {
                return elytra;
            }
        });
    }

    /**
     * Registers a custom Airtight Armors Handler for the gas identified by the
     * given resource location.
     * <p>
     * The gas is resolved using {@link Gas#getGasTypeByName(ResourceLocation)}. If
     * the given {@link ResourceLocation} does not point to an existing gas,
     * registration is skipped and an error is logged.
     * <p>
     * This method does not override existing handlers. If the target gas already
     * has an {@link AirtightArmorsHandler} registered in
     * {@link AirtightArmorsHandler#REGISTRY}, registration is skipped and an error
     * is logged.
     *
     * @param location the resource location of the gas to register an armors handler for
     * @param handler  the Airtight Armors Handler to associate with the gas
     * @see AirtightArmorsHandler
     * @see AirtightArmorsHandler#REGISTRY
     * @see Gas#getGasTypeByName(ResourceLocation)
     */
    public static void register(ResourceLocation location, AirtightArmorsHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Armors Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Armors Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        AirtightArmorsHandler.REGISTRY.register(gasType, handler);
    }
}
