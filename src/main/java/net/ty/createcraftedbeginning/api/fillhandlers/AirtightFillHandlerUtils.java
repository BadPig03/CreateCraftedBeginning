package net.ty.createcraftedbeginning.api.fillhandlers;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightFillHandlerEvent.FillHandler;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for airtight gas-fill sources.
 * Handlers may be associated with individual blocks or selected dynamically through block-state predicates.
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
    public static void register(Block block, FillHandler handler) {
        register(block, (AirtightFillHandler) (l, p, s) -> CCBGasRegistries.GAS_REGISTRY.getOptional(handler.apply(l, p, s)).orElse(Gas.EMPTY_GAS_HOLDER.value()));
    }

    /**
     * Registers a custom airtight fill handler for the supplied target.
     *
     * @param predicate the predicate used to select matching values
     * @param handler   the handler to register or invoke
     */
    public static void register(BlockStatePredicate predicate, FillHandler handler) {
        register(predicate, (AirtightFillHandler) (level, pos, state) -> CCBGasRegistries.GAS_REGISTRY.getOptional(handler.apply(level, pos, state)).orElse(Gas.EMPTY_GAS_HOLDER.value()));
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
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Fill Handler for block '{}': a handler is already registered.", block.kjs$getIdLocation());
            return;
        }

        AirtightFillHandler.REGISTRY.register(block, handler);
    }

    /**
     * Registers a custom airtight fill handler for the supplied target.
     *
     * @param predicate the predicate used to select matching values
     * @param handler   the handler to register or invoke
     */
    public static void register(BlockStatePredicate predicate, AirtightFillHandler handler) {
        AirtightFillHandler.REGISTRY.registerProvider(b -> predicate.testBlock(b) ? handler : null);
    }
}
