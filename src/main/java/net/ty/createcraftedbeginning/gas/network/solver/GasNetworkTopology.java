package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver.AdjacentConnection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasNetworkTopology {
    private static final WorldAttached<TopologySnapshotCache> CACHES = new WorldAttached<>(ignored -> new TopologySnapshotCache());

    private GasNetworkTopology() {
    }

    public static Snapshot get(Level level, BlockPos startPos) {
        TopologySnapshotCache cache = CACHES.get(level);
        Snapshot cached = cache.byPipe.get(startPos);
        if (cached != null && cached.matchesLoadedChunks(level)) {
            GasSolverProfiler.recordTopologyCacheHit();
            return cached;
        }

        if (cached != null) {
            cache.remove(cached);
            for (BlockPos pipePos : cached.pipePositions()) {
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipePos);
                if (behaviour == null) {
                    continue;
                }

                behaviour.markConnectionsDirty();
            }
        }

        GasSolverProfiler.recordTopologyCacheMiss();
        Snapshot scanned = scan(level, startPos);
        cache.install(scanned);
        return scanned;
    }

    public static void invalidate(Level level, BlockPos changedPos) {
        if (level.isClientSide) {
            return;
        }

        CACHES.get(level).invalidateAround(changedPos);
    }

    public static int compareBlockFaces(BlockFace first, BlockFace second) {
        int pos = compareBlockPositions(first.getPos(), second.getPos());
        if (pos != 0) {
            return pos;
        }

        return Integer.compare(first.getFace().ordinal(), second.getFace().ordinal());
    }

    public static int compareBlockPositions(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }

        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }

        return Integer.compare(first.getZ(), second.getZ());
    }

    private static Snapshot scan(Level level, BlockPos startPos) {
        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockFace> endpointFaces = new LinkedHashSet<>();
        Set<BlockFace> atmosphericFaces = new LinkedHashSet<>();
        Map<ChunkPos, Boolean> loadedChunks = new HashMap<>();
        if (observeChunk(level, startPos, loadedChunks)) {
            frontier.add(startPos);
            visited.add(startPos);
        }

        while (!frontier.isEmpty()) {
            BlockPos pipePos = frontier.removeFirst();
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipePos);
            if (behaviour == null) {
                continue;
            }

            for (Direction side : Iterate.directions) {
                if (!observeChunk(level, pipePos.relative(side), loadedChunks)) {
                    continue;
                }

                GasPipeConnection connection = behaviour.getConnection(side);
                if (connection == null) {
                    continue;
                }

                AdjacentConnection adjacent = GasConnectionResolver.resolveAdjacentConnection(level, pipePos, side);
                if (adjacent.isAtmospheric()) {
                    atmosphericFaces.add(new BlockFace(pipePos, side));
                    continue;
                }

                if (adjacent.hasGasHandler()) {
                    endpointFaces.add(new BlockFace(pipePos, side));
                    continue;
                }

                GasTransportBehaviour adjacentBehaviour = adjacent.behaviour();
                if (adjacentBehaviour == null || adjacentBehaviour.getConnection(side.getOpposite()) == null) {
                    continue;
                }

                BlockPos adjacentPos = adjacent.pos();
                if (!visited.add(adjacentPos)) {
                    continue;
                }

                frontier.addLast(adjacentPos);
            }
        }

        List<BlockFace> orderedAtmosphericFaces = new ArrayList<>(atmosphericFaces);
        orderedAtmosphericFaces.sort(GasNetworkTopology::compareBlockFaces);
        List<BlockFace> orderedEndpoints = new ArrayList<>(endpointFaces);
        orderedEndpoints.sort(GasNetworkTopology::compareBlockFaces);
        return new Snapshot(Set.copyOf(visited), List.copyOf(orderedAtmosphericFaces), List.copyOf(orderedEndpoints), Map.copyOf(loadedChunks));
    }

    private static boolean observeChunk(Level level, BlockPos pos, Map<ChunkPos, Boolean> loadedChunks) {
        return loadedChunks.computeIfAbsent(new ChunkPos(pos), chunk -> level.hasChunk(chunk.x, chunk.z));
    }

    public static final class Snapshot {
        private final Set<BlockPos> pipePositions;
        private final List<BlockFace> atmosphericFaces;
        private final List<BlockFace> endpointFaces;
        private final Map<ChunkPos, Boolean> loadedChunks;
        private final Map<Object, Object> cachedArtifacts = new IdentityHashMap<>();

        private Snapshot(Set<BlockPos> pipePositions, List<BlockFace> atmosphericFaces, List<BlockFace> endpointFaces, Map<ChunkPos, Boolean> loadedChunks) {
            this.pipePositions = pipePositions;
            this.atmosphericFaces = atmosphericFaces;
            this.endpointFaces = endpointFaces;
            this.loadedChunks = loadedChunks;
        }

        public Set<BlockPos> pipePositions() {
            return pipePositions;
        }

        public List<BlockFace> atmosphericFaces() {
            return atmosphericFaces;
        }

        public List<BlockFace> endpointFaces() {
            return endpointFaces;
        }

        @SuppressWarnings("unchecked")
        public <T> T cachedArtifact(Object key, Supplier<T> factory) {
            Object cached = cachedArtifacts.get(key);
            if (cached != null) {
                return (T) cached;
            }

            T created = factory.get();
            cachedArtifacts.put(key, created);
            return created;
        }

        private boolean matchesLoadedChunks(Level level) {
            for (Entry<ChunkPos, Boolean> entry : loadedChunks.entrySet()) {
                ChunkPos chunk = entry.getKey();
                if (level.hasChunk(chunk.x, chunk.z) != entry.getValue()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class TopologySnapshotCache {
        private final Map<BlockPos, Snapshot> byPipe = new HashMap<>();

        private void install(Snapshot snapshot) {
            Set<Snapshot> displaced = Collections.newSetFromMap(new IdentityHashMap<>());
            for (BlockPos pipePos : snapshot.pipePositions()) {
                Snapshot previous = byPipe.get(pipePos);
                if (previous == null || previous == snapshot) {
                    continue;
                }

                displaced.add(previous);
            }
            for (Snapshot previous : displaced) {
                remove(previous);
            }
            for (BlockPos pipePos : snapshot.pipePositions()) {
                byPipe.put(pipePos, snapshot);
            }
        }

        private void invalidateAround(BlockPos changedPos) {
            Set<Snapshot> invalidated = Collections.newSetFromMap(new IdentityHashMap<>());
            add(changedPos, invalidated);
            for (Direction direction : Iterate.directions) {
                add(changedPos.relative(direction), invalidated);
            }
            for (Snapshot snapshot : invalidated) {
                remove(snapshot);
            }
        }

        private void add(BlockPos pipePos, Set<Snapshot> snapshots) {
            Snapshot snapshot = byPipe.get(pipePos);
            if (snapshot == null) {
                return;
            }

            snapshots.add(snapshot);
        }

        private void remove(Snapshot snapshot) {
            for (BlockPos pipePos : snapshot.pipePositions()) {
                byPipe.remove(pipePos, snapshot);
            }
        }
    }
}
