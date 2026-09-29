package net.ty.createcraftedbeginning.api.armorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureHandlerRegistry;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmorsHandlers {
    private static final GameplayPressureHandlerRegistry<AirtightArmorsHandler> HANDLERS = GameplayPressureHandlerRegistry.create();

    private AirtightArmorsHandlers() {
    }

    /** Equipment uses the normal gas baseline; pressure-specific handlers remain available to other systems. */
    public static AirtightArmorsHandler resolveForEquipment(Gas gasType) {
        return resolve(gasType, GameplayPressureProfiles.NORMAL);
    }

    public static AirtightArmorsHandler resolve(GasStack gasStack, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightArmorsHandler resolve(GasStack gasStack, GameplayPressureProfile profile) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), profile);
    }

    public static AirtightArmorsHandler resolve(Gas gasType, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightArmorsHandler resolve(Gas gasType, GameplayPressureProfile profile) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException("Airtight armor handler resolution requires a non-empty gas.");
        }

        AirtightArmorsHandler armorsHandler = HANDLERS.get(gasType, profile);
        if (armorsHandler == null) {
            return DefaultArmorsHandler.INSTANCE;
        }

        return armorsHandler;
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, AirtightArmorsHandler handler) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register airtight armor handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsExact(gasType, profile)) {
            CCBAPI.LOGGER.error("Failed to register airtight armor handler for gas '{}' and gameplay pressure profile '{}': a handler is already registered.", location, profile.id());
            return;
        }

        if (!validateHandler(location, handler)) {
            return;
        }

        HANDLERS.register(gasType, profile, handler);
    }

    private static boolean validateHandler(ResourceLocation location, AirtightArmorsHandler handler) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            float multiplier = handler.getConsumptionMultiplier(slot);
            if (GasConsumptionMath.isNonNegativeFinite(multiplier)) {
                continue;
            }

            CCBAPI.LOGGER.error("Failed to register airtight armor handler for gas '{}': consumption multiplier for slot '{}' must be finite and non-negative, got {}.", location, slot, multiplier);
            return false;
        }

        float elytraMultiplier = handler.getMultiplierForBoostingElytra();
        if (!GasConsumptionMath.isNonNegativeFinite(elytraMultiplier)) {
            CCBAPI.LOGGER.error("Failed to register airtight armor handler for gas '{}': elytra multiplier must be finite and non-negative, got {}.", location, elytraMultiplier);
            return false;
        }

        return true;
    }
}
