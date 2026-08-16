package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightEngineHandlerUtils {
    public static final double MAX_WORK_FACTOR = 32;

    private AirtightEngineHandlerUtils() {
    }

    public static AirtightEngineHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

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

    public static void register(ResourceLocation location, double workFactor) {
        register(location, workFactor, AirtightEngineHandler.MAX_LEVEL);
    }

    public static void register(ResourceLocation location, double workFactor, int maxLevel) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register Airtight Engine Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightEngineHandler engineHandler = AirtightEngineHandler.REGISTRY.get(gasType);
        if (engineHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Engine Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (!GasConsumptions.isFinite(workFactor) || workFactor < 0 || workFactor > MAX_WORK_FACTOR) {
            CCBAPI.LOGGER.error("Failed to register Airtight Engine Handler for gas '{}': work factor is out of range! Valid range is [0, {}].", location, MAX_WORK_FACTOR);
            return;
        }

        if (maxLevel < 0 || maxLevel > AirtightEngineHandler.MAX_LEVEL) {
            CCBAPI.LOGGER.error("Failed to register Airtight Engine Handler for gas '{}': maximum level is out of range! Valid range is [0, {}].", location, AirtightEngineHandler.MAX_LEVEL);
            return;
        }

        AirtightEngineHandler.REGISTRY.register(gasType, new AirtightEngineHandler() {
            @Override
            public double getWorkFactor() {
                return workFactor;
            }

            @Override
            public int getMaxLevel() {
                return maxLevel;
            }
        });
    }
}
