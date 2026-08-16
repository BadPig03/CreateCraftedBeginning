package net.ty.createcraftedbeginning.api.armorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmorsHandlerUtils {
    private AirtightArmorsHandlerUtils() {
    }

    public static AirtightArmorsHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    public static AirtightArmorsHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return DefaultArmorsHandler.INSTANCE;
        }
        return armorsHandler;
    }

    public static void register(ResourceLocation location, AirtightArmorsHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register Airtight Armors Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Armors Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (!validateHandler(location, handler)) {
            return;
        }

        AirtightArmorsHandler.REGISTRY.register(gasType, handler);
    }

    private static boolean validateHandler(ResourceLocation location, AirtightArmorsHandler handler) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            float multiplier = handler.getConsumptionMultiplier(slot);
            if (!GasConsumptions.isNonNegativeFinite(multiplier)) {
                CCBAPI.LOGGER.error("Failed to register Airtight Armors Handler for gas '{}': {} multiplier must be finite and non-negative, got {}.", location, slot, multiplier);
                return false;
            }
        }

        float elytraMultiplier = handler.getMultiplierForBoostingElytra();
        if (!GasConsumptions.isNonNegativeFinite(elytraMultiplier)) {
            CCBAPI.LOGGER.error("Failed to register Airtight Armors Handler for gas '{}': elytra multiplier must be finite and non-negative, got {}.", location, elytraMultiplier);
            return false;
        }

        return true;
    }
}
