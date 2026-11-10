package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalPowerNetworkManager {
    private static final Map<ServerLevel, OpticalPowerNetworkCache> LEVELS = new IdentityHashMap<>();

    private OpticalPowerNetworkManager() {
    }

    public static void registerConsumer(Level level, BlockPos consumerPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        getOrCreateCache(serverLevel).registerConsumer(serverLevel, consumerPos);
    }

    public static void requestConsumerRebuildIfUnassigned(Level level, BlockPos consumerPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        getOrCreateCache(serverLevel).requestConsumerRebuildIfUnassigned(consumerPos);
    }

    public static void invalidateAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        OpticalPowerNetworkCache cache = LEVELS.get(serverLevel);
        if (cache == null) {
            return;
        }

        cache.invalidateNetworksAt(serverLevel, pos);
    }

    public static void invalidateAround(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        OpticalPowerNetworkCache cache = LEVELS.get(serverLevel);
        if (cache == null) {
            return;
        }

        cache.invalidateNetworksAt(serverLevel, pos);
        for (Direction direction : Iterate.directions) {
            cache.invalidateNetworksAt(serverLevel, pos.relative(direction));
        }
    }

    static void markNetworksNearChunkForInvalidation(ServerLevel level, ChunkPos chunkPos) {
        OpticalPowerNetworkCache cache = LEVELS.get(level);
        if (cache == null) {
            return;
        }

        cache.markNetworksNearChunkForInvalidation(chunkPos);
    }

    static void tick(ServerLevel level) {
        OpticalPowerNetworkCache cache = LEVELS.get(level);
        if (cache == null) {
            return;
        }

        cache.tick(level);
    }

    static void removeLevel(ServerLevel level) {
        LEVELS.remove(level);
    }

    static void clear() {
        LEVELS.clear();
    }

    static @Nullable OpticalPowerNetwork findCachedNetwork(ServerLevel level, BlockPos consumerPos) {
        OpticalPowerNetworkCache cache = LEVELS.get(level);
        if (cache == null) {
            return null;
        }

        return cache.findNetwork(consumerPos);
    }

    private static OpticalPowerNetworkCache getOrCreateCache(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, ignored -> new OpticalPowerNetworkCache());
    }
}
