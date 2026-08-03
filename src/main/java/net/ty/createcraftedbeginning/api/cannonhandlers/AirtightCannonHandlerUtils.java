package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptionUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for gas-specific airtight cannon behavior.
 * Handlers define explosion behavior, gas consumption, and contextual tooltip content.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonHandlerUtils {
    private AirtightCannonHandlerUtils() {
    }

    /**
     * Resolves the airtight cannon handler associated with the supplied input.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resolved airtight cannon handler
     */
    public static AirtightCannonHandler of(GasStack gasStack) {
        return of(gasStack.getGasType());
    }

    /**
     * Resolves the airtight cannon handler associated with the supplied input.
     *
     * @param gasType the gas type to inspect or process
     * @return the resolved airtight cannon handler
     */
    public static AirtightCannonHandler of(Gas gasType) {
        if (gasType.isEmpty()) {
            return DefaultCannonHandler.INSTANCE;
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasType);
        return cannonHandler != null ? cannonHandler : DefaultCannonHandler.INSTANCE;
    }

    /**
     * Registers a custom airtight cannon handler for the supplied target.
     *
     * @param location the resource location identifying the target value
     * @param handler  the handler to register or invoke
     */
    public static void register(ResourceLocation location, AirtightCannonHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Cannon Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasType);
        if (cannonHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Cannon Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        float consumptionMultiplier = handler.getGasConsumptionMultiplier();
        if (!GasConsumptionUtils.isNonNegativeFinite(consumptionMultiplier)) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Cannon Handler for gas '{}': consumption multiplier must be finite and non-negative, got {}.", location, consumptionMultiplier);
            return;
        }

        if (handler instanceof AirtightCannonVisualHandler visualHandler && !GasConsumptionUtils.isFinite(visualHandler.getRotationSpeed())) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Cannon Handler for gas '{}': rotation speed must be finite, got {}.", location, visualHandler.getRotationSpeed());
            return;
        }

        AirtightCannonHandler.REGISTRY.register(gasType, handler);
        if (!(handler instanceof AirtightCannonVisualHandler visualHandler)) {
            return;
        }

        AirtightCannonVisualHandlerUtils.register(location, visualHandler);
    }
}
