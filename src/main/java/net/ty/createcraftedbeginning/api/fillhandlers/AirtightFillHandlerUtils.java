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
 * Utility class for resolving and registering Airtight Fill Handlers.
 * <p>
 * Fill handlers are used to determine which gas should be produced when a block
 * is used as an airtight fill source.
 * <p>
 * This class provides both Java-side registration methods using
 * {@link AirtightFillHandler} directly and KubeJS-friendly registration methods
 * using {@link FillHandler}, where scripts return a gas resource location.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightFillHandlerUtils {
    private AirtightFillHandlerUtils() {
    }

    /**
     * Gets the Airtight Fill Handler registered for the given block.
     * <p>
     * If no handler is registered for the block, a {@link DefaultFillHandlers}
     * instance is returned.
     *
     * @param block the block to get the fill handler for
     * @return the registered fill handler, or the default fill handler if none is registered
     */
    public static AirtightFillHandler of(Block block) {
        AirtightFillHandler fillHandler = AirtightFillHandler.REGISTRY.get(block);
        if (fillHandler == null) {
            return new DefaultFillHandlers();
        }
        return fillHandler;
    }

    /**
     * Registers a KubeJS fill handler for the given block.
     * <p>
     * The provided {@link FillHandler} returns a gas resource location, which is
     * resolved through {@link CCBGasRegistries#GAS_REGISTRY}. If the returned
     * location does not point to a registered gas, the empty gas is used instead.
     *
     * @param block   the block to register the fill handler for
     * @param handler the KubeJS fill handler to register
     * @see #register(Block, AirtightFillHandler)
     * @see Gas#EMPTY_GAS_HOLDER
     */
    public static void register(Block block, FillHandler handler) {
        register(block, (AirtightFillHandler) (l, p, s) -> CCBGasRegistries.GAS_REGISTRY.getOptional(handler.apply(l, p, s)).orElse(Gas.EMPTY_GAS_HOLDER.value()));
    }

    /**
     * Registers a KubeJS fill handler for blocks matching the given predicate.
     * <p>
     * The provided {@link FillHandler} returns a gas resource location, which is
     * resolved through {@link CCBGasRegistries#GAS_REGISTRY}. If the returned
     * location does not point to a registered gas, the empty gas is used instead.
     *
     * @param predicate the block state predicate used to select matching blocks
     * @param handler   the KubeJS fill handler to register
     * @see #register(BlockStatePredicate, AirtightFillHandler)
     * @see Gas#EMPTY_GAS_HOLDER
     */
    public static void register(BlockStatePredicate predicate, FillHandler handler) {
        register(predicate, (AirtightFillHandler) (level, pos, state) -> CCBGasRegistries.GAS_REGISTRY.getOptional(handler.apply(level, pos, state)).orElse(Gas.EMPTY_GAS_HOLDER.value()));
    }

    /**
     * Registers an Airtight Fill Handler for the given block.
     * <p>
     * If a handler is already registered for the block, registration is skipped and
     * an error is written to the mod logger.
     *
     * @param block   the block to register the fill handler for
     * @param handler the airtight fill handler to register
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
     * Registers an Airtight Fill Handler provider for blocks matching the given
     * predicate.
     * <p>
     * The registered provider returns the supplied handler when the predicate
     * matches a block, otherwise it returns {@code null} so that other handlers or
     * fallback behavior may be used.
     *
     * @param predicate the block state predicate used to select matching blocks
     * @param handler   the airtight fill handler to register
     */
    public static void register(BlockStatePredicate predicate, AirtightFillHandler handler) {
        AirtightFillHandler.REGISTRY.registerProvider(b -> predicate.testBlock(b) ? handler : null);
    }
}
