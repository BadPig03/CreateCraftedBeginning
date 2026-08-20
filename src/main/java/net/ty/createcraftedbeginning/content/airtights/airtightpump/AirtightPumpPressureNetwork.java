package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPressure;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator.AdjacentTarget;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightPumpPressureNetwork {
    private static final int DIRECTION_COUNT = Direction.values().length;
    private static final PressureTopologyEdge[] NO_TOPOLOGY_EDGES = new PressureTopologyEdge[0];
    private static final WorldAttached<PressureTopologyBatch> PRESSURE_TOPOLOGY_BATCHES = new WorldAttached<>($ -> new PressureTopologyBatch());

    private AirtightPumpPressureNetwork() {
    }

    public static PressureDistributionResult distributePressureTo(AirtightPumpBlockEntity pump, Direction side) {
        Level level = pump.getLevel();
        if (!pump.isPumpRunning() || level == null) {
            return new PressureDistributionResult(false, false);
        }

        boolean pull = AirtightPumpPressureController.isPullingOnSide(pump.isFront(side));
        PressureBuildContext context = PressureBuildContext.forLevel(level, pull);
        Direction entryFace = side.getOpposite();
        BlockPos pumpPos = pump.getBlockPos();
        BlockPos startPos = pumpPos.relative(side);
        if (!context.isLoaded(startPos)) {
            return new PressureDistributionResult(false, true);
        }

        if (!pull) {
            GasPropagator.resetAffectedNetworks(level, pumpPos, entryFace);
        }
        AdjacentTarget startTarget = GasPropagator.resolveAdjacentTarget(level, pumpPos, side);
        if (hasReachedValidEndpoint(level, startTarget, pull)) {
            return new PressureDistributionResult(true, false);
        }

        PressureTraversal traversal = buildPressureTraversal(context, startPos, entryFace);
        boolean validPath = traversal.hasEndpoint && markReachableStates(context, traversal);
        if (validPath) {
            applyPressureTraversal(context, pumpPos, traversal, pump.getPumpPressure());
        }
        return new PressureDistributionResult(validPath, traversal.topologyIncomplete);
    }

    private static boolean hasReachedValidEndpoint(Level level, AdjacentTarget target, boolean pull) {
        if (target.isAlignedPump() && level.getBlockEntity(target.pos()) instanceof AirtightPumpBlockEntity pump) {
            return pump.isPumpRunning() && AirtightPumpPressureController.isPullingOnSide(pump.isFront(target.connectedFace())) != pull;
        }
        return !target.canFlowToward() && (target.hasGasCapability() || target.isOpenEnded());
    }

    private static PressureTraversal buildPressureTraversal(PressureBuildContext context, BlockPos startPos, Direction entryFace) {
        PressurePipeSnapshot startPipe = context.pipe(startPos);
        if (startPipe == null || startPipe.behaviour == null || startPipe.behaviour instanceof AirtightPumpTransportBehaviour) {
            return new PressureTraversal(startPos, entryFace, null);
        }

        PressureTopologyNode start = context.topologyNode(startPipe, entryFace);
        PressureTraversal traversal = new PressureTraversal(startPos, entryFace, start);
        int maxDistance = GasPropagator.getAirtightPumpMaxRange();
        Deque<PressureTopologyNode> frontier = new ArrayDeque<>();
        traversal.visit(start, 1);
        frontier.addLast(start);

        while (!frontier.isEmpty()) {
            PressureTopologyNode current = frontier.removeFirst();
            int currentDistance = traversal.distance(current);
            PressureTopologyEdge[] edges = current.edges(context.topology);
            traversal.topologyIncomplete |= current.hasUnloadedBoundary;
            for (PressureTopologyEdge edge : edges) {
                if (traversal.skipsStartEdge(current, edge)) {
                    continue;
                }

                if (edge.isEndpoint(context.topology.level, context.pull)) {
                    traversal.markEndpoint(current, edge.face);
                    continue;
                }

                PressureTopologyNode next = edge.target;
                if (next == null) {
                    continue;
                }

                int nextDistance = currentDistance + 1;
                if (nextDistance > maxDistance) {
                    continue;
                }

                int knownDistance = traversal.distance(next);
                if (knownDistance != 0) {
                    continue;
                }

                traversal.visit(next, nextDistance);
                frontier.addLast(next);
            }
        }

        return traversal;
    }

    private static boolean markReachableStates(PressureBuildContext context, PressureTraversal traversal) {
        if (traversal.start == null) {
            return false;
        }

        for (int i = traversal.visitedCount - 1; i >= 0; i--) {
            PressureTopologyNode current = traversal.visitedNodes[i];
            boolean reachable = traversal.endpointMask(current) != 0;
            if (!reachable) {
                int nextDistance = traversal.distance(current) + 1;
                for (PressureTopologyEdge edge : current.edges(context.topology)) {
                    if (traversal.skipsStartEdge(current, edge)) {
                        continue;
                    }

                    PressureTopologyNode next = edge.target;
                    if (next == null || traversal.distance(next) != nextDistance || !traversal.isReachable(next)) {
                        continue;
                    }

                    reachable = true;
                    break;
                }
            }
            if (reachable) {
                traversal.markReachable(current);
            }
        }
        return traversal.isReachable(traversal.start);
    }

    private static void applyPressureTraversal(PressureBuildContext context, BlockPos pumpPos, PressureTraversal traversal, float pressure) {
        PressureTopologyNode start = traversal.start;
        long pressureUnits = GasPressure.toUnits(pressure);
        if (start == null || pressureUnits <= 0) {
            return;
        }

        Deque<PressureTopologyNode> frontier = new ArrayDeque<>();
        traversal.addPendingPressure(start, pressureUnits);
        traversal.markPressureQueued(start);
        frontier.addLast(start);

        while (!frontier.isEmpty()) {
            PressureTopologyNode current = frontier.removeFirst();
            traversal.clearPressureQueued(current);
            if (!traversal.isReachable(current) || traversal.isPressureApplied(current)) {
                continue;
            }

            int validEdgeCount = countValidPressureEdges(context, traversal, current);
            if (validEdgeCount == 0) {
                traversal.markPressureApplied(current);
                continue;
            }

            traversal.markPressureApplied(current);
            long currentPressureUnits = traversal.pendingPressure(current);
            addPressureToPipe(context, pumpPos, current.pipe.pos, current.entryFace, !context.pull, currentPressureUnits);

            int validEdgeIndex = 0;
            int endpointMask = traversal.endpointMask(current);
            int nextDistance = traversal.distance(current) + 1;
            for (PressureTopologyEdge edge : current.edges(context.topology)) {
                if (traversal.skipsStartEdge(current, edge)) {
                    continue;
                }

                int faceMask = 1 << edge.face.ordinal();
                boolean endpoint = (endpointMask & faceMask) != 0;
                PressureTopologyNode next = edge.target;
                boolean reachesChild = !endpoint && next != null && traversal.distance(next) == nextDistance && traversal.isReachable(next);
                if (!endpoint && !reachesChild) {
                    continue;
                }

                long branchPressureUnits = GasPressure.splitShare(currentPressureUnits, validEdgeCount, validEdgeIndex++);
                addPressureToPipe(context, pumpPos, current.pipe.pos, edge.face, context.pull, branchPressureUnits);
                if (!reachesChild || traversal.isPressureApplied(next) || branchPressureUnits <= 0) {
                    continue;
                }

                traversal.addPendingPressure(next, branchPressureUnits);
                if (traversal.isPressureQueued(next)) {
                    continue;
                }

                traversal.markPressureQueued(next);
                frontier.addLast(next);
            }
        }
    }

    private static int countValidPressureEdges(PressureBuildContext context, PressureTraversal traversal, PressureTopologyNode current) {
        int validEdgeCount = 0;
        int endpointMask = traversal.endpointMask(current);
        int nextDistance = traversal.distance(current) + 1;
        for (PressureTopologyEdge edge : current.edges(context.topology)) {
            if (traversal.skipsStartEdge(current, edge)) {
                continue;
            }

            int faceMask = 1 << edge.face.ordinal();
            if ((endpointMask & faceMask) != 0) {
                validEdgeCount++;
                continue;
            }

            PressureTopologyNode next = edge.target;
            if (next != null && traversal.distance(next) == nextDistance && traversal.isReachable(next)) {
                validEdgeCount++;
            }
        }
        return validEdgeCount;
    }

    private static void addPressureToPipe(PressureBuildContext context, BlockPos pumpPos, BlockPos pipePos, Direction pipeSide, boolean inbound, long pressureUnits) {
        if (pipePos.equals(pumpPos) || pressureUnits <= 0) {
            return;
        }

        PressurePipeSnapshot pipe = context.pipe(pipePos);
        if (pipe == null || pipe.behaviour == null) {
            return;
        }

        pipe.behaviour.addPressureUnits(pipeSide, inbound, pressureUnits);
    }

    private enum EndpointKind {
        NONE,
        STATIC,
        ALIGNED_PUMP
    }

    public record PressureDistributionResult(boolean validPath, boolean topologyIncomplete) {}

    private record PressureBuildContext(PressureTopologyCache topology, boolean pull) {
        private static PressureBuildContext forLevel(Level level, boolean pull) {
            PressureTopologyBatch batch = PRESSURE_TOPOLOGY_BATCHES.get(level);
            long gameTime = level.getGameTime();
            long revision = GasPropagator.getPressureTopologyRevision(level);
            PressureTopologyCache topology = batch.topology == null ? null : batch.topology.get();
            if (topology == null || batch.gameTime != gameTime || batch.revision != revision) {
                topology = new PressureTopologyCache(level);
                batch.gameTime = gameTime;
                batch.revision = revision;
                batch.topology = new WeakReference<>(topology);
            }
            return new PressureBuildContext(topology, pull);
        }

        private boolean isLoaded(BlockPos pos) {
            return topology.isLoaded(pos);
        }

        @Nullable
        private PressurePipeSnapshot pipe(BlockPos pos) {
            return topology.pipe(pos);
        }

        private PressureTopologyNode topologyNode(PressurePipeSnapshot pipe, Direction entryFace) {
            return PressureTopologyCache.topologyNode(pipe, entryFace, pull);
        }
    }

    private static final class PressureTopologyCache {
        private final Level level;
        private final Map<BlockPos, Boolean> loadedByPos = new HashMap<>();
        private final Map<BlockPos, PressurePipeSnapshot> pipesByPos = new HashMap<>();

        private PressureTopologyCache(Level level) {
            this.level = level;
        }

        private static EndpointKind endpointKind(AdjacentTarget target) {
            if (target.isAlignedPump()) {
                return EndpointKind.ALIGNED_PUMP;
            }
            return !target.canFlowToward() && (target.hasGasCapability() || target.isOpenEnded()) ? EndpointKind.STATIC : EndpointKind.NONE;
        }

        private static boolean allowsEntryFlow(PressurePipeSnapshot pipe, Direction face, boolean pull) {
            return pull ? pipe.allowsOutbound(face) : pipe.allowsInbound(face);
        }

        private static boolean allowsExitFlowWithoutLevel(PressurePipeSnapshot pipe, Direction face, boolean pull) {
            return pull ? pipe.allowsInboundWithoutLevel(face) : pipe.allowsOutboundWithoutLevel(face);
        }

        private static boolean allowsExitFlow(PressurePipeSnapshot pipe, Direction face, boolean pull) {
            return pull ? pipe.allowsInbound(face) : pipe.allowsOutbound(face);
        }

        private static boolean allowsBoundaryFlow(PressurePipeSnapshot pipe, Direction face, boolean pull) {
            return pull ? pipe.allowsOutbound(face) : pipe.allowsInbound(face);
        }

        private static PressureTopologyNode topologyNode(PressurePipeSnapshot pipe, Direction entryFace, boolean pull) {
            PressureTopologyNode[] nodes = pull ? pipe.pullTopologyNodes : pipe.pushTopologyNodes;
            if (nodes == null) {
                nodes = new PressureTopologyNode[DIRECTION_COUNT];
                if (pull) {
                    pipe.pullTopologyNodes = nodes;
                }
                else {
                    pipe.pushTopologyNodes = nodes;
                }
            }

            int index = entryFace.ordinal();
            PressureTopologyNode node = nodes[index];
            if (node != null) {
                return node;
            }

            node = new PressureTopologyNode(pipe, entryFace, pull);
            nodes[index] = node;
            return node;
        }

        private boolean isLoaded(BlockPos pos) {
            BlockPos key = pos.immutable();
            return loadedByPos.computeIfAbsent(key, level::isLoaded);
        }

        @Nullable
        private PressurePipeSnapshot pipe(BlockPos pos) {
            BlockPos key = pos.immutable();
            if (!isLoaded(key)) {
                return null;
            }
            return pipesByPos.computeIfAbsent(key, ignored -> new PressurePipeSnapshot(key, level.getBlockState(key), GasPropagator.getBehaviour(level, key)));
        }

        private PressurePipeSnapshot pipe(AdjacentTarget target) {
            BlockPos key = target.pos().immutable();
            PressurePipeSnapshot cached = pipesByPos.get(key);
            if (cached != null) {
                return cached;
            }

            PressurePipeSnapshot pipe = new PressurePipeSnapshot(key, target.state(), target.behaviour());
            pipesByPos.put(key, pipe);
            loadedByPos.put(key, true);
            return pipe;
        }

        private AdjacentTarget adjacentTarget(PressurePipeSnapshot pipe, Direction face) {
            int index = face.ordinal();
            if (pipe.adjacentTargets != null) {
                AdjacentTarget target = pipe.adjacentTargets[index];
                if (target != null) {
                    return target;
                }
            }
            else {
                pipe.adjacentTargets = new AdjacentTarget[DIRECTION_COUNT];
            }
            AdjacentTarget target = GasPropagator.resolveAdjacentTarget(level, pipe.pos, face);
            pipe.adjacentTargets[index] = target;
            return target;
        }

        private PressureTopologyEdge[] buildEdges(PressureTopologyNode node) {
            PressurePipeSnapshot currentPipe = node.pipe;
            if (!allowsEntryFlow(currentPipe, node.entryFace, node.pull)) {
                return NO_TOPOLOGY_EDGES;
            }

            List<PressureTopologyEdge> edges = null;
            for (Direction face : Iterate.directions) {
                if (face == node.entryFace || !allowsExitFlowWithoutLevel(currentPipe, face, node.pull)) {
                    continue;
                }

                BlockPos connectedPos = currentPipe.pos.relative(face);
                if (!isLoaded(connectedPos)) {
                    node.hasUnloadedBoundary = true;
                    continue;
                }
                if (!allowsExitFlow(currentPipe, face, node.pull)) {
                    continue;
                }

                AdjacentTarget target = adjacentTarget(currentPipe, face);
                EndpointKind endpointKind = endpointKind(target);
                PressureTopologyNode connectedNode = null;
                if (endpointKind == EndpointKind.NONE) {
                    PressurePipeSnapshot connectedPipe = pipe(target);
                    if (connectedPipe.behaviour != null && !(connectedPipe.behaviour instanceof AirtightPumpTransportBehaviour)) {
                        Direction connectedFace = target.connectedFace();
                        if (allowsBoundaryFlow(connectedPipe, connectedFace, node.pull)) {
                            connectedNode = topologyNode(connectedPipe, connectedFace, node.pull);
                        }
                    }
                }

                if (endpointKind == EndpointKind.NONE && connectedNode == null) {
                    continue;
                }
                if (edges == null) {
                    edges = new ArrayList<>();
                }
                edges.add(new PressureTopologyEdge(face, target, endpointKind, connectedNode));
            }

            if (edges == null) {
                return NO_TOPOLOGY_EDGES;
            }
            return edges.toArray(PressureTopologyEdge[]::new);
        }
    }

    private static final class PressureTopologyBatch {
        private long gameTime = Long.MIN_VALUE;
        private long revision = Long.MIN_VALUE;
        @Nullable
        private WeakReference<PressureTopologyCache> topology;
    }

    private static final class PressurePipeSnapshot {
        private final BlockPos pos;
        private final BlockState state;
        @Nullable
        private final GasTransportBehaviour behaviour;
        private int inboundKnownMask;
        private int inboundTrueMask;
        private int outboundKnownMask;
        private int outboundTrueMask;
        @Nullable
        private AdjacentTarget[] adjacentTargets;
        @Nullable
        private PressureTopologyNode[] pushTopologyNodes;
        @Nullable
        private PressureTopologyNode[] pullTopologyNodes;

        private PressurePipeSnapshot(BlockPos pos, BlockState state, @Nullable GasTransportBehaviour behaviour) {
            this.pos = pos;
            this.state = state;
            this.behaviour = behaviour;
        }

        private boolean allowsInboundWithoutLevel(Direction face) {
            return behaviour != null && behaviour.allowsInboundFlowWithoutLevel(state, face);
        }

        private boolean allowsOutboundWithoutLevel(Direction face) {
            return behaviour != null && behaviour.allowsOutboundFlowWithoutLevel(state, face);
        }

        private boolean allowsInbound(Direction face) {
            int mask = 1 << face.ordinal();
            if ((inboundKnownMask & mask) != 0) {
                return (inboundTrueMask & mask) != 0;
            }

            boolean value = behaviour != null && behaviour.allowsInboundFlow(state, face);
            inboundKnownMask |= mask;
            if (value) {
                inboundTrueMask |= mask;
            }
            return value;
        }

        private boolean allowsOutbound(Direction face) {
            int mask = 1 << face.ordinal();
            if ((outboundKnownMask & mask) != 0) {
                return (outboundTrueMask & mask) != 0;
            }

            boolean value = behaviour != null && behaviour.allowsOutboundFlow(state, face);
            outboundKnownMask |= mask;
            if (value) {
                outboundTrueMask |= mask;
            }
            return value;
        }
    }

    private static final class PressureTopologyNode {
        private final PressurePipeSnapshot pipe;
        private final Direction entryFace;
        private final boolean pull;
        private boolean hasUnloadedBoundary;
        @Nullable
        private PressureTopologyEdge[] edges;

        private PressureTopologyNode(PressurePipeSnapshot pipe, Direction entryFace, boolean pull) {
            this.pipe = pipe;
            this.entryFace = entryFace;
            this.pull = pull;
        }

        private PressureTopologyEdge[] edges(PressureTopologyCache topology) {
            if (edges == null) {
                edges = topology.buildEdges(this);
            }
            return edges;
        }
    }

    private record PressureTopologyEdge(Direction face, AdjacentTarget adjacentTarget, EndpointKind endpointKind, @Nullable PressureTopologyNode target) {
        private boolean isEndpoint(Level level, boolean pull) {
            return endpointKind == EndpointKind.STATIC || endpointKind == EndpointKind.ALIGNED_PUMP && level.getBlockEntity(adjacentTarget.pos()) instanceof AirtightPumpBlockEntity pump && pump.isPumpRunning() && AirtightPumpPressureController.isPullingOnSide(pump.isFront(adjacentTarget.connectedFace())) != pull;
        }
    }

    private static final class PressureTraversal {
        private static final byte REACHABLE = 1;
        private static final byte PRESSURE_QUEUED = 2;
        private static final byte PRESSURE_APPLIED = 4;
        private static final int INITIAL_STATE_CAPACITY = 16;
        private static final int INITIAL_INDEX_CAPACITY = 32;

        private final BlockPos startPos;
        private final Direction startEntryFace;
        @Nullable
        private final PressureTopologyNode start;
        private PressureTopologyNode[] visitedNodes = new PressureTopologyNode[INITIAL_STATE_CAPACITY];
        private int[] distances = new int[INITIAL_STATE_CAPACITY];
        private int[] endpointMasks = new int[INITIAL_STATE_CAPACITY];
        private byte[] flags = new byte[INITIAL_STATE_CAPACITY];
        private long[] pendingPressureUnits = new long[INITIAL_STATE_CAPACITY];
        private PressureTopologyNode[] indexKeys = new PressureTopologyNode[INITIAL_INDEX_CAPACITY];
        private int[] indexValues = new int[INITIAL_INDEX_CAPACITY];
        private int visitedCount;
        private int indexSize;
        private boolean hasEndpoint;
        private boolean topologyIncomplete;

        private PressureTraversal(BlockPos startPos, Direction startEntryFace, @Nullable PressureTopologyNode start) {
            this.startPos = startPos;
            this.startEntryFace = startEntryFace;
            this.start = start;
        }

        private static int mixIdentityHash(Object object) {
            int hash = System.identityHashCode(object);
            hash ^= hash >>> 16;
            return hash;
        }

        private void visit(PressureTopologyNode node, int distance) {
            ensureStateCapacity(visitedCount + 1);
            int index = visitedCount++;
            visitedNodes[index] = node;
            distances[index] = distance;
            putIndex(node, index);
        }

        private int distance(PressureTopologyNode node) {
            int index = indexOf(node);
            if (index < 0) {
                return 0;
            }
            return distances[index];
        }

        private void markEndpoint(PressureTopologyNode node, Direction face) {
            endpointMasks[requiredIndex(node)] |= 1 << face.ordinal();
            hasEndpoint = true;
        }

        private int endpointMask(PressureTopologyNode node) {
            int index = indexOf(node);
            if (index < 0) {
                return 0;
            }
            return endpointMasks[index];
        }

        private boolean skipsStartEdge(PressureTopologyNode node, PressureTopologyEdge edge) {
            return node.pipe.pos.equals(startPos) && edge.face == startEntryFace;
        }

        private void markReachable(PressureTopologyNode node) {
            flags[requiredIndex(node)] |= REACHABLE;
        }

        private boolean isReachable(PressureTopologyNode node) {
            int index = indexOf(node);
            return index >= 0 && (flags[index] & REACHABLE) != 0;
        }

        private void markPressureQueued(PressureTopologyNode node) {
            flags[requiredIndex(node)] |= PRESSURE_QUEUED;
        }

        private void clearPressureQueued(PressureTopologyNode node) {
            flags[requiredIndex(node)] &= ~PRESSURE_QUEUED;
        }

        private boolean isPressureQueued(PressureTopologyNode node) {
            return (flags[requiredIndex(node)] & PRESSURE_QUEUED) != 0;
        }

        private void markPressureApplied(PressureTopologyNode node) {
            flags[requiredIndex(node)] |= PRESSURE_APPLIED;
        }

        private boolean isPressureApplied(PressureTopologyNode node) {
            return (flags[requiredIndex(node)] & PRESSURE_APPLIED) != 0;
        }

        private void addPendingPressure(PressureTopologyNode node, long pressureUnits) {
            int index = requiredIndex(node);
            pendingPressureUnits[index] = GasPressure.addSaturated(pendingPressureUnits[index], pressureUnits);
        }

        private long pendingPressure(PressureTopologyNode node) {
            return pendingPressureUnits[requiredIndex(node)];
        }

        private int requiredIndex(PressureTopologyNode node) {
            int index = indexOf(node);
            if (index < 0) {
                throw new IllegalStateException("Pressure topology node was not visited by this traversal");
            }
            return index;
        }

        private int indexOf(PressureTopologyNode node) {
            int mask = indexKeys.length - 1;
            int slot = mixIdentityHash(node) & mask;
            while (true) {
                PressureTopologyNode key = indexKeys[slot];
                if (key == null) {
                    return -1;
                }
                if (key == node) {
                    return indexValues[slot] - 1;
                }
                slot = slot + 1 & mask;
            }
        }

        private void putIndex(PressureTopologyNode node, int index) {
            if ((indexSize + 1) * 3 >= indexKeys.length * 2) {
                resizeIndex(indexKeys.length << 1);
            }

            int mask = indexKeys.length - 1;
            int slot = mixIdentityHash(node) & mask;
            while (indexKeys[slot] != null) {
                slot = slot + 1 & mask;
            }
            indexKeys[slot] = node;
            indexValues[slot] = index + 1;
            indexSize++;
        }

        private void resizeIndex(int newCapacity) {
            PressureTopologyNode[] oldKeys = indexKeys;
            int[] oldValues = indexValues;
            indexKeys = new PressureTopologyNode[newCapacity];
            indexValues = new int[newCapacity];
            int mask = newCapacity - 1;
            for (int i = 0; i < oldKeys.length; i++) {
                PressureTopologyNode key = oldKeys[i];
                if (key == null) {
                    continue;
                }

                int slot = mixIdentityHash(key) & mask;
                while (indexKeys[slot] != null) {
                    slot = slot + 1 & mask;
                }
                indexKeys[slot] = key;
                indexValues[slot] = oldValues[i];
            }
        }

        private void ensureStateCapacity(int requiredCapacity) {
            if (requiredCapacity <= visitedNodes.length) {
                return;
            }

            int newLength = visitedNodes.length;
            while (newLength < requiredCapacity) {
                newLength <<= 1;
            }
            visitedNodes = Arrays.copyOf(visitedNodes, newLength);
            distances = Arrays.copyOf(distances, newLength);
            endpointMasks = Arrays.copyOf(endpointMasks, newLength);
            flags = Arrays.copyOf(flags, newLength);
            pendingPressureUnits = Arrays.copyOf(pendingPressureUnits, newLength);
        }
    }
}
