package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlers;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureHandlerRegistry;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonHandlers {
    private static final GameplayPressureHandlerRegistry<AirtightCannonHandler> HANDLERS = GameplayPressureHandlerRegistry.create();

    private AirtightCannonHandlers() {
    }

    /** Equipment uses the normal gas baseline; pressure-specific handlers remain available to other systems. */
    public static AirtightCannonHandler resolveForEquipment(Gas gasType) {
        return resolve(gasType, GameplayPressureProfiles.NORMAL);
    }

    public static AirtightCannonHandler resolve(GasStack gasStack, long sourcePressurePa) {
        return resolve(gasStack.getGasType(), GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightCannonHandler resolve(GasStack gasStack, GameplayPressureProfile profile) {
        return resolve(gasStack.getGasType(), profile);
    }

    public static AirtightCannonHandler resolve(Gas gasType, long sourcePressurePa) {
        return resolve(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightCannonHandler resolve(Gas gasType, GameplayPressureProfile profile) {
        if (gasType.isEmpty()) {
            return DefaultCannonHandler.INSTANCE;
        }

        AirtightCannonHandler cannonHandler = HANDLERS.get(gasType, profile);
        if (cannonHandler == null) {
            return DefaultCannonHandler.INSTANCE;
        }

        return cannonHandler;
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, AirtightCannonHandler handler) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register airtight cannon handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsExact(gasType, profile)) {
            CCBAPI.LOGGER.error("Failed to register airtight cannon handler for gas '{}' and gameplay pressure profile '{}': a handler is already registered.", location, profile.id());
            return;
        }

        double consumptionMultiplier = handler.getGasConsumptionMultiplier();
        if (!GasConsumptionMath.isNonNegativeFinite(consumptionMultiplier)) {
            CCBAPI.LOGGER.error("Failed to register airtight cannon handler for gas '{}': consumption multiplier must be finite and non-negative, got {}.", location, consumptionMultiplier);
            return;
        }

        if (handler instanceof AirtightCannonVisualHandler visualHandler) {
            float rotationSpeed = visualHandler.getRotationSpeed();
            if (!GasConsumptionMath.isFinite(rotationSpeed)) {
                CCBAPI.LOGGER.error("Failed to register airtight cannon handler for gas '{}': rotation speed must be finite, got {}.", location, rotationSpeed);
                return;
            }
        }

        HANDLERS.register(gasType, profile, handler);
        if (!(handler instanceof AirtightCannonVisualHandler visualHandler)) {
            return;
        }

        AirtightCannonVisualHandlers.register(location, profile, visualHandler);
    }

    /** Changes only the consumption of an exact gameplay registration, retaining its separate visual registration. */
    public static void modify(ResourceLocation location, GameplayPressureProfile profile, AirtightCannonHandlerPatch patch) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException("Gas '" + location + "' does not exist.");
        }

        AirtightCannonHandler original = HANDLERS.findExact(gasType, profile);
        if (original == null) {
            throw new IllegalStateException("No airtight cannon handler is registered for gas '" + location + "' and gameplay pressure profile '" + profile.id() + "'.");
        }

        AirtightCannonHandler modified = patch.applyTo(original);
        HANDLERS.replaceExact(gasType, profile, modified);
    }
}
