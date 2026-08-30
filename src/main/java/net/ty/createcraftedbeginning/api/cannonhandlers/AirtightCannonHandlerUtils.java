package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonHandlerUtils {
    private AirtightCannonHandlerUtils() {
    }

    public static AirtightCannonHandler of(GasStack gasStack) {
        return of(gasStack.getGasType());
    }

    public static AirtightCannonHandler of(Gas gasType) {
        if (gasType.isEmpty()) {
            return DefaultCannonHandler.INSTANCE;
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasType);
        if (cannonHandler == null) {
            return DefaultCannonHandler.INSTANCE;
        }
        return cannonHandler;
    }

    public static void register(ResourceLocation location, AirtightCannonHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register Airtight Cannon Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasType);
        if (cannonHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Cannon Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        float consumptionMultiplier = handler.getGasConsumptionMultiplier();
        if (!GasConsumptions.isNonNegativeFinite(consumptionMultiplier)) {
            CCBAPI.LOGGER.error("Failed to register Airtight Cannon Handler for gas '{}': consumption multiplier must be finite and non-negative, got {}.", location, consumptionMultiplier);
            return;
        }

        if (handler instanceof AirtightCannonVisualHandler visualHandler && !GasConsumptions.isFinite(visualHandler.getRotationSpeed())) {
            CCBAPI.LOGGER.error("Failed to register Airtight Cannon Handler for gas '{}': rotation speed must be finite, got {}.", location, visualHandler.getRotationSpeed());
            return;
        }

        AirtightCannonHandler.REGISTRY.register(gasType, handler);
        if (!(handler instanceof AirtightCannonVisualHandler visualHandler)) {
            return;
        }

        AirtightCannonVisualHandlerUtils.register(location, visualHandler);
    }
}
