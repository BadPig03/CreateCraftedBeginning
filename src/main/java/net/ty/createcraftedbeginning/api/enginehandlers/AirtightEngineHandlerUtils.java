package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for gas-specific airtight engine behaviour.
 * Registered handlers determine the work factor and maximum engine level contributed
 * by each supported gas.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightEngineHandlerUtils {
    public static final double MAX_WORK_FACTOR = 32;

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
     * Registers a custom airtight engine handler that can reach the normal maximum level.
     *
     * @param location   the resource location identifying the target gas
     * @param workFactor the effective supply contributed by each unit of gas
     */
    public static void register(ResourceLocation location, double workFactor) {
        register(location, workFactor, AirtightEngineHandler.MAX_LEVEL);
    }

    /**
     * Registers a custom airtight engine handler for the supplied target.
     *
     * @param location   the resource location identifying the target gas
     * @param workFactor the effective supply contributed by each unit of gas
     * @param maxLevel   the highest airtight engine level the gas can sustain
     */
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

        if (!Double.isFinite(workFactor) || workFactor < 0 || workFactor > MAX_WORK_FACTOR) {
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
