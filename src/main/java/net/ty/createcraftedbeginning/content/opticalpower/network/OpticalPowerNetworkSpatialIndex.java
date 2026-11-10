package net.ty.createcraftedbeginning.content.opticalpower.network;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkCache.CachedNetwork;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalPowerNetworkSpatialIndex {
    private static final int CHUNK_INVALIDATION_RADIUS = 1;

    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> networksByNode = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> networksBySourceDependency = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> networksByChunk = new Long2ObjectOpenHashMap<>();
    private final Map<CachedNetwork, LongOpenHashSet> chunkKeysByNetwork = new IdentityHashMap<>();

    private static void indexPositions(Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> index, Iterable<BlockPos> positions, CachedNetwork cached, LongOpenHashSet chunkKeys) {
        for (BlockPos pos : positions) {
            index.computeIfAbsent(pos.asLong(), $ -> new ObjectOpenHashSet<>()).add(cached);
            chunkKeys.add(ChunkPos.asLong(pos));
        }
    }

    private static void removeFromIndex(Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> index, long key, CachedNetwork cached) {
        ObjectOpenHashSet<CachedNetwork> networks = index.get(key);
        if (networks == null) {
            return;
        }

        networks.remove(cached);
        if (!networks.isEmpty()) {
            return;
        }

        index.remove(key);
    }

    private static void collectNetworks(Long2ObjectOpenHashMap<ObjectOpenHashSet<CachedNetwork>> index, long key, Set<CachedNetwork> result) {
        Set<CachedNetwork> networks = index.get(key);
        if (networks == null) {
            return;
        }

        result.addAll(networks);
    }

    void add(CachedNetwork cached) {
        OpticalPowerNetwork network = cached.network;
        LongOpenHashSet chunkKeys = new LongOpenHashSet();
        indexPositions(networksByNode, network.getNodes(), cached, chunkKeys);
        indexPositions(networksBySourceDependency, network.getSourceDependencies(), cached, chunkKeys);
        for (long chunkKey : chunkKeys) {
            networksByChunk.computeIfAbsent(chunkKey, $ -> new ObjectOpenHashSet<>()).add(cached);
        }
        chunkKeysByNetwork.put(cached, chunkKeys);
    }

    void remove(CachedNetwork cached) {
        OpticalPowerNetwork network = cached.network;
        for (BlockPos node : network.getNodes()) {
            removeFromIndex(networksByNode, node.asLong(), cached);
        }
        for (BlockPos dependency : network.getSourceDependencies()) {
            removeFromIndex(networksBySourceDependency, dependency.asLong(), cached);
        }
        for (long chunkKey : chunkKeysByNetwork.remove(cached)) {
            removeFromIndex(networksByChunk, chunkKey, cached);
        }
    }

    Set<CachedNetwork> networksAt(BlockPos pos) {
        long posKey = pos.asLong();
        Set<CachedNetwork> affected = new ObjectOpenHashSet<>();
        collectNetworks(networksByNode, posKey, affected);
        collectNetworks(networksBySourceDependency, posKey, affected);
        return affected;
    }

    void collectNetworksNearChunk(ChunkPos chunkPos, Set<CachedNetwork> result) {
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        for (int dx = -CHUNK_INVALIDATION_RADIUS; dx <= CHUNK_INVALIDATION_RADIUS; dx++) {
            int x = chunkX + dx;
            for (int dz = -CHUNK_INVALIDATION_RADIUS; dz <= CHUNK_INVALIDATION_RADIUS; dz++) {
                collectNetworks(networksByChunk, ChunkPos.asLong(x, chunkZ + dz), result);
            }
        }
    }
}
