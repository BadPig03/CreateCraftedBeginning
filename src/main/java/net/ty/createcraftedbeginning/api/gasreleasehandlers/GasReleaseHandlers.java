package net.ty.createcraftedbeginning.api.gasreleasehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureHandlerRegistry;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasReleaseHandlers {
    private static final GasReleaseHandler DEFAULT_HANDLER = new GasReleaseHandler() {};
    private static final GameplayPressureHandlerRegistry<GasReleaseHandler> HANDLERS = GameplayPressureHandlerRegistry.create();
    private static volatile OutlineSender outlineSender = (level, effectPos, inflation, color) -> {};

    private GasReleaseHandlers() {
    }

    public static GasReleaseHandler resolve(GasStack gasStack, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static GasReleaseHandler resolve(GasStack gasStack, GameplayPressureProfile profile) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), profile);
    }

    public static GasReleaseHandler resolve(Gas gasType, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static GasReleaseHandler resolve(Gas gasType, GameplayPressureProfile profile) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException("Gas release handler resolution requires a non-empty gas.");
        }

        GasReleaseHandler releaseHandler = HANDLERS.get(gasType, profile);
        if (releaseHandler == null) {
            return DEFAULT_HANDLER;
        }

        return releaseHandler;
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, GasReleaseHandler handler) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register gas release handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsExact(gasType, profile)) {
            CCBAPI.LOGGER.error("Failed to register gas release handler for gas '{}' and gameplay pressure profile '{}': a handler is already registered.", location, profile.id());
            return;
        }

        HANDLERS.register(gasType, profile, handler);
    }

    public static void registerOutlineSender(OutlineSender sender) {
        outlineSender = sender;
    }

    public static void showOutline(Level level, BlockPos effectPos, float inflation, int color) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        outlineSender.send(serverLevel, effectPos, inflation, color);
    }

    @FunctionalInterface
    public interface OutlineSender {
        void send(ServerLevel level, BlockPos effectPos, float inflation, int color);
    }
}
