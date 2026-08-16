package net.ty.createcraftedbeginning.api.coolantshandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCoolantHandlerUtils {
    private AirtightCoolantHandlerUtils() {
    }

    public static AirtightCoolantHandler of(Block block) {
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandler.REGISTRY.get(block);
        if (coolantHandler == null) {
            return DefaultCoolantHandler.INSTANCE;
        }
        return coolantHandler;
    }

    public static void register(Block block, AirtightCoolantHandler handler) {
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandler.REGISTRY.get(block);
        if (coolantHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Coolant Handler for block '{}': a handler is already registered.", BuiltInRegistries.BLOCK.getKey(block));
            return;
        }

        AirtightCoolantHandler.REGISTRY.register(block, handler);
    }
}
