package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandler;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Fill Handlers for blocks.
 * <p>
 * This event is exposed through {@code CCBEvents.airtightFillHandler} and allows
 * scripts to associate blocks, tags, or block predicates with custom gas fill
 * behavior.
 * <p>
 * A fill handler receives the level, block position, and block state, then returns
 * the {@link ResourceLocation} of the gas that should be produced by that block.
 * <p>
 * Example usage in KubeJS:
 * <pre>{@code
 * CCBEvents.airtightFillHandler((event) => {
 *     event.add('minecraft:oak_leave', (level, pos, state) => {
 *         return 'kubejs:oxygen'
 *     })
 * })
 * }</pre>
 * <pre>{@code
 * CCBEvents.airtightFillHandler((event) => {
 *     event.addAdvanced('#minecraft:leaves', (level, pos, state) => {
 *         return 'kubejs:oxygen'
 *     })
 * })
 * }</pre>
 * <p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class AirtightFillHandlerEvent implements KubeEvent {
    /**
     * Registers an Airtight Fill Handler for the given block.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal
     * Java-side usage.
     *
     * @param block   the block to register the fill handler for
     * @param handler the airtight fill handler to register
     * @see AirtightFillHandlerUtils#register(Block, AirtightFillHandler)
     */
    @HideFromJS
    public static void add(Block block, AirtightFillHandler handler) {
        AirtightFillHandlerUtils.register(block, handler);
    }

    /**
     * Registers an Airtight Fill Handler for the given block.
     * <p>
     * The provided {@link FillHandler} returns the resource location of the gas
     * that should be produced when the target block is used as an airtight fill
     * source.
     * <p>
     * The returned gas location is resolved by
     * {@link AirtightFillHandlerUtils#register(Block, FillHandler)}. If the
     * location does not point to a registered gas, the handler falls back to the
     * empty gas.
     *
     * @param block   the block to register the fill handler for
     * @param handler the fill handler to execute
     * @see AirtightFillHandlerUtils#register(Block, FillHandler)
     */
    public void add(Block block, FillHandler handler) {
        AirtightFillHandlerUtils.register(block, handler);
    }

    /**
     * Registers an Airtight Fill Handler for blocks matching the given predicate.
     * <p>
     * This is useful for registering handlers for block tags, block groups, or
     * more advanced block matching rules.
     * <p>
     * The provided {@link FillHandler} returns the resource location of the gas
     * that should be produced when a matching block is used as an airtight fill
     * source.
     *
     * @param predicate the block state predicate used to select matching blocks
     * @param handler   the fill handler to execute
     * @see AirtightFillHandlerUtils#register(BlockStatePredicate, FillHandler)
     */
    public void addAdvanced(BlockStatePredicate predicate, FillHandler handler) {
        AirtightFillHandlerUtils.register(predicate, handler);
    }

    /**
     * Functional interface used by KubeJS scripts to define custom fill behavior.
     * <p>
     * The handler is called with the level, target block position, and block state,
     * then returns the resource location of the gas that should be filled from that
     * block.
     */
    @FunctionalInterface
    public interface FillHandler {
        /**
         * Returns the gas produced by the target block.
         *
         * @param level the level where the fill operation occurs
         * @param pos   the target block position of the fill operation
         * @param state the block state at the target position
         * @return the resource location of the gas to produce
         */
        ResourceLocation apply(Level level, BlockPos pos, BlockState state);
    }
}
