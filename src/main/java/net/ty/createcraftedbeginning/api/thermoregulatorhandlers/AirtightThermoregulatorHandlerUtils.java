package net.ty.createcraftedbeginning.api.thermoregulatorhandlers;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightThermoregulatorHandlerEvent.ThermoregulatorHandler;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility class for resolving and registering Airtight Thermoregulator Handlers.
 * <p>
 * Thermoregulator handlers are used to determine the thermoregulator value
 * provided by a block.
 * <p>
 * This class provides both Java-side registration methods using
 * {@link AirtightThermoregulatorHandler} directly and KubeJS-friendly
 * registration methods using {@link ThermoregulatorHandler}.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightThermoregulatorHandlerUtils {
    private AirtightThermoregulatorHandlerUtils() {
    }

    /**
     * Gets the Airtight Thermoregulator Handler registered for the given block.
     * <p>
     * If no handler is registered for the block, a
     * {@link DefaultThermoregulatorHandler} instance is returned.
     *
     * @param block the block to get the thermoregulator handler for
     * @return the registered thermoregulator handler, or the default
     * thermoregulator handler if none is registered
     */
    public static AirtightThermoregulatorHandler of(Block block) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler == null) {
            return new DefaultThermoregulatorHandler();
        }
        return thermoregulatorHandler;
    }

    /**
     * Registers a KubeJS thermoregulator handler for the given block.
     * <p>
     * The provided {@link ThermoregulatorHandler} returns the thermoregulator value
     * used by the target block.
     *
     * @param block   the block to register the thermoregulator handler for
     * @param handler the KubeJS thermoregulator handler to register
     * @see #register(Block, AirtightThermoregulatorHandler)
     */
    public static void register(Block block, ThermoregulatorHandler handler) {
        register(block, (AirtightThermoregulatorHandler) handler::apply);
    }

    /**
     * Registers a KubeJS thermoregulator handler for blocks matching the given
     * predicate.
     * <p>
     * The provided {@link ThermoregulatorHandler} returns the thermoregulator value
     * used by each matching block.
     *
     * @param predicate the block state predicate used to select matching blocks
     * @param handler   the KubeJS thermoregulator handler to register
     * @see #register(BlockStatePredicate, AirtightThermoregulatorHandler)
     */
    public static void register(BlockStatePredicate predicate, ThermoregulatorHandler handler) {
        register(predicate, (AirtightThermoregulatorHandler) handler::apply);
    }

    /**
     * Registers an Airtight Thermoregulator Handler for the given block.
     * <p>
     * If a handler is already registered for the block, registration is skipped and
     * an error is written to the mod logger.
     *
     * @param block   the block to register the thermoregulator handler for
     * @param handler the airtight thermoregulator handler to register
     */
    public static void register(Block block, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Thermoregulator Handler for block '{}': a handler is already registered.", block.kjs$getIdLocation());
            return;
        }

        AirtightThermoregulatorHandler.REGISTRY.register(block, handler);
    }

    /**
     * Registers an Airtight Thermoregulator Handler provider for blocks matching
     * the given predicate.
     * <p>
     * The registered provider returns the supplied handler when the predicate
     * matches a block, otherwise it returns {@code null} so that other handlers or
     * fallback behavior may be used.
     *
     * @param predicate the block state predicate used to select matching blocks
     * @param handler   the airtight thermoregulator handler to register
     */
    public static void register(BlockStatePredicate predicate, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler.REGISTRY.registerProvider(b -> predicate.testBlock(b) ? handler : null);
    }
}
