package net.ty.createcraftedbeginning.api.coolantshandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for airtight coolant behaviour.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCoolantHandlerUtils {
    private AirtightCoolantHandlerUtils() {
    }

    /**
     * Resolves the airtight coolant handler associated with the supplied input.
     *
     * @param block the target block
     * @return the resolved airtight coolant handler
     */
    public static AirtightCoolantHandler of(Block block) {
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandler.REGISTRY.get(block);
        if (coolantHandler == null) {
            return DefaultCoolantHandler.INSTANCE;
        }
        return coolantHandler;
    }

    /**
     * Registers a custom airtight coolant handler for the supplied target.
     *
     * @param block   the target block
     * @param handler the handler to register or invoke
     */
    public static void register(Block block, AirtightCoolantHandler handler) {
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandler.REGISTRY.get(block);
        if (coolantHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Coolant Handler for block '{}': a handler is already registered.", BuiltInRegistries.BLOCK.getKey(block));
            return;
        }

        AirtightCoolantHandler.REGISTRY.register(block, handler);
    }
}
