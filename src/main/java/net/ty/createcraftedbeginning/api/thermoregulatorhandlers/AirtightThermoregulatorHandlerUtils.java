package net.ty.createcraftedbeginning.api.thermoregulatorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightThermoregulatorHandlerUtils {
    private AirtightThermoregulatorHandlerUtils() {
    }

    public static AirtightThermoregulatorHandler of(Block block) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler == null) {
            return new DefaultThermoregulatorHandler();
        }
        return thermoregulatorHandler;
    }

    public static void register(Block block, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Thermoregulator Handler for block '{}': a handler is already registered.", BuiltInRegistries.BLOCK.getKey(block));
            return;
        }

        AirtightThermoregulatorHandler.REGISTRY.register(block, handler);
    }
}
