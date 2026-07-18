package net.ty.createcraftedbeginning.api.thermoregulatorhandlers;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightThermoregulatorHandlerEvent.ThermoregulatorHandler;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for airtight thermoregulator handlers.
 * The overloads support direct Java handlers as well as script-facing adapters and block-state predicates.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightThermoregulatorHandlerUtils {
    private AirtightThermoregulatorHandlerUtils() {
    }

    /**
     * Resolves the airtight thermoregulator handler associated with the supplied input.
     *
     * @param block the target block
     * @return the resolved airtight thermoregulator handler
     */
    public static AirtightThermoregulatorHandler of(Block block) {
        AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandler.REGISTRY.get(block);
        if (thermoregulatorHandler == null) {
            return new DefaultThermoregulatorHandler();
        }
        return thermoregulatorHandler;
    }

    /**
     * Registers a custom airtight thermoregulator handler for the supplied target.
     *
     * @param block   the target block
     * @param handler the handler to register or invoke
     */
    public static void register(Block block, ThermoregulatorHandler handler) {
        register(block, (AirtightThermoregulatorHandler) handler::apply);
    }

    /**
     * Registers a custom airtight thermoregulator handler for the supplied target.
     *
     * @param predicate the predicate used to select matching values
     * @param handler   the handler to register or invoke
     */
    public static void register(BlockStatePredicate predicate, ThermoregulatorHandler handler) {
        register(predicate, (AirtightThermoregulatorHandler) handler::apply);
    }

    /**
     * Registers a custom airtight thermoregulator handler for the supplied target.
     *
     * @param block   the target block
     * @param handler the handler to register or invoke
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
     * Registers a custom airtight thermoregulator handler for the supplied target.
     *
     * @param predicate the predicate used to select matching values
     * @param handler   the handler to register or invoke
     */
    public static void register(BlockStatePredicate predicate, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler.REGISTRY.registerProvider(b -> predicate.testBlock(b) ? handler : null);
    }
}
