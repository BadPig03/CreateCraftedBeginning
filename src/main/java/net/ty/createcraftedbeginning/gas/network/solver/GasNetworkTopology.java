package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.solver.GasPipeLinkIndex.Links;
import net.ty.createcraftedbeginning.gas.telemetry.GasFlowTelemetryTarget;
import net.ty.createcraftedbeginning.gas.telemetry.GasPressureTelemetryTarget;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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

    public static @Nullable FlowState readFlow(Level level, BlockPos pos, Direction side, GasPipeConnection connection) {
        TopologySnapshotCache cache = CACHES.get(level);
        Snapshot snapshot = cache.byPipe.get(pos);
        if ((snapshot == null || snapshot.scheduler == null) && cache.retirementTick == level.getGameTime()) {
            snapshot = cache.retiredThisTick.get(pos);
        }
        if (snapshot != null && snapshot.scheduler != null && snapshot.scheduler.hasZoneFlows()) {
            return snapshot.scheduler.zoneFlow(pos, side);
        }
        return connection.getFlowState();
    }

    public static Snapshot get(Level level, BlockPos startPos) {
        TopologySnapshotCache cache = CACHES.get(level);
        Snapshot cached = cache.byPipe.get(startPos);
        if (cached != null && !cached.invalidateChangedChunks(level, cache)) {
            GasSolverProfiler.recordTopologyCacheHit();
            return cached;
        }

        GasSolverProfiler.recordTopologyCacheMiss();
        return cache.rebuild(level, startPos);
    }

    public static void invalidate(Level level, BlockPos changedPos) {
        if (level.isClientSide) {
            return;
        }

        CACHES.get(level).invalidateAround(changedPos, level.getGameTime());
    }

    public static void invalidateChunk(Level level, ChunkPos chunk) {
        if (level.isClientSide) {
            return;
        }

        CACHES.get(level).invalidateChunk(level, chunk);
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

    static boolean alreadySimulated(Level level, BlockPos pos, long tick) {
        TopologySnapshotCache cache = CACHES.get(level);
        cache.advance(tick);
        Snapshot topology = cache.byPipe.get(pos);
        return topology != null && topology.state.simulatedAt(tick) || cache.retiredThisTick.containsKey(pos);
    }

    record PressureGauge(BlockPos pos, int connectedFaces) {}

    public static final class Snapshot {
        private final GasNetworkState state;
        private @Nullable GasTransferScheduler scheduler;
        private @Nullable GasShadowComparison shadowOwner;
        private @Nullable GasTransferScheduler shadow;
        private long lastShadowTick;
        private final Set<BlockPos> pipePositions;
        private final List<BlockPos> meteredPipes;
        private final List<PressureGauge> pressureGauges;
        private final List<BlockFace> atmosphericFaces;
        private final List<BlockFace> endpointFaces;
        private final Map<ChunkPos, Boolean> loadedChunks;
        private final Map<Object, Object> cachedArtifacts = new IdentityHashMap<>();

        private Snapshot(GasNetworkState state, Set<BlockPos> pipePositions, List<BlockPos> meteredPipes, List<PressureGauge> pressureGauges, List<BlockFace> atmosphericFaces, List<BlockFace> endpointFaces, Map<ChunkPos, Boolean> loadedChunks) {
            this.state = state;
            this.pipePositions = pipePositions;
            this.meteredPipes = meteredPipes;
            this.pressureGauges = pressureGauges;
            this.atmosphericFaces = atmosphericFaces;
            this.endpointFaces = endpointFaces;
            this.loadedChunks = loadedChunks;
        }

        boolean hasContinuousShadow(GasShadowComparison comparison, long tick) {
            return shadowOwner == comparison && lastShadowTick == tick - 1;
        }

        GasTransferScheduler shadow(Level level, GasShadowComparison comparison, long tick, boolean cold) {
            if (cold || shadow == null) {
                comparison.attach(this);
                GasZoneLayout layout = new GasZoneLayout(this, comparison.capacities);
                shadow = new GasTransferScheduler(this, comparison.capacities, comparison.work, comparison.quantum, layout, comparison.order(level), true);
                shadowOwner = comparison;
            }
            lastShadowTick = tick;
            return shadow;
        }

        void releaseShadow(GasShadowComparison comparison) {
            if (shadowOwner != comparison) {
                return;
            }

            shadow = null;
            shadowOwner = null;
        }

        boolean ownsShadow(GasTransferScheduler candidate) {
            return shadow == candidate;
        }

        void selectCapacityScheduler(Map<BlockPos, Long> limits, int workLimit, long quantumLimit) {
            scheduler = new GasTransferScheduler(this, limits, workLimit, quantumLimit);
        }

        void selectPressureScheduler(Level level, Map<BlockPos, Long> limits, int workLimit, long quantumLimit) {
            GasZoneLayout layout = new GasZoneLayout(level, this, limits);
            clearPressureTelemetry(level);
            scheduler = new GasTransferScheduler(this, limits, workLimit, quantumLimit, layout, null, false);
        }

        GasTransferScheduler activateBatch(Level level) {
            GasZoneLayout layout = new GasZoneLayout(this, Map.of());
            scheduler = new GasTransferScheduler(this, Map.of(), GasTransferScheduler.WORK_PER_TICK, GasTransferScheduler.GU_PER_TICK, layout, CACHES.get(level).serviceOrder, false);
            return scheduler;
        }

        @Nullable
        GasTransferScheduler scheduler() {
            return scheduler;
        }

        public GasNetworkState state() {
            return state;
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

        List<BlockPos> meteredPipes() {
            return meteredPipes;
        }

        List<PressureGauge> pressureGauges() {
            return pressureGauges;
        }

        void clearPressureTelemetry(Level level) {
            for (PressureGauge gauge : pressureGauges) {
                if (!(level.getBlockEntity(gauge.pos()) instanceof GasPressureTelemetryTarget target)) {
                    continue;
                }

                target.clearPressureTelemetry();
            }
        }

        private boolean invalidateChangedChunks(Level level, TopologySnapshotCache cache) {
            boolean changed = false;
            for (Entry<ChunkPos, Boolean> entry : loadedChunks.entrySet()) {
                ChunkPos chunk = entry.getKey();
                if (level.hasChunk(chunk.x, chunk.z) == entry.getValue()) {
                    continue;
                }

                cache.invalidateChunk(level, chunk);
                changed = true;
            }
            return changed;
        }
    }

    private static final class TopologySnapshotCache {
        private final Map<BlockPos, Snapshot> byPipe = new HashMap<>();
        private final GasServiceOrder serviceOrder = new GasServiceOrder();
        private final GasPipeLinkIndex links = new GasPipeLinkIndex();
        private final Map<BlockPos, Snapshot> retiredThisTick = new HashMap<>();
        private long nextVersion;
        private long retirementTick = Long.MIN_VALUE;

        private Snapshot rebuild(Level level, BlockPos startPos) {
            long tick = level.getGameTime();
            advance(tick);
            Deque<BlockPos> frontier = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            Map<BlockPos, Links> members = new HashMap<>();
            List<BlockFace> endpoints = new ArrayList<>();
            List<BlockFace> atmosphere = new ArrayList<>();
            List<BlockPos> meteredPipes = new ArrayList<>();
            List<PressureGauge> pressureGauges = new ArrayList<>();
            Map<ChunkPos, Boolean> chunks = new HashMap<>();
            BlockPos start = startPos.immutable();
            ChunkPos firstChunk = new ChunkPos(start);
            if (chunks.computeIfAbsent(firstChunk, chunk -> level.hasChunk(chunk.x, chunk.z))) {
                frontier.add(start);
                visited.add(start);
            }

            boolean simulated = false;
            while (!frontier.isEmpty()) {
                BlockPos pos = frontier.removeFirst();
                Snapshot displaced = byPipe.get(pos);
                if (displaced != null) {
                    remove(displaced, tick);
                }
                simulated |= retiredThisTick.containsKey(pos);
                Links node = links.resolve(level, pos, chunks);
                if (node == null) {
                    continue;
                }

                members.put(pos, node);
                BlockEntity entity = level.getBlockEntity(pos);
                if (node.ordinary() && entity instanceof GasFlowTelemetryTarget) {
                    meteredPipes.add(pos);
                }
                if (entity instanceof GasPressureTelemetryTarget) {
                    pressureGauges.add(new PressureGauge(pos, node.neighbors() | node.endpoints() | node.atmosphere()));
                }
                for (Direction side : Iterate.directions) {
                    int bit = 1 << side.ordinal();
                    if ((node.endpoints() & bit) != 0) {
                        endpoints.add(new BlockFace(pos, side));
                    }
                    if ((node.atmosphere() & bit) != 0) {
                        atmosphere.add(new BlockFace(pos, side));
                    }
                    if ((node.neighbors() & bit) == 0) {
                        continue;
                    }

                    BlockPos neighbor = pos.relative(side);
                    if (!visited.add(neighbor)) {
                        continue;
                    }

                    frontier.addLast(neighbor);
                }
            }
            endpoints.sort(GasNetworkTopology::compareBlockFaces);
            atmosphere.sort(GasNetworkTopology::compareBlockFaces);
            Set<BlockPos> positions = Set.copyOf(visited);
            GasNetworkState state = new GasNetworkState(++nextVersion, positions, members, simulated ? tick : Long.MIN_VALUE);
            Snapshot snapshot = new Snapshot(state, positions, List.copyOf(meteredPipes), List.copyOf(pressureGauges), List.copyOf(atmosphere), List.copyOf(endpoints), Map.copyOf(chunks));
            for (BlockPos pos : positions) {
                byPipe.put(pos, snapshot);
            }
            return snapshot;
        }

        private void advance(long tick) {
            if (retirementTick == tick) {
                return;
            }

            retirementTick = tick;
            retiredThisTick.clear();
        }

        private void invalidateAround(BlockPos pos, long tick) {
            invalidatePipe(pos, tick);
            for (Direction side : Iterate.directions) {
                invalidatePipe(pos.relative(side), tick);
            }
        }

        private void invalidateChunk(Level level, ChunkPos chunk) {
            long tick = level.getGameTime();
            for (BlockPos pos : links.touching(chunk)) {
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pos);
                if (behaviour == null) {
                    invalidateAround(pos, tick);
                    continue;
                }

                behaviour.markConnectionsDirty();
            }
        }

        private void invalidatePipe(BlockPos pos, long tick) {
            links.forget(pos);
            Snapshot snapshot = byPipe.get(pos);
            if (snapshot == null) {
                return;
            }

            remove(snapshot, tick);
        }

        private void remove(Snapshot snapshot, long tick) {
            advance(tick);
            boolean simulated = snapshot.state.simulatedAt(tick);
            snapshot.state.retire();
            for (BlockPos pos : snapshot.pipePositions) {
                if (simulated) {
                    retiredThisTick.putIfAbsent(pos, snapshot);
                }
                byPipe.remove(pos, snapshot);
            }
        }
    }
}
