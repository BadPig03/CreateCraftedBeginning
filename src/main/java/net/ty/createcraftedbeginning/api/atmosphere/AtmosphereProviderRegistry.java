package net.ty.createcraftedbeginning.api.atmosphere;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.Gas;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AtmosphereProviderRegistry {
    public static final int DEFAULT_PRIORITY = 0;

    private static final Set<FailureKey> FAILED_PROVIDER_QUERIES = new HashSet<>();
    private static volatile AtmosphereProviderChain providers = AtmosphereProviderChain.empty();

    private AtmosphereProviderRegistry() {
    }

    public static void register(ResourceLocation id, AtmosphereProvider provider) {
        register(id, DEFAULT_PRIORITY, provider);
    }

    public static synchronized void register(ResourceLocation id, int priority, AtmosphereProvider provider) {
        providers = providers.withProvider(id, priority, provider);
    }

    public static @Nullable Gas resolveComposition(Level level, BlockPos pos) {
        return providers.resolveComposition(level, pos, AtmosphereProviderRegistry::reportFailureOnce);
    }

    public static OptionalLong resolvePressurePa(Level level, BlockPos pos) {
        return providers.resolvePressurePa(level, pos, AtmosphereProviderRegistry::reportFailureOnce);
    }

    private static synchronized void reportFailureOnce(ResourceLocation id, Level level, String queryType, RuntimeException exception) {
        if (!FAILED_PROVIDER_QUERIES.add(new FailureKey(id, queryType))) {
            return;
        }

        CCBAPI.LOGGER.error("Atmosphere provider '{}' failed while resolving {} in dimension '{}'; ignoring that claim and continuing with lower-priority providers.", id, queryType, level.dimension().location(), exception);
    }

    private record FailureKey(ResourceLocation id, String queryType) {}
}
