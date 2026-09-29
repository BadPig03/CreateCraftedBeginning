package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFlowRouting {
    private final Map<GasNetworkPressureEndpoint, Integer> endpointNodes = new IdentityHashMap<>();
    private final List<Edge> edges;
    private final List<List<Integer>> outgoing = new ArrayList<>();
    private final double[] netOutflow;

    public GasFlowRouting(Map<GasNetworkPressureEndpoint, Integer> terminals, List<Edge> edges) {
        Map<Integer, Integer> indices = new HashMap<>();
        for (Edge edge : edges) {
            indices.computeIfAbsent(edge.from(), ignored -> indices.size());
            indices.computeIfAbsent(edge.to(), ignored -> indices.size());
        }
        terminals.forEach((endpoint, node) -> endpointNodes.put(endpoint, indices.computeIfAbsent(node, ignored -> indices.size())));
        netOutflow = new double[indices.size()];
        for (int index = 0; index < indices.size(); index++) {
            outgoing.add(new ArrayList<>());
        }
        List<Edge> compact = new ArrayList<>();
        for (Edge edge : edges) {
            Edge mapped = new Edge(indices.get(edge.from()), indices.get(edge.to()), edge.flowRate(), edge.segment(), edge.budgetPos());
            outgoing.get(mapped.from()).add(compact.size());
            compact.add(mapped);
            netOutflow[mapped.from()] += mapped.flowRate();
            netOutflow[mapped.to()] -= mapped.flowRate();
        }
        this.edges = List.copyOf(compact);
    }

    public RoutedPlan route(GasTransferPlan plan, GasTransportFlowBudget budget) {
        double[] remainingRates = edges.stream().mapToDouble(Edge::flowRate).toArray();
        long[] remainingDrains = plan.drains().stream().mapToLong(PlannedDrain::amount).toArray();
        Map<BlockPos, Long> reserved = new HashMap<>();
        List<Route> routes = new ArrayList<>();
        long remainingTotal = Math.max(0, plan.totalAmount());
        for (PlannedFill fill : plan.fills()) {
            Integer target = endpointNodes.get(fill.endpoint());
            if (target == null || -netOutflow[target] <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            long remainingFill = Math.max(0, fill.amount());
            for (int drainIndex = 0; drainIndex < plan.drains().size() && remainingFill > 0 && remainingTotal > 0; drainIndex++) {
                PlannedDrain drain = plan.drains().get(drainIndex);
                Integer source = endpointNodes.get(drain.endpoint());
                if (source == null || source.equals(target) || netOutflow[source] <= GasFlowMath.FLOW_RATE_EPSILON) {
                    continue;
                }

                double scale = Math.max(drain.amount() / netOutflow[source], fill.amount() / -netOutflow[target]);
                while (remainingDrains[drainIndex] > 0 && remainingFill > 0 && remainingTotal > 0) {
                    List<Integer> path = findPath(source, target, remainingRates, budget, reserved);
                    if (path.isEmpty()) {
                        break;
                    }

                    double rate = Double.POSITIVE_INFINITY;
                    List<PipeSegment> segments = new ArrayList<>();
                    Map<BlockPos, Integer> visits = new HashMap<>();
                    for (int edgeIndex : path) {
                        Edge edge = edges.get(edgeIndex);
                        rate = Math.min(rate, remainingRates[edgeIndex]);
                        if (edge.segment() != null) {
                            segments.add(edge.segment());
                        }
                        if (!(edge.budgetPos() != null)) {
                            continue;
                        }

                        visits.merge(edge.budgetPos(), 1, Integer::sum);
                    }
                    long amount = GasFlowMath.toWholeAmount(Math.max(1, Math.ceil(rate * scale - 1.0E-7)));
                    amount = Math.min(amount, Math.min(remainingTotal, Math.min(remainingDrains[drainIndex], remainingFill)));
                    for (Entry<BlockPos,Integer> entry : visits.entrySet()) {
                        long available = budget.remainingKnown(entry.getKey()) - reserved.getOrDefault(entry.getKey(), 0L);
                        amount = Math.min(amount, available / entry.getValue());
                    }
                    if (amount <= 0) {
                        break;
                    }

                    routes.add(new Route(drain.endpoint(), fill.endpoint(), fill.sourcePressurePa(), amount, segments, visits));
                    for (Entry<BlockPos,Integer> entry : visits.entrySet()) {
                        reserved.merge(entry.getKey(), amount * entry.getValue(), Long::sum);
                    }
                    remainingDrains[drainIndex] -= amount;
                    remainingFill -= amount;
                    remainingTotal -= amount;
                    double usedRate = Math.min(rate, amount / scale);
                    for (int edgeIndex : path) {
                        remainingRates[edgeIndex] = Math.max(0, remainingRates[edgeIndex] - usedRate);
                    }
                }
            }
        }
        return new RoutedPlan(routes, Math.max(0, plan.totalAmount()) - remainingTotal);
    }

    private List<Integer> findPath(int source, int target, double[] remainingRates, GasTransportFlowBudget budget, Map<BlockPos, Long> reserved) {
        int[] previous = new int[outgoing.size()];
        Arrays.fill(previous, -1);
        int[] queue = new int[outgoing.size()];
        int head = 0;
        int tail = 0;
        queue[tail++] = source;
        previous[source] = -2;
        while (head < tail && previous[target] < 0) {
            int node = queue[head++];
            for (int edgeIndex : outgoing.get(node)) {
                Edge edge = edges.get(edgeIndex);
                if (previous[edge.to()] != -1 || remainingRates[edgeIndex] <= GasFlowMath.FLOW_RATE_EPSILON) {
                    continue;
                }

                if (edge.budgetPos() != null && reserved.getOrDefault(edge.budgetPos(), 0L) >= budget.remainingKnown(edge.budgetPos())) {
                    continue;
                }

                previous[edge.to()] = edgeIndex;
                queue[tail++] = edge.to();
            }
        }
        if (previous[target] < 0) {
            return List.of();
        }

        List<Integer> path = new ArrayList<>();
        int node = target;
        while (node != source) {
            int edgeIndex = previous[node];
            path.add(edgeIndex);
            node = edges.get(edgeIndex).from();
        }
        Collections.reverse(path);
        return path;
    }

    public record Edge(int from, int to, double flowRate, @Nullable PipeSegment segment, @Nullable BlockPos budgetPos) {}

    public record RoutedPlan(List<Route> routes, long totalAmount) {
        public RoutedPlan { routes = List.copyOf(routes); }
    }

    public record Route(GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long sourcePressurePa, long amount, List<PipeSegment> segments, Map<BlockPos, Integer> budgetVisits) {
        public Route {
            segments = List.copyOf(segments);
            budgetVisits = Map.copyOf(budgetVisits);
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
