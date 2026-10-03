package net.ty.createcraftedbeginning.content.opticalpower.network;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork.RefreshResult;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalPowerNetworkManager {
    private static final int DYNAMIC_REFRESH_INTERVAL_TICKS = 20;
    private static final int MAX_TOPOLOGY_REBUILDS_PER_TICK = 8;
    private static final Map<ServerLevel, LevelCache> LEVELS = new IdentityHashMap<>();

    private OpticalPowerNetworkManager() {
    }

    public static void registerConsumer(Level level, BlockPos consumerPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LevelCache cache = getCache(serverLevel);
        CachedNetwork network = cache.networkByConsumer.get(consumerPos.asLong());
        if (network != null && network.valid) {
            applyAllocation(serverLevel, consumerPos, network.network.getAllocatedPowerLp(consumerPos));
            return;
        }

        cache.dirtyConsumers.add(consumerPos.asLong());
    }

    public static void ensureConsumer(Level level, BlockPos consumerPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LevelCache cache = getCache(serverLevel);
        CachedNetwork network = cache.networkByConsumer.get(consumerPos.asLong());
        if (network != null && network.valid) {
            return;
        }

        cache.dirtyConsumers.add(consumerPos.asLong());
    }

    public static void invalidateAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LevelCache cache = LEVELS.get(serverLevel);
        if (cache == null) {
            return;
        }

        ObjectOpenHashSet<CachedNetwork> affected = new ObjectOpenHashSet<>();
        Set<CachedNetwork> nodeNetworks = cache.networksByNode.get(pos.asLong());
        if (nodeNetworks != null) {
            affected.addAll(nodeNetworks);
        }

        Set<CachedNetwork> dependencyNetworks = cache.networkBySourceDependency.get(pos.asLong());
        if (dependencyNetworks != null) {
            affected.addAll(dependencyNetworks);
        }

        for (CachedNetwork network : affected) {
            cache.invalidate(serverLevel, network);
        }
    }

    public static void invalidateAround(Level level, BlockPos pos) {
        invalidateAt(level, pos);
        for (Direction direction : Iterate.directions) {
            invalidateAt(level, pos.relative(direction));
        }
    }

    public static void invalidateAmethystCollectorPanelChange(Level level, BlockPos pos) {
        invalidateAround(level, pos);
    }

    public static void onChunkAccessibilityChanged(ServerLevel level, ChunkPos chunkPos) {
        invalidateChunkNeighborhood(level, chunkPos);
    }

    public static void tick(ServerLevel level) {
        LevelCache cache = LEVELS.get(level);
        if (cache == null) {
            return;
        }

        List<CachedNetwork> changedNetworks = List.copyOf(cache.networksWithChangedAccessibility);
        cache.networksWithChangedAccessibility.clear();
        for (CachedNetwork network : changedNetworks) {
            cache.invalidate(level, network);
        }
        cache.refreshConfiguredLimits(level);
        cache.refreshDynamicNetworks(level);
        cache.rebuildDirtyNetworks(level);
    }

    public static void removeLevel(ServerLevel level) {
        LEVELS.remove(level);
    }

    public static void clear() {
        LEVELS.clear();
    }

    @Internal
    public static @Nullable OpticalPowerNetwork findCachedNetwork(ServerLevel level, BlockPos consumerPos) {
        LevelCache cache = LEVELS.get(level);
        if (cache == null) {
            return null;
        }

        CachedNetwork cached = cache.networkByConsumer.get(consumerPos.asLong());
        if (cached == null || !cached.valid) {
            return null;
        }

        return cached.network;
    }

    static void queueChunkAccessibilityChange(ServerLevel level, ChunkPos chunkPos) {
        LevelCache cache = LEVELS.get(level);
        if (cache == null) {
            return;
        }

        cache.networksWithChangedAccessibility.addAll(findChunkNeighborhoodNetworks(cache, chunkPos));
    }

    private static void invalidateChunkNeighborhood(ServerLevel level, ChunkPos chunkPos) {
        LevelCache cache = LEVELS.get(level);
        if (cache == null) {
            return;
        }

        for (CachedNetwork network : findChunkNeighborhoodNetworks(cache, chunkPos)) {
            cache.invalidate(level, network);
        }
    }

    private static ObjectOpenHashSet<CachedNetwork> findChunkNeighborhoodNetworks(LevelCache cache, ChunkPos chunkPos) {
        ObjectOpenHashSet<CachedNetwork> affected = new ObjectOpenHashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long chunkKey = ChunkPos.asLong(chunkPos.x + dx, chunkPos.z + dz);
                Set<CachedNetwork> networks = cache.networkByChunk.get(chunkKey);
                if (networks == null) {
                    continue;
                }

                affected.addAll(networks);
            }
        }

        return affected;
    }

    private static LevelCache getCache(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, ignored -> new LevelCache());
    }

    private static void applyAllocation(ServerLevel level, BlockPos consumerPos, int powerLp) {
        if (!level.isLoaded(consumerPos) || !(level.getBlockEntity(consumerPos) instanceof OpticalPowerConsumerBlockEntity consumer)) {
            return;
        }

        consumer.applyOpticalPowerAllocation(powerLp);
    }

    private static final class LevelCache {
        private final Long2ObjectOpenHashMap<CachedNetwork> networkByConsumer = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> networksByNode = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> networkBySourceDependency = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> networkByChunk = new Long2ObjectOpenHashMap<>();
        private final LongOpenHashSet dirtyConsumers = new LongOpenHashSet();
        private final ObjectOpenHashSet<CachedNetwork> networksWithChangedAccessibility = new ObjectOpenHashSet<>();
        private final List<ObjectOpenHashSet<CachedNetwork>> dynamicBuckets = createDynamicBuckets();
        private int nextDynamicBucket;
        private int observedNetworkPowerLimit = -1;

        private static List<ObjectOpenHashSet<CachedNetwork>> createDynamicBuckets() {
            List<ObjectOpenHashSet<CachedNetwork>> buckets = new ArrayList<>(DYNAMIC_REFRESH_INTERVAL_TICKS);
            for (int i = 0; i < DYNAMIC_REFRESH_INTERVAL_TICKS; i++) {
                buckets.add(new ObjectOpenHashSet<>());
            }
            return buckets;
        }

        private void pushAllocations(ServerLevel level, CachedNetwork cached) {
            List<BlockPos> consumers = cached.network.getConsumers();
            for (int i = 0; i < consumers.size(); i++) {
                BlockPos consumer = consumers.get(i);
                if (networkByConsumer.get(consumer.asLong()) != cached) {
                    continue;
                }

                applyAllocation(level, consumer, cached.network.getAllocatedPowerLp(i));
            }
        }

        private void refreshConfiguredLimits(ServerLevel level) {
            int configuredLimit = OpticalPowerNetwork.getMaxNetworkPowerLp();
            if (configuredLimit == observedNetworkPowerLimit) {
                return;
            }

            observedNetworkPowerLimit = configuredLimit;
            ObjectOpenHashSet<CachedNetwork> uniqueNetworks = new ObjectOpenHashSet<>();
            for (CachedNetwork cached : networkByConsumer.values()) {
                if (!cached.valid) {
                    continue;
                }

                uniqueNetworks.add(cached);
            }
            for (CachedNetwork cached : uniqueNetworks) {
                pushAllocations(level, cached);
            }
        }

        private void refreshDynamicNetworks(ServerLevel level) {
            int bucketIndex = Math.floorMod(level.getGameTime(), DYNAMIC_REFRESH_INTERVAL_TICKS);
            ObjectOpenHashSet<CachedNetwork> bucket = dynamicBuckets.get(bucketIndex);
            if (bucket.isEmpty()) {
                return;
            }

            List<CachedNetwork> snapshot = List.copyOf(bucket);
            for (CachedNetwork cached : snapshot) {
                if (!cached.valid) {
                    bucket.remove(cached);
                    continue;
                }

                RefreshResult result = cached.network.refreshDynamicSources(level);
                if (result == RefreshResult.STALE) {
                    invalidate(level, cached);
                    continue;
                }

                if (result != RefreshResult.CHANGED) {
                    continue;
                }

                pushAllocations(level, cached);
            }
        }

        private void rebuildDirtyNetworks(ServerLevel level) {
            int rebuilds = 0;
            while (!dirtyConsumers.isEmpty() && rebuilds < MAX_TOPOLOGY_REBUILDS_PER_TICK) {
                LongIterator iterator = dirtyConsumers.iterator();
                long consumerLong = iterator.nextLong();
                iterator.remove();

                BlockPos consumerPos = BlockPos.of(consumerLong);
                if (!level.isLoaded(consumerPos) || !(level.getBlockState(consumerPos).getBlock() instanceof OpticalPowerConsumer)) {
                    continue;
                }

                CachedNetwork existing = networkByConsumer.get(consumerLong);
                if (existing != null && existing.valid) {
                    applyAllocation(level, consumerPos, existing.network.getAllocatedPowerLp(consumerPos));
                    continue;
                }

                OpticalPowerNetwork network = OpticalPowerNetwork.scan(level, consumerPos);
                CachedNetwork cached = new CachedNetwork(network);
                cache(cached);
                pushAllocations(level, cached);
                rebuilds++;
            }
        }

        private void cache(CachedNetwork cached) {
            for (BlockPos consumer : cached.network.getConsumers()) {
                long consumerKey = consumer.asLong();
                CachedNetwork previous = networkByConsumer.get(consumerKey);
                if (previous != null && previous.valid) {
                    continue;
                }

                networkByConsumer.put(consumerKey, cached);
                dirtyConsumers.remove(consumerKey);
            }

            LongOpenHashSet chunks = new LongOpenHashSet();
            for (BlockPos node : cached.network.getNodes()) {
                networksByNode.computeIfAbsent(node.asLong(), ignored -> new ObjectOpenHashSet<>()).add(cached);
                chunks.add(ChunkPos.asLong(node.getX() >> 4, node.getZ() >> 4));
            }
            for (BlockPos dependency : cached.network.getSourceDependencies()) {
                networkBySourceDependency.computeIfAbsent(dependency.asLong(), ignored -> new ObjectOpenHashSet<>()).add(cached);
                chunks.add(ChunkPos.asLong(dependency.getX() >> 4, dependency.getZ() >> 4));
            }
            for (long chunkKey : chunks) {
                networkByChunk.computeIfAbsent(chunkKey, ignored -> new ObjectOpenHashSet<>()).add(cached);
                cached.chunkKeys.add(chunkKey);
            }
            if (!cached.network.hasDynamicSources()) {
                return;
            }

            int bucket = nextDynamicBucket;
            nextDynamicBucket = (nextDynamicBucket + 1) % DYNAMIC_REFRESH_INTERVAL_TICKS;
            cached.dynamicBucket = bucket;
            dynamicBuckets.get(bucket).add(cached);
        }

        private void invalidate(ServerLevel level, CachedNetwork cached) {
            if (!cached.valid) {
                return;
            }

            cached.valid = false;
            for (BlockPos consumer : cached.network.getConsumers()) {
                long consumerKey = consumer.asLong();
                if (networkByConsumer.get(consumerKey) != cached) {
                    continue;
                }

                networkByConsumer.remove(consumerKey);
                applyAllocation(level, consumer, 0);
                dirtyConsumers.add(consumerKey);
            }

            for (BlockPos node : cached.network.getNodes()) {
                long nodeKey = node.asLong();
                ObjectOpenHashSet<CachedNetwork> networks = networksByNode.get(nodeKey);
                if (networks == null) {
                    continue;
                }

                networks.remove(cached);
                if (!networks.isEmpty()) {
                    continue;
                }

                networksByNode.remove(nodeKey);
            }
            for (BlockPos dependency : cached.network.getSourceDependencies()) {
                ObjectOpenHashSet<CachedNetwork> networks = networkBySourceDependency.get(dependency.asLong());
                if (networks == null) {
                    continue;
                }

                networks.remove(cached);
                if (!networks.isEmpty()) {
                    continue;
                }

                networkBySourceDependency.remove(dependency.asLong());
            }
            for (long chunkKey : cached.chunkKeys) {
                ObjectOpenHashSet<CachedNetwork> networks = networkByChunk.get(chunkKey);
                if (networks == null) {
                    continue;
                }

                networks.remove(cached);
                if (!networks.isEmpty()) {
                    continue;
                }

                networkByChunk.remove(chunkKey);
            }
            if (cached.dynamicBucket < 0) {
                return;
            }

            dynamicBuckets.get(cached.dynamicBucket).remove(cached);
        }
    }

    private static final class CachedNetwork {
        private final OpticalPowerNetwork network;
        private final LongOpenHashSet chunkKeys = new LongOpenHashSet();
        private boolean valid = true;
        private int dynamicBucket = -1;

        private CachedNetwork(OpticalPowerNetwork network) {
            this.network = network;
        }
    }
}
