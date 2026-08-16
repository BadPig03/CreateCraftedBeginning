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

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightDrainageHandlerUtils {
    private static final AirtightDrainageHandler DEFAULT_HANDLER = new DefaultDrainageHandler();
    private static volatile OutlineSender outlineSender = (level, pos, direction, inflation, color) -> {};

    private AirtightDrainageHandlerUtils() {
    }

    public static AirtightDrainageHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

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

    public static void registerOutlineSender(OutlineSender sender) {
        outlineSender = Objects.requireNonNull(sender);
    }

    public static void showOutline(Level level, BlockPos pos, Direction direction, float inflation, int color) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        outlineSender.send(serverLevel, pos, direction, inflation, color);
    }

    @FunctionalInterface
    public interface OutlineSender {
        void send(ServerLevel level, BlockPos pos, Direction direction, float inflation, int color);
    }
}
