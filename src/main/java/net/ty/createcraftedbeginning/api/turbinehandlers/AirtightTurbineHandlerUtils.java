package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTurbineHandlerUtils {
    private AirtightTurbineHandlerUtils() {
    }

    public static AirtightTurbineHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

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
