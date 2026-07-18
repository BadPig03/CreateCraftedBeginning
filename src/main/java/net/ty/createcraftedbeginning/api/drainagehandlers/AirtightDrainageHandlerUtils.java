package net.ty.createcraftedbeginning.api.drainagehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrainageHandlerEvent.DrainageHandler;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for gas-specific airtight drainage effects.
 * Handlers control effect application, outline rendering, and the outline inflation applied to drained gas.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightDrainageHandlerUtils {
    private static final AirtightDrainageHandler DEFAULT_HANDLER = new DefaultDrainageHandler();

    private AirtightDrainageHandlerUtils() {
    }

    /**
     * Resolves the airtight drainage handler associated with the supplied input.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resolved airtight drainage handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightDrainageHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Resolves the airtight drainage handler associated with the supplied input.
     *
     * @param gasType the gas type to inspect or process
     * @return the resolved airtight drainage handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightDrainageHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightDrainageHandler drainageHandler = AirtightDrainageHandler.REGISTRY.get(gasType);
        if (drainageHandler == null) {
            return DEFAULT_HANDLER;
        }
        return drainageHandler;
    }

    /**
     * Registers a custom airtight drainage handler for the supplied target.
     *
     * @param location    the resource location identifying the target value
     * @param inflation   the inflation value to use
     * @param showOutline whether show outline is enabled
     * @param handler     the handler to register or invoke
     */
    public static void register(ResourceLocation location, float inflation, boolean showOutline, DrainageHandler handler) {
        register(location, new AirtightDrainageHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public float getInflation() {
                return inflation;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean shouldShowOutline() {
                return showOutline;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void apply(Level level, BlockPos pos, Direction direction, Gas gasType) {
                if (showOutline) {
                    showOutline(level, pos, direction, inflation, gasType.getTint());
                }
                handler.apply(level, pos, direction, gasType);
            }
        });
    }

    /**
     * Registers a custom airtight drainage handler for the supplied target.
     *
     * @param location the resource location identifying the target value
     * @param handler  the handler to register or invoke
     */
    public static void register(ResourceLocation location, AirtightDrainageHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drainage Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightDrainageHandler drainageHandler = AirtightDrainageHandler.REGISTRY.get(gasType);
        if (drainageHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drainage Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        AirtightDrainageHandler.REGISTRY.register(gasType, handler);
    }
}
