package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * KubeJS event used to register Airtight Thermoregulator Handlers for blocks.
 * <p>
 * This event is exposed through {@code CCBEvents.airtightThermoregulatorHandler}
 * and allows scripts to associate blocks, tags, or block predicates with custom
 * thermoregulator behavior.
 * <p>
 * A thermoregulator handler receives the level, block position, and block state,
 * then returns the thermoregulator value used by that block.
 * <p>
 * Example usage in KubeJS:
 * <pre>{@code
 * CCBEvents.airtightThermoregulatorHandler((event) => {
 *     event.add('minecraft:stone', (level, pos, state) => {
 *         return 1.0
 *     })
 * })
 * }</pre>
 * <pre>{@code
 * CCBEvents.airtightThermoregulatorHandler((event) => {
 *     event.addAdvanced('#minecraft:stone_tool_materials', (level, pos, state) => {
 *         return 1.0
 *     })
 * })
 * }</pre>
 * <p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class AirtightThermoregulatorHandlerEvent implements KubeEvent {
    /**
     * Registers an Airtight Thermoregulator Handler for the given block.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal
     * Java-side usage.
     *
     * @param block   the block to register the thermoregulator handler for
     * @param handler the airtight thermoregulator handler to register
     * @see AirtightThermoregulatorHandlerUtils#register(Block, AirtightThermoregulatorHandler)
     */
    @HideFromJS
    public static void add(Block block, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlerUtils.register(block, handler);
    }

    /**
     * Registers an Airtight Thermoregulator Handler for the given block.
     * <p>
     * The provided {@link ThermoregulatorHandler} returns the thermoregulator value
     * used by the target block.
     *
     * @param block   the block to register the thermoregulator handler for
     * @param handler the thermoregulator handler to execute
     * @see AirtightThermoregulatorHandlerUtils#register(Block, ThermoregulatorHandler)
     */
    public void add(Block block, ThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlerUtils.register(block, handler);
    }

    /**
     * Registers an Airtight Thermoregulator Handler for blocks matching the given
     * predicate.
     * <p>
     * This is useful for registering handlers for block tags, block groups, or
     * more advanced block matching rules.
     * <p>
     * The provided {@link ThermoregulatorHandler} returns the thermoregulator value
     * used by each matching block.
     *
     * @param predicate the block state predicate used to select matching blocks
     * @param handler   the thermoregulator handler to execute
     * @see AirtightThermoregulatorHandlerUtils#register(BlockStatePredicate, ThermoregulatorHandler)
     */
    public void addAdvanced(BlockStatePredicate predicate, ThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlerUtils.register(predicate, handler);
    }

    /**
     * Functional interface used by KubeJS scripts to define custom thermoregulator
     * behavior.
     * <p>
     * The handler is called with the level, target block position, and block state,
     * then returns the thermoregulator value that should be used by that block.
     */
    @FunctionalInterface
    public interface ThermoregulatorHandler {
        /**
         * Returns the thermoregulator value for the target block.
         *
         * @param level the level where the thermoregulator is checked
         * @param pos   the target block position
         * @param state the block state at the target position
         * @return the thermoregulator value to use
         */
        float apply(Level level, BlockPos pos, BlockState state);
    }
}
