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
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandlerUtils;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.CoolantEfficiency;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Coolant Handlers for blocks.
 * <p>
 * This event is exposed through {@code CCBEvents.airtightCoolantHandler} and
 * allows scripts to associate blocks, tags, or block predicates with custom
 * coolant behavior.
 * <p>
 * A coolant handler receives the level, block position, and block state, then
 * returns both a coolant efficiency value and the {@link ResourceLocation} of the
 * block that should replace the coolant after melting.
 * <p>
 * Example usage in KubeJS:
 * <pre>{@code
 * CCBEvents.airtightCoolantHandler((event) => {
 *     event.add('minecraft:snow', (level, pos, state) => {
 *         return 1
 *     }, (level, pos, state) => {
 *         return 'minecraft:air'
 *     })
 * })
 * }</pre>
 * <pre>{@code
 * CCBEvents.airtightCoolantHandler((event) => {
 *     event.addAdvanced('#minecraft:ice', (level, pos, state) => {
 *         return 2
 *     }, (level, pos, state) => {
 *         return 'minecraft:water'
 *     })
 * })
 * }</pre>
 * <p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class AirtightCoolantHandlerEvent implements KubeEvent {
    /**
     * Registers an Airtight Coolant Handler for the given block.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal
     * Java-side usage.
     *
     * @param block   the block to register the coolant handler for
     * @param handler the airtight coolant handler to register
     * @see AirtightCoolantHandlerUtils#register(Block, AirtightCoolantHandler)
     */
    @HideFromJS
    public static void add(Block block, AirtightCoolantHandler handler) {
        AirtightCoolantHandlerUtils.register(block, handler);
    }

    /**
     * Registers an Airtight Coolant Handler for the given block.
     * <p>
     * The provided {@link EfficiencyCoolantHandler} returns the coolant efficiency
     * value used by the target block, while the provided {@link MeltCoolantHandler}
     * returns the resource location of the block that should replace it after
     * melting.
     * <p>
     * The efficiency value is converted by {@link CoolantEfficiency#fromInt(int)}.
     *
     * @param block      the block to register the coolant handler for
     * @param efficiency the efficiency handler to execute
     * @param melt       the melt handler to execute
     * @see AirtightCoolantHandlerUtils#register(Block, EfficiencyCoolantHandler, MeltCoolantHandler)
     */
    public void add(Block block, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandlerUtils.register(block, efficiency, melt);
    }

    /**
     * Registers an Airtight Coolant Handler for blocks matching the given predicate.
     * <p>
     * This is useful for registering handlers for block tags, block groups, or
     * more advanced block matching rules.
     * <p>
     * The provided {@link EfficiencyCoolantHandler} returns the coolant efficiency
     * value used by matching blocks, while the provided {@link MeltCoolantHandler}
     * returns the resource location of the block that should replace each matching
     * block after melting.
     *
     * @param predicate  the block state predicate used to select matching blocks
     * @param efficiency the efficiency handler to execute
     * @param melt       the melt handler to execute
     * @see AirtightCoolantHandlerUtils#register(BlockStatePredicate, EfficiencyCoolantHandler, MeltCoolantHandler)
     */
    public void addAdvanced(BlockStatePredicate predicate, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandlerUtils.register(predicate, efficiency, melt);
    }

    /**
     * Functional interface used by KubeJS scripts to define coolant efficiency.
     * <p>
     * The handler is called with the level, target block position, and block state,
     * then returns an integer that is converted into a {@link CoolantEfficiency}.
     * By default, {@code 0} or lower is none, {@code 1} is basic, {@code 2} is advanced,
     * and {@code 3} or higher is extreme.
     */
    @FunctionalInterface
    public interface EfficiencyCoolantHandler {
        /**
         * Returns the coolant efficiency value for the target block.
         *
         * @param level the level where the coolant is checked
         * @param pos   the target block position
         * @param state the block state at the target position
         * @return the integer coolant efficiency value
         */
        int apply(Level level, BlockPos pos, BlockState state);
    }

    /**
     * Functional interface used by KubeJS scripts to define coolant melt behavior.
     * <p>
     * The handler is called with the level, target block position, and block state,
     * then returns the resource location of the block that should replace the
     * coolant after melting.
     */
    @FunctionalInterface
    public interface MeltCoolantHandler {
        /**
         * Returns the block produced after the target coolant block melts.
         *
         * @param level the level where the coolant is checked
         * @param pos   the target block position
         * @param state the block state at the target position
         * @return the resource location of the block to replace the coolant with
         */
        ResourceLocation apply(Level level, BlockPos pos, BlockState state);
    }
}
