package net.ty.createcraftedbeginning.api.thermoregulatorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for airtight thermoregulator handlers.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightThermoregulatorHandlerUtils {
    private AirtightThermoregulatorHandlerUtils() {
    }

    /**
     * Resolves the airtight thermoregulator handler associated with the supplied input.
     *
     * @param block the target block
     * @return the resolved airtight thermoregulator handler
     */
    public static AirtightThermoregulatorHandler of(Block block) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler == null) {
            return new DefaultThermoregulatorHandler();
        }
        return thermoregulatorHandler;
    }

    /**
     * Registers a custom airtight thermoregulator handler for the supplied target.
     *
     * @param block   the target block
     * @param handler the handler to register or invoke
     */
    public static void register(Block block, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Thermoregulator Handler for block '{}': a handler is already registered.", BuiltInRegistries.BLOCK.getKey(block));
            return;
        }

        AirtightThermoregulatorHandler.REGISTRY.register(block, handler);
    }
}
