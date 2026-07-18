package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for gas-specific airtight engine behavior.
 * Registered handlers determine the efficiency contributed by each supported gas.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightEngineHandlerUtils {
    private AirtightEngineHandlerUtils() {
    }

    /**
     * Resolves the airtight engine handler associated with the supplied input.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resolved airtight engine handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightEngineHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Resolves the airtight engine handler associated with the supplied input.
     *
     * @param gasType the gas type to inspect or process
     * @return the resolved airtight engine handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightEngineHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightEngineHandler engineHandler = AirtightEngineHandler.REGISTRY.get(gasType);
        if (engineHandler == null) {
            return DefaultEngineHandler.INSTANCE;
        }
        return engineHandler;
    }

    /**
     * Registers a custom airtight engine handler for the supplied target.
     *
     * @param location   the resource location identifying the target value
     * @param efficiency the efficiency value to use
     */
    public static void register(ResourceLocation location, int efficiency) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Engine Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightEngineHandler engineHandler = AirtightEngineHandler.REGISTRY.get(gasType);
        if (engineHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Engine Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (efficiency < 0 || efficiency > 16) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Engine Handler for gas '{}': efficiency is out of range! Valid range is [0, 16].", location);
            return;
        }

        AirtightEngineHandler.REGISTRY.register(gasType, () -> efficiency);
    }
}
