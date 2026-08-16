package net.ty.createcraftedbeginning.api.fillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightFillHandlerUtils {
    private AirtightFillHandlerUtils() {
    }

    public static AirtightFillHandler of(Block block) {
        AirtightFillHandler fillHandler = AirtightFillHandler.REGISTRY.get(block);
        if (fillHandler == null) {
            return new DefaultFillHandlers();
        }
        return fillHandler;
    }

    public static void register(Block block, AirtightFillHandler handler) {
        AirtightFillHandler fillHandler = AirtightFillHandler.REGISTRY.get(block);
        if (fillHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Fill Handler for block '{}': a handler is already registered.", BuiltInRegistries.BLOCK.getKey(block));
            return;
        }

        AirtightFillHandler.REGISTRY.register(block, handler);
    }
}
