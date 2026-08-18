package net.ty.createcraftedbeginning.content.photostresses.network;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.photostresses.network.PhotoStressNetwork.RefreshResult;
import net.ty.createcraftedbeginning.content.photostresses.phohostressbearing.PhotoStressBearingBlock;
import net.ty.createcraftedbeginning.content.photostresses.phohostressbearing.PhotoStressBearingBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotoStressNetworkManager {
    private static final int DYNAMIC_REFRESH_INTERVAL_TICKS = 20;
    private static final int MAX_TOPOLOGY_REBUILDS_PER_TICK = 8;
    private static final Map<ServerLevel, LevelCache> LEVELS = new IdentityHashMap<>();

    private PhotoStressNetworkManager() {
    }

    public static void registerBearing(Level level, BlockPos bearingPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LevelCache cache = getCache(serverLevel);
        CachedNetwork network = cache.networkByNode.get(bearingPos.asLong());
        if (network != null && network.valid) {
            applyAllocation(serverLevel, bearingPos, network.network.getAllocatedStressPoints(bearingPos));
            return;
        }

        cache.dirtyBearings.add(bearingPos.asLong());
    }

    public static void ensureBearing(Level level, BlockPos bearingPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LevelCache cache = getCache(serverLevel);
        CachedNetwork network = cache.networkByNode.get(bearingPos.asLong());
        if (network != null && network.valid) {
            return;
        }

        cache.dirtyBearings.add(bearingPos.asLong());
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
        CachedNetwork nodeNetwork = cache.networkByNode.get(pos.asLong());
        if (nodeNetwork != null) {
            affected.add(nodeNetwork);
        }

        Set<CachedNetwork> dependencyNetworks = cache.networkBySourceDependency.get(pos.asLong());
        if (dependencyNetworks != null) {
            affected.addAll(dependencyNetworks);
        }

        for (CachedNetwork network : affected) {
            cache.invalidate(network);
        }
    }

    public static void invalidateAround(Level level, BlockPos pos) {
        invalidateAt(level, pos);
        for (Direction direction : Direction.values()) {
            invalidateAt(level, pos.relative(direction));
        }
    }

    public static void invalidateSailChange(Level level, BlockPos pos) {
        invalidateAround(level, pos);
    }

    public static void onChunkAccessibilityChanged(ServerLevel level, ChunkPos chunkPos) {
        invalidateChunkNeighborhood(level, chunkPos);
    }

    private static void invalidateChunkNeighborhood(ServerLevel level, ChunkPos chunkPos) {
        LevelCache cache = LEVELS.get(level);
        if (cache == null) {
            return;
        }

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

        for (CachedNetwork network : affected) {
            cache.invalidate(network);
        }
    }

    public static void tick(ServerLevel level) {
        LevelCache cache = LEVELS.get(level);
        if (cache == null) {
            return;
        }

        cache.refreshDynamicNetworks(level);
        cache.rebuildDirtyNetworks(level);
    }

    public static void removeLevel(ServerLevel level) {
        LEVELS.remove(level);
    }

    public static void clear() {
        LEVELS.clear();
    }

    private static LevelCache getCache(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, ignored -> new LevelCache());
    }

    private static void applyAllocation(ServerLevel level, BlockPos bearingPos, int stressPoints) {
        if (!level.isLoaded(bearingPos) || !(level.getBlockEntity(bearingPos) instanceof PhotoStressBearingBlockEntity bearing)) {
            return;
        }

        bearing.applyNetworkAllocation(stressPoints);
    }

    private static final class LevelCache {
        private final Long2ObjectOpenHashMap<CachedNetwork> networkByNode = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> networkBySourceDependency = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> networkByChunk = new Long2ObjectOpenHashMap<>();
        private final LongOpenHashSet dirtyBearings = new LongOpenHashSet();
        private final List<ObjectOpenHashSet<CachedNetwork>> dynamicBuckets = createDynamicBuckets();
        private int nextDynamicBucket;

        private static List<ObjectOpenHashSet<CachedNetwork>> createDynamicBuckets() {
            List<ObjectOpenHashSet<CachedNetwork>> buckets = new ArrayList<>(DYNAMIC_REFRESH_INTERVAL_TICKS);
            for (int i = 0; i < DYNAMIC_REFRESH_INTERVAL_TICKS; i++) {
                buckets.add(new ObjectOpenHashSet<>());
            }
            return buckets;
        }

        private static void pushAllocations(ServerLevel level, CachedNetwork cached) {
            List<BlockPos> bearings = cached.network.getBearings();
            for (int i = 0; i < bearings.size(); i++) {
                applyAllocation(level, bearings.get(i), cached.network.getAllocatedStressPoints(i));
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
                    invalidate(cached);
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
            while (!dirtyBearings.isEmpty() && rebuilds < MAX_TOPOLOGY_REBUILDS_PER_TICK) {
                LongIterator iterator = dirtyBearings.iterator();
                long bearingLong = iterator.nextLong();
                iterator.remove();

                BlockPos bearingPos = BlockPos.of(bearingLong);
                if (!level.isLoaded(bearingPos) || !(level.getBlockState(bearingPos).getBlock() instanceof PhotoStressBearingBlock)) {
                    continue;
                }

                CachedNetwork existing = networkByNode.get(bearingLong);
                if (existing != null && existing.valid) {
                    applyAllocation(level, bearingPos, existing.network.getAllocatedStressPoints(bearingPos));
                    continue;
                }

                PhotoStressNetwork network = PhotoStressNetwork.scan(level, bearingPos);
                CachedNetwork cached = new CachedNetwork(network);
                cache(cached);
                pushAllocations(level, cached);
                rebuilds++;
            }
        }

        private void cache(CachedNetwork cached) {
            ObjectOpenHashSet<CachedNetwork> overlaps = new ObjectOpenHashSet<>();
            for (BlockPos node : cached.network.getNodes()) {
                CachedNetwork previous = networkByNode.get(node.asLong());
                if (previous == null || !previous.valid || previous == cached) {
                    continue;
                }

                overlaps.add(previous);
            }
            for (BlockPos dependency : cached.network.getSourceDependencies()) {
                Set<CachedNetwork> previous = networkBySourceDependency.get(dependency.asLong());
                if (previous == null) {
                    continue;
                }

                for (CachedNetwork network : previous) {
                    if (!network.valid || network == cached) {
                        continue;
                    }

                    overlaps.add(network);
                }
            }
            for (CachedNetwork overlap : overlaps) {
                invalidate(overlap);
            }

            LongOpenHashSet chunks = new LongOpenHashSet();
            for (BlockPos node : cached.network.getNodes()) {
                networkByNode.put(node.asLong(), cached);
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

            for (BlockPos bearing : cached.network.getBearings()) {
                dirtyBearings.remove(bearing.asLong());
            }
            if (!cached.network.hasDynamicSources()) {
                return;
            }

            int bucket = nextDynamicBucket;
            nextDynamicBucket = (nextDynamicBucket + 1) % DYNAMIC_REFRESH_INTERVAL_TICKS;
            cached.dynamicBucket = bucket;
            dynamicBuckets.get(bucket).add(cached);
        }

        private void invalidate(CachedNetwork cached) {
            if (!cached.valid) {
                return;
            }

            cached.valid = false;
            for (BlockPos bearing : cached.network.getBearings()) {
                dirtyBearings.add(bearing.asLong());
            }

            for (BlockPos node : cached.network.getNodes()) {
                long nodeKey = node.asLong();
                if (networkByNode.get(nodeKey) != cached) {
                    continue;
                }

                networkByNode.remove(nodeKey);
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
        private final PhotoStressNetwork network;
        private final LongOpenHashSet chunkKeys = new LongOpenHashSet();
        private boolean valid = true;
        private int dynamicBucket = -1;

        private CachedNetwork(PhotoStressNetwork network) {
            this.network = network;
        }
    }
}
