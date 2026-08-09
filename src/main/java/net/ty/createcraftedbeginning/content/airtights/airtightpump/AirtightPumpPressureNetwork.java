package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator.AdjacentTarget;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPumpPressureNetwork {
    private static final int DIRECTION_COUNT = Direction.values().length;

    private AirtightPumpPressureNetwork() {
    }

    static boolean distributePressureTo(AirtightPumpBlockEntity pump, Direction side) {
        Level level = pump.getLevel();
        if (!pump.isPumpRunning() || level == null) {
            return false;
        }

        boolean pull = AirtightPumpPressureController.isPullingOnSide(pump.isFront(side));
        Direction entryFace = side.getOpposite();
        BlockPos pumpPos = pump.getBlockPos();
        BlockPos startPos = pumpPos.relative(side);
        if (!level.isLoaded(startPos)) {
            return false;
        }

        if (!pull) {
            GasPropagator.resetAffectedNetworks(level, pumpPos, entryFace);
        }
        AdjacentTarget startTarget = GasPropagator.resolveAdjacentTarget(level, pumpPos, side);
        if (hasReachedValidEndpoint(level, startTarget, pull)) {
            return true;
        }

        PressureGraph graph = buildPressureGraph(level, startPos, entryFace, pull);
        if (graph.endpointParents.isEmpty() || !markReachableStates(graph)) {
            return false;
        }

        applyPressureGraph(level, pumpPos, graph, pull, pump.getPumpPressure());
        return true;
    }

    private static boolean hasReachedValidEndpoint(Level level, AdjacentTarget target, boolean pull) {
        if (target.isAlignedPump() && level.getBlockEntity(target.pos()) instanceof AirtightPumpBlockEntity pump) {
            return pump.isPumpRunning() && AirtightPumpPressureController.isPullingOnSide(pump.isFront(target.connectedFace())) != pull;
        }
        return !target.canFlowToward() && (target.hasGasCapability() || target.isOpenEnded());
    }

    private static PressureGraph buildPressureGraph(Level level, BlockPos startPos, Direction entryFace, boolean pull) {
        PressureGraph graph = new PressureGraph(startPos, entryFace);
        if (!level.isLoaded(startPos)) {
            return graph;
        }

        GasTransportBehaviour startPipe = GasPropagator.getBehaviour(level, startPos);
        if (startPipe == null || startPipe instanceof AirtightPumpTransportBehaviour) {
            return graph;
        }

        int maxDistance = GasPropagator.getAirtightPumpMaxRange();
        Deque<PressureNode> frontier = new ArrayDeque<>();
        graph.start.searchQueued = true;
        frontier.addLast(graph.start);

        while (!frontier.isEmpty()) {
            PressureNode current = frontier.removeFirst();
            current.searchQueued = false;
            if (current.expanded || !level.isLoaded(current.pos)) {
                continue;
            }

            current.expanded = true;
            BlockState currentState = level.getBlockState(current.pos);
            GasTransportBehaviour currentPipe = GasPropagator.getBehaviour(level, current.pos);
            if (currentPipe == null || currentPipe instanceof AirtightPumpTransportBehaviour) {
                continue;
            }

            boolean canFlowFromEntry = pull ? currentPipe.allowsOutboundFlow(currentState, current.entryFace) : currentPipe.allowsInboundFlow(currentState, current.entryFace);
            if (!canFlowFromEntry) {
                continue;
            }

            for (Direction face : Iterate.directions) {
                if (face == current.entryFace || current.pos.equals(startPos) && face == entryFace) {
                    continue;
                }

                boolean canFlowToExit = pull ? currentPipe.allowsInboundFlow(currentState, face) : currentPipe.allowsOutboundFlow(currentState, face);
                if (!canFlowToExit) {
                    continue;
                }

                BlockPos connectedPos = current.pos.relative(face);
                if (!level.isLoaded(connectedPos)) {
                    continue;
                }

                AdjacentTarget target = GasPropagator.resolveAdjacentTarget(level, current.pos, face);
                if (hasReachedValidEndpoint(level, target, pull)) {
                    current.addEdge(new FlowEdge(face, pull, null));
                    graph.markEndpointParent(current);
                    continue;
                }

                GasTransportBehaviour connectedPipe = target.behaviour();
                if (connectedPipe == null || connectedPipe instanceof AirtightPumpTransportBehaviour) {
                    continue;
                }

                BlockState connectedState = target.state();
                Direction connectedFace = target.connectedFace();
                boolean canCrossBoundary = pull ? connectedPipe.allowsOutboundFlow(connectedState, connectedFace) : connectedPipe.allowsInboundFlow(connectedState, connectedFace);
                if (!canCrossBoundary) {
                    continue;
                }

                int nextDistance = current.distance + 1;
                if (nextDistance > maxDistance) {
                    continue;
                }

                PressureNode next = graph.getOrCreate(connectedPos, connectedFace, nextDistance);
                if (next.distance != nextDistance) {
                    continue;
                }

                current.addEdge(new FlowEdge(face, pull, next));
                next.addParent(current);
                if (next.expanded || next.searchQueued) {
                    continue;
                }

                next.searchQueued = true;
                frontier.addLast(next);
            }
        }

        return graph;
    }

    private static boolean markReachableStates(PressureGraph graph) {
        Deque<PressureNode> frontier = new ArrayDeque<>();
        for (PressureNode endpointParent : graph.endpointParents) {
            if (endpointParent.reachable) {
                continue;
            }

            endpointParent.reachable = true;
            frontier.addLast(endpointParent);
        }

        while (!frontier.isEmpty()) {
            PressureNode current = frontier.removeFirst();
            if (current.parents == null) {
                continue;
            }

            for (PressureNode parent : current.parents) {
                if (parent.reachable) {
                    continue;
                }

                parent.reachable = true;
                frontier.addLast(parent);
            }
        }
        return graph.start.reachable;
    }

    private static void applyPressureGraph(Level level, BlockPos pumpPos, PressureGraph graph, boolean pull, float pressure) {
        Deque<PressureNode> frontier = new ArrayDeque<>();
        graph.start.pendingPressure = pressure;
        graph.start.pressureQueued = true;
        frontier.addLast(graph.start);

        while (!frontier.isEmpty()) {
            PressureNode current = frontier.removeFirst();
            current.pressureQueued = false;
            if (!current.reachable || current.pressureApplied || current.edges == null) {
                continue;
            }

            current.pressureApplied = true;
            int validEdgeCount = 0;
            for (FlowEdge edge : current.edges) {
                if (edge.target == null || edge.target.reachable) {
                    validEdgeCount++;
                }
            }
            if (validEdgeCount == 0) {
                continue;
            }

            addPressureToPipe(level, pumpPos, current.pos, current.entryFace, !pull, current.pendingPressure);
            float branchPressure = current.pendingPressure / validEdgeCount;
            for (FlowEdge edge : current.edges) {
                if (edge.target != null && !edge.target.reachable) {
                    continue;
                }

                addPressureToPipe(level, pumpPos, current.pos, edge.face, edge.inbound, branchPressure);
                if (edge.target == null || edge.target.pressureApplied) {
                    continue;
                }

                edge.target.pendingPressure += branchPressure;
                if (edge.target.pressureQueued) {
                    continue;
                }

                edge.target.pressureQueued = true;
                frontier.addLast(edge.target);
            }
        }
    }

    private static void addPressureToPipe(Level level, BlockPos pumpPos, BlockPos pipePos, Direction pipeSide, boolean inbound, float pressure) {
        if (pipePos.equals(pumpPos) || pressure <= 0) {
            return;
        }

        GasTransportBehaviour transport = GasPropagator.getBehaviour(level, pipePos);
        if (transport == null) {
            return;
        }

        transport.addPressure(pipeSide, inbound, pressure);
    }

    private static final class PressureGraph {
        private final Map<BlockPos, PressureNode[]> nodesByPos = new HashMap<>();
        private final List<PressureNode> endpointParents = new ArrayList<>();
        private final PressureNode start;

        private PressureGraph(BlockPos startPos, Direction entryFace) {
            start = getOrCreate(startPos, entryFace, 1);
        }

        private PressureNode getOrCreate(BlockPos pos, Direction entryFace, int distance) {
            PressureNode[] nodes = nodesByPos.computeIfAbsent(pos, ignored -> new PressureNode[DIRECTION_COUNT]);
            int index = entryFace.ordinal();
            PressureNode node = nodes[index];
            if (node != null) {
                return node;
            }

            node = new PressureNode(pos, entryFace, distance);
            nodes[index] = node;
            return node;
        }

        private void markEndpointParent(PressureNode node) {
            if (node.endpointParent) {
                return;
            }

            node.endpointParent = true;
            endpointParents.add(node);
        }
    }

    private static final class PressureNode {
        private final BlockPos pos;
        private final Direction entryFace;
        private final int distance;
        @Nullable
        private List<FlowEdge> edges;
        @Nullable
        private List<PressureNode> parents;
        private boolean searchQueued;
        private boolean expanded;
        private boolean endpointParent;
        private boolean reachable;
        private boolean pressureQueued;
        private boolean pressureApplied;
        private float pendingPressure;

        private PressureNode(BlockPos pos, Direction entryFace, int distance) {
            this.pos = pos;
            this.entryFace = entryFace;
            this.distance = distance;
        }

        private void addEdge(FlowEdge edge) {
            if (edges == null) {
                edges = new ArrayList<>();
            }
            edges.add(edge);
        }

        private void addParent(PressureNode parent) {
            if (parents == null) {
                parents = new ArrayList<>();
            }
            parents.add(parent);
        }
    }

    private record FlowEdge(Direction face, boolean inbound, @Nullable PressureNode target) {}
}
