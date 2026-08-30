package net.ty.createcraftedbeginning.api.cannonhandlers.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.DefaultCannonHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonVisualHandlerUtils {
    private AirtightCannonVisualHandlerUtils() {
    }

    public static AirtightCannonVisualHandler of(GasStack gasStack) {
        return of(gasStack.getGasType());
    }

    public static AirtightCannonVisualHandler of(Gas gasType) {
        if (gasType.isEmpty()) {
            return DefaultCannonHandler.INSTANCE;
        }

        AirtightCannonVisualHandler handler = AirtightCannonVisualHandler.REGISTRY.get(gasType);
        if (handler == null) {
            return DefaultCannonHandler.INSTANCE;
        }
        return handler;
    }

    public static void register(ResourceLocation location, AirtightCannonVisualHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register Airtight Cannon Visual Handler: gas '{}' does not exist.", location);
            return;
        }

        if (AirtightCannonVisualHandler.REGISTRY.get(gasType) != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Cannon Visual Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        AirtightCannonVisualHandler.REGISTRY.register(gasType, handler);
    }
}
