package net.ty.createcraftedbeginning.api.drainagehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * Provides lookup and registration helpers for gas-specific airtight drainage effects.
 * Handlers control effect application, outline rendering, and the outline inflation applied to drained gas.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightDrainageHandlerUtils {
    private static final AirtightDrainageHandler DEFAULT_HANDLER = new DefaultDrainageHandler();
    private static volatile OutlineSender outlineSender = (level, pos, direction, inflation, color) -> {};

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
     * @param location the resource location identifying the target value
     * @param handler  the handler to register or invoke
     */
    public static void register(ResourceLocation location, AirtightDrainageHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register Airtight Drainage Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightDrainageHandler drainageHandler = AirtightDrainageHandler.REGISTRY.get(gasType);
        if (drainageHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Drainage Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        AirtightDrainageHandler.REGISTRY.register(gasType, handler);
    }

    /**
     * Installs the server-to-client outline transport used by the default drainage visuals.
     * This is a bootstrap hook; normal integrations should call {@link AirtightDrainageHandler#showOutline}.
     *
     * @param sender the sender implementation
     */
    public static void registerOutlineSender(OutlineSender sender) {
        outlineSender = Objects.requireNonNull(sender);
    }

    static void showOutline(Level level, BlockPos pos, Direction direction, float inflation, int color) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        outlineSender.send(serverLevel, pos, direction, inflation, color);
    }

    @FunctionalInterface
    public interface OutlineSender {
        /**
         * Sends an outline update for a drainage interaction.
         *
         * @param level     the server level containing the outlined position
         * @param pos       the outlined block position
         * @param direction the direction associated with the drainage interaction
         * @param inflation the amount by which the outline should be inflated
         * @param color     the outline color
         */
        void send(ServerLevel level, BlockPos pos, Direction direction, float inflation, int color);
    }

}
