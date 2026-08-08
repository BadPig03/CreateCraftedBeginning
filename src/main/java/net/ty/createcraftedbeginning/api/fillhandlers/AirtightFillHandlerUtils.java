package net.ty.createcraftedbeginning.api.fillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for airtight gas-fill sources.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightFillHandlerUtils {
    private AirtightFillHandlerUtils() {
    }

    /**
     * Resolves the airtight fill handler associated with the supplied input.
     *
     * @param block the target block
     * @return the resolved airtight fill handler
     */
    public static AirtightFillHandler of(Block block) {
        AirtightFillHandler fillHandler = AirtightFillHandler.REGISTRY.get(block);
        if (fillHandler == null) {
            return new DefaultFillHandlers();
        }
        return fillHandler;
    }

    /**
     * Registers a custom airtight fill handler for the supplied target.
     *
     * @param block   the target block
     * @param handler the handler to register or invoke
     */
    public static void register(Block block, AirtightFillHandler handler) {
        AirtightFillHandler fillHandler = AirtightFillHandler.REGISTRY.get(block);
        if (fillHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Fill Handler for block '{}': a handler is already registered.", BuiltInRegistries.BLOCK.getKey(block));
            return;
        }

        AirtightFillHandler.REGISTRY.register(block, handler);
    }
}
