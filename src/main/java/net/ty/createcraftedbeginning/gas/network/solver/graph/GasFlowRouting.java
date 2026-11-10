package net.ty.createcraftedbeginning.gas.network.solver.graph;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.OutletPressureTarget;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.PressureBoost;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PipeSegment;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedFill;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFlowRouting {
    private static final double AMOUNT_ROUNDING_EPSILON = 1.0E-7;
    private final Map<GasNetworkPressureEndpoint, Integer> endpointNodes = new IdentityHashMap<>();
    private final List<Edge> edges;
    private final int[] fromNodes;
    private final int[] toNodes;
    private final double[] netOutflow;

    public GasFlowRouting(Map<GasNetworkPressureEndpoint, Integer> terminals, List<Edge> edges) {
        Int2IntMap indices = new Int2IntOpenHashMap(edges.size() + terminals.size());
        for (Edge edge : edges) {
            indices.putIfAbsent(edge.from(), indices.size());
            indices.putIfAbsent(edge.to(), indices.size());
        }
        for (Entry<GasNetworkPressureEndpoint, Integer> terminal : terminals.entrySet()) {
            int node = terminal.getValue();
            indices.putIfAbsent(node, indices.size());
            endpointNodes.put(terminal.getKey(), indices.get(node));
        }
        this.edges = List.copyOf(edges);
        fromNodes = new int[edges.size()];
        toNodes = new int[edges.size()];
        netOutflow = new double[indices.size()];
        for (int index = 0; index < edges.size(); index++) {
            Edge edge = edges.get(index);
            int from = indices.get(edge.from());
            int to = indices.get(edge.to());
            fromNodes[index] = from;
            toNodes[index] = to;
            double flowRate = edge.flowRate();
            netOutflow[from] += flowRate;
            netOutflow[to] -= flowRate;
        }
    }

    public RoutedPlan route(GasTransferPlan plan, GasTransportFlowBudget budget) {
        double scale = 0;
        for (PlannedDrain drain : plan.drains()) {
            Integer node = endpointNodes.get(drain.endpoint());
            if (node == null || netOutflow[node] <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            scale = Math.max(scale, drain.amount() / netOutflow[node]);
        }
        for (PlannedFill fill : plan.fills()) {
            Integer node = endpointNodes.get(fill.endpoint());
            if (node == null || -netOutflow[node] <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            scale = Math.max(scale, fill.amount() / -netOutflow[node]);
        }
        IntegerRouting routing = new IntegerRouting(netOutflow.length + 2, edges.size() + plan.drains().size() + plan.fills().size(), budget);
        for (int index = 0; index < edges.size(); index++) {
            Edge edge = edges.get(index);
            long capacity = GasFlowMath.toWholeAmount(Math.ceil(edge.flowRate() * scale - AMOUNT_ROUNDING_EPSILON));
            routing.add(fromNodes[index], toNodes[index], capacity, edge.budgetPos());
        }
        int source = netOutflow.length;
        int target = source + 1;
        Int2ObjectMap<PlannedDrain> drains = new Int2ObjectOpenHashMap<>(plan.drains().size());
        Int2ObjectMap<PlannedFill> fills = new Int2ObjectOpenHashMap<>(plan.fills().size());
        for (PlannedDrain drain : plan.drains()) {
            Integer node = endpointNodes.get(drain.endpoint());
            if (node == null || netOutflow[node] <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            drains.put(routing.add(source, node, drain.amount(), null), drain);
        }
        for (PlannedFill fill : plan.fills()) {
            Integer node = endpointNodes.get(fill.endpoint());
            if (node == null || -netOutflow[node] <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            fills.put(routing.add(node, target, fill.amount(), null), fill);
        }
        routing.allocate(source, target, Math.max(0, plan.totalAmount()));
        List<Route> routes = new ArrayList<>();
        long total = 0;
        int pathLength;
        while ((pathLength = routing.findPath(source, target, true, Set.of())) > 0) {
            long amount = Long.MAX_VALUE;
            List<PipeSegment> segments = new ArrayList<>();
            List<GasTransportPressureDrive> pressureDrives = new ArrayList<>();
            Map<BlockPos, Integer> visits = new HashMap<>();
            for (int step = 0; step < pathLength; step++) {
                int index = routing.path[step];
                amount = Math.min(amount, routing.flows[index]);
                if (index / 2 >= edges.size()) {
                    continue;
                }

                Edge edge = edges.get(index / 2);
                PipeSegment segment = edge.segment();
                if (segment != null) {
                    segments.add(segment);
                }
                GasTransportPressureDrive drive = edge.pressureDrive();
                if (!(drive instanceof PressureBoost boost) || boost.pressureBoostPa() > 0) {
                    pressureDrives.add(drive);
                }
                if (edge.budgetPos() == null) {
                    continue;
                }

                visits.merge(edge.budgetPos(), 1, Integer::sum);
            }
            PlannedDrain drain = drains.get(routing.path[0]);
            PlannedFill fill = fills.get(routing.path[pathLength - 1]);
            for (int step = 0; step < pathLength; step++) {
                routing.flows[routing.path[step]] -= amount;
            }
            routes.add(new Route(drain.endpoint(), fill.endpoint(), fill.sourcePressurePa(), amount, segments, visits, pressureDrives));
            total += amount;
        }
        return new RoutedPlan(routes, total);
    }

    private static final class IntegerRouting {
        private final int[] destinations;
        private final long[] capacities;
        private final long[] flows;
        private final @Nullable BlockPos[] budgetPositions;
        private final int[] firstEdges;
        private final int[] lastEdges;
        private final int[] nextEdges;
        private final int[] previous;
        private final int[] queue;
        private final int[] path;
        private final GasTransportFlowBudget budget;
        private final Map<BlockPos, Long> reserved = new HashMap<>();
        private int edgeCount;

        private IntegerRouting(int nodeCount, int pairCount, GasTransportFlowBudget budget) {
            int capacity = pairCount * 2;
            destinations = new int[capacity];
            capacities = new long[capacity];
            flows = new long[capacity];
            budgetPositions = new BlockPos[pairCount];
            firstEdges = new int[nodeCount];
            Arrays.fill(firstEdges, -1);
            lastEdges = new int[nodeCount];
            nextEdges = new int[capacity];
            previous = new int[nodeCount];
            queue = new int[nodeCount];
            path = new int[nodeCount];
            this.budget = budget;
        }

        private int add(int from, int to, long capacity, @Nullable BlockPos budgetPos) {
            int index = edgeCount;
            capacities[index] = Math.max(0, capacity);
            budgetPositions[index / 2] = budgetPos;
            appendEdge(index, from, to);
            appendEdge(index + 1, to, from);
            edgeCount += 2;
            return index;
        }

        private void appendEdge(int index, int from, int to) {
            destinations[index] = to;
            nextEdges[index] = -1;
            if (firstEdges[from] < 0) {
                firstEdges[from] = index;
            }
            else {
                nextEdges[lastEdges[from]] = index;
            }
            lastEdges[from] = index;
        }

        private void allocate(int source, int target, long remaining) {
            Set<BlockPos> blocked = new HashSet<>();
            while (remaining > 0) {
                int pathLength = findPath(source, target, false, blocked);
                if (pathLength == 0) {
                    return;
                }

                long amount = remaining;
                Map<BlockPos, Integer> visits = new HashMap<>();
                for (int step = 0; step < pathLength; step++) {
                    int index = path[step];
                    amount = Math.min(amount, capacities[index] - flows[index]);
                    BlockPos budgetPos = budgetPositions[index / 2];
                    if (budgetPos == null) {
                        continue;
                    }

                    visits.merge(budgetPos, (index & 1) == 0 ? 1 : -1, Integer::sum);
                }
                for (Entry<BlockPos, Integer> entry : visits.entrySet()) {
                    int count = entry.getValue();
                    if (count <= 0) {
                        continue;
                    }

                    BlockPos pos = entry.getKey();
                    long available = (budget.remainingKnown(pos) - reserved.getOrDefault(pos, 0L)) / count;
                    amount = Math.min(amount, available);
                    if (available > 0) {
                        continue;
                    }

                    blocked.add(pos);
                }
                if (amount <= 0) {
                    continue;
                }

                for (int step = 0; step < pathLength; step++) {
                    int index = path[step];
                    flows[index] += amount;
                    flows[index ^ 1] -= amount;
                }
                for (Entry<BlockPos, Integer> entry : visits.entrySet()) {
                    reserved.merge(entry.getKey(), amount * entry.getValue(), Long::sum);
                }
                remaining -= amount;
                blocked.clear();
            }
        }

        private int findPath(int source, int target, boolean positiveFlow, Set<BlockPos> blocked) {
            Arrays.fill(previous, -1);
            int head = 0;
            int tail = 0;
            queue[tail++] = source;
            previous[source] = -2;
            while (head < tail && previous[target] < 0) {
                int node = queue[head++];
                for (int index = firstEdges[node]; index >= 0; index = nextEdges[index]) {
                    int to = destinations[index];
                    long available = positiveFlow ? flows[index] : capacities[index] - flows[index];
                    if (previous[to] != -1 || available <= 0 || positiveFlow && (index & 1) != 0) {
                        continue;
                    }
                    if (!positiveFlow && (index & 1) == 0 && blocked.contains(budgetPositions[index / 2])) {
                        continue;
                    }

                    previous[to] = index;
                    queue[tail++] = to;
                }
            }
            if (previous[target] < 0) {
                return 0;
            }

            int pathLength = 0;
            int node = target;
            while (node != source) {
                int index = previous[node];
                path[pathLength++] = index;
                node = destinations[index ^ 1];
            }
            for (int first = 0, last = pathLength - 1; first < last; first++, last--) {
                int index = path[first];
                path[first] = path[last];
                path[last] = index;
            }
            return pathLength;
        }
    }

    public record Edge(int from, int to, double flowRate, @Nullable PipeSegment segment, @Nullable BlockPos budgetPos, GasTransportPressureDrive pressureDrive) {}

    public record RoutedPlan(List<Route> routes, long totalAmount) {
        public RoutedPlan { routes = List.copyOf(routes); }
    }

    public record Route(GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long sourcePressurePa, long amount, List<PipeSegment> segments, Map<BlockPos, Integer> budgetVisits, List<GasTransportPressureDrive> pressureDrives) {
        public Route {
            segments = List.copyOf(segments);
            budgetVisits = Map.copyOf(budgetVisits);
            pressureDrives = List.copyOf(pressureDrives);
        }

        public boolean allowsRegulatedPressure(double predictedSourcePressurePa, double predictedTargetPressurePa) {
            double pressureLimitPa = predictedSourcePressurePa;
            boolean regulated = false;
            for (GasTransportPressureDrive drive : pressureDrives) {
                regulated |= drive instanceof OutletPressureTarget;
                pressureLimitPa = drive.drivenOutletPressurePa(pressureLimitPa);
            }
            return !regulated || predictedTargetPressurePa <= pressureLimitPa;
        }

        public long availableAmount(GasTransportFlowBudget budget) {
            long available = amount;
            for (Entry<BlockPos,Integer> entry : budgetVisits.entrySet()) {
                available = Math.min(available, budget.remainingKnown(entry.getKey()) / entry.getValue());
            }
            return available;
        }

        public GasTransferPlan transferPlan(long transferAmount) {
            return new GasTransferPlan(List.of(new PlannedDrain(source, transferAmount)), List.of(new PlannedFill(target, transferAmount, sourcePressurePa)), transferAmount);
        }
    }
}
