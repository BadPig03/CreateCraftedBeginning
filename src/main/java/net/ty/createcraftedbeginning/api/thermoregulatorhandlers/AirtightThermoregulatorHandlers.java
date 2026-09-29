package net.ty.createcraftedbeginning.api.thermoregulatorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightThermoregulatorHandlers {
    private AirtightThermoregulatorHandlers() {
    }

    public static AirtightThermoregulatorHandler resolve(Block block) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler == null) {
            return new DefaultThermoregulatorHandler();
        }

        return thermoregulatorHandler;
    }

    public static void register(Block block, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler != null) {
            CCBAPI.LOGGER.error("Failed to register thermoregulator handler for block '{}': a handler is already registered.", BuiltInRegistries.BLOCK.getKey(block));
            return;
        }

        AirtightThermoregulatorHandler.REGISTRY.register(block, handler);
    }
}
