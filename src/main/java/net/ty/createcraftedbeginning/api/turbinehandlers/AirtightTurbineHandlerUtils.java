package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for gas-specific airtight turbine behaviour.
 * Registered handlers determine the maximum Tesla Turbine operating level unlocked
 * by each supported gas. This value is a gas grade, not a linear energy multiplier.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTurbineHandlerUtils {
    private AirtightTurbineHandlerUtils() {
    }

    /**
     * Resolves the airtight turbine handler associated with the supplied input.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resolved airtight turbine handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightTurbineHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Resolves the airtight turbine handler associated with the supplied input.
     *
     * @param gasType the gas type to inspect or process
     * @return the resolved airtight turbine handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightTurbineHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightTurbineHandler turbineHandler = AirtightTurbineHandler.REGISTRY.get(gasType);
        if (turbineHandler == null) {
            return DefaultTurbineHandler.INSTANCE;
        }
        return turbineHandler;
    }

    /**
     * Registers a custom airtight turbine handler for the supplied target.
     *
     * @param location the resource location identifying the target gas
     * @param maxLevel the maximum Tesla Turbine operating level unlocked by the gas
     */
    public static void register(ResourceLocation location, int maxLevel) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register Airtight Turbine Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightTurbineHandler turbineHandler = AirtightTurbineHandler.REGISTRY.get(gasType);
        if (turbineHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Turbine Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (maxLevel < 0 || maxLevel > AirtightTurbineHandler.MAX_LEVEL) {
            CCBAPI.LOGGER.error("Failed to register Airtight Turbine Handler for gas '{}': maximum level is out of range! Valid range is [0, {}].", location, AirtightTurbineHandler.MAX_LEVEL);
            return;
        }

        AirtightTurbineHandler.REGISTRY.register(gasType, () -> maxLevel);
    }
}
