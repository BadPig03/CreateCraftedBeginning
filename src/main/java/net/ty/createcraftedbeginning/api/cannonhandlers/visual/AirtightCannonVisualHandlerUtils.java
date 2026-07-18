package net.ty.createcraftedbeginning.api.cannonhandlers.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.cannonhandlers.DefaultCannonHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonVisualHandlerUtils {
    private AirtightCannonVisualHandlerUtils() {
    }

    /**
     * Resolves the airtight cannon visual handler associated with the supplied input.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resolved airtight cannon visual handler
     */
    public static AirtightCannonVisualHandler of(GasStack gasStack) {
        return of(gasStack.getGasType());
    }

    /**
     * Resolves the airtight cannon visual handler associated with the supplied input.
     *
     * @param gasType the gas type to inspect or process
     * @return the resolved airtight cannon visual handler
     */
    public static AirtightCannonVisualHandler of(Gas gasType) {
        if (gasType.isEmpty()) {
            return DefaultCannonHandler.INSTANCE;
        }

        AirtightCannonVisualHandler handler = AirtightCannonVisualHandler.REGISTRY.get(gasType);
        return handler != null ? handler : DefaultCannonHandler.INSTANCE;
    }

    /**
     * Registers a custom airtight cannon visual handler for the supplied target.
     *
     * @param location the resource location identifying the target value
     * @param handler  the handler to register or invoke
     */
    public static void register(ResourceLocation location, AirtightCannonVisualHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Cannon Visual Handler: gas '{}' does not exist.", location);
            return;
        }

        if (AirtightCannonVisualHandler.REGISTRY.get(gasType) != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Cannon Visual Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        AirtightCannonVisualHandler.REGISTRY.register(gasType, handler);
    }
}
