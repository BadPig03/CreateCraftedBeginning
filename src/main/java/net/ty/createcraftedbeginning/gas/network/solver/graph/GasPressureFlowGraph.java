package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.Linearization;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PipeSegment;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureFlowGraph {
    static final double ACTIVE_SET_PRESSURE_EPSILON_PA = 1.0E-9;

    final List<PressureNode> nodes = new ArrayList<>();
    final List<FlowEdge> edges = new ArrayList<>();
    final List<List<Integer>> incidentEdges = new ArrayList<>();
    private final GasNonlinearPressureSolver solver = new GasNonlinearPressureSolver(this);
    private int staticNodeCount = -1;
    private int staticEdgeCount = -1;
    private int[] staticIncidentEdgeCounts = {};

    public List<PressureNode> getNodes() {
        return List.copyOf(nodes);
    }

    public List<FlowEdge> getEdges() {
        return List.copyOf(edges);
    }

    public int[] getIncidentEdgeCounts() {
        return incidentEdges.stream().mapToInt(List::size).toArray();
    }

    public double getNodePressure(int nodeIndex) {
        return nodes.get(nodeIndex).pressurePa;
    }

    public void setNodePressure(int nodeIndex, double pressurePa) {
        nodes.get(nodeIndex).pressurePa = pressurePa;
    }

    @Internal
    public int addUnknownNode(double initialPressurePa) {
        int index = nodes.size();
        nodes.add(new PressureNode(false, initialPressurePa));
        incidentEdges.add(new ArrayList<>());
        return index;
    }

    @Internal
    public int addFixedNode(double pressurePa) {
        int index = nodes.size();
        nodes.add(new PressureNode(true, Math.max(0, pressurePa)));
        incidentEdges.add(new ArrayList<>());
        return index;
    }

    @Internal
    public int addEdge(int from, int to, long resistanceUnits, long flowRateLimit, @Nullable PipeSegment pipeSegment) {
        return addEdge(from, to, new GasTransportEdgeProperties(resistanceUnits, GasTransportPressureDrive.pressureBoost(GasPressure.VACUUM_PA), flowRateLimit, GasPressure.VACUUM_PA, Double.NaN, false, false), pipeSegment, null);
    }

    @Internal
    public int addEdge(int from, int to, GasTransportEdgeProperties properties, @Nullable PipeSegment pipeSegment, @Nullable BlockPos flowBudgetPos) {
        int index = edges.size();
        edges.add(new FlowEdge(from, to, properties, pipeSegment, flowBudgetPos));
        incidentEdges.get(from).add(index);
        incidentEdges.get(to).add(index);
        return index;
    }

    @Internal
    public void captureStaticState() {
        staticNodeCount = nodes.size();
        staticEdgeCount = edges.size();
        staticIncidentEdgeCounts = new int[staticNodeCount];
        for (int nodeIndex = 0; nodeIndex < staticNodeCount; nodeIndex++) {
            staticIncidentEdgeCounts[nodeIndex] = incidentEdges.get(nodeIndex).size();
        }
    }

    @Internal
    public void resetDynamicState(double @Nullable [] initialPressures) {
        if (staticNodeCount < 0 || staticEdgeCount < 0) {
            throw new IllegalStateException("Static pressure graph state was not captured.");
        }

        if (edges.size() > staticEdgeCount) {
            edges.subList(staticEdgeCount, edges.size()).clear();
        }
        for (int nodeIndex = 0; nodeIndex < staticNodeCount; nodeIndex++) {
            double initialPressure = initialPressures != null && nodeIndex < initialPressures.length ? initialPressures[nodeIndex] : -1;
            nodes.get(nodeIndex).pressurePa = Double.isFinite(initialPressure) && initialPressure >= 0 ? initialPressure : -1;
            List<Integer> nodeEdges = incidentEdges.get(nodeIndex);
            int staticCount = staticIncidentEdgeCounts[nodeIndex];
            if (nodeEdges.size() <= staticCount) {
                continue;
            }

            nodeEdges.subList(staticCount, nodeEdges.size()).clear();
        }
        if (nodes.size() > staticNodeCount) {
            nodes.subList(staticNodeCount, nodes.size()).clear();
        }
        if (incidentEdges.size() <= staticNodeCount) {
            return;
        }

        incidentEdges.subList(staticNodeCount, incidentEdges.size()).clear();
    }

    @Internal
    public FlowGraphSolution solve() {
        return solver.solve();
    }

    double[] copyStaticPressures() {
        double[] pressures = new double[staticNodeCount];
        for (int nodeIndex = 0; nodeIndex < staticNodeCount; nodeIndex++) {
            pressures[nodeIndex] = nodes.get(nodeIndex).pressurePa;
        }
        return pressures;
    }

    void updateEdgeDynamics(int edgeIndex, GasTransportEdgeProperties properties, long flowRateLimit, @Nullable BlockPos flowBudgetPos) {
        edges.get(edgeIndex).updateDynamics(properties, flowRateLimit, flowBudgetPos);
    }

    boolean[] findPressureAnchoredNodes() {
        boolean[] anchored = new boolean[nodes.size()];
        int[] pending = new int[nodes.size()];
        int pendingCount = 0;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            if (!nodes.get(nodeIndex).fixed) {
                continue;
            }

            anchored[nodeIndex] = true;
            pending[pendingCount++] = nodeIndex;
        }

        for (int cursor = 0; cursor < pendingCount; cursor++) {
            int nodeIndex = pending[cursor];
            for (int edgeIndex : incidentEdges.get(nodeIndex)) {
                FlowEdge edge = edges.get(edgeIndex);
                if (edge.flowRateLimit <= 0) {
                    continue;
                }

                int adjacentNode = edge.from == nodeIndex ? edge.to : edge.from;
                if (anchored[adjacentNode]) {
                    continue;
                }

                anchored[adjacentNode] = true;
                pending[pendingCount++] = adjacentNode;
            }
        }
        return anchored;
    }

    @Internal
    public enum EdgeConstraintState {
        CLOSED,
        LINEAR,
        PRESSURE_LIMITED,
        CAPPED
    }

    static final class ActiveFlowComponents {
        private final int[] parent;
        private final byte[] rank;

        private ActiveFlowComponents(int nodeCount) {
            parent = new int[nodeCount];
            rank = new byte[nodeCount];
            for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
                parent[nodeIndex] = nodeIndex;
            }
        }

        static ActiveFlowComponents from(GasPressureFlowGraph graph, FlowGraphSolution solution) {
            ActiveFlowComponents components = new ActiveFlowComponents(graph.nodes.size());
            for (int edgeIndex = 0; edgeIndex < graph.edges.size(); edgeIndex++) {
                if (solution.flowRate(edgeIndex) <= GasFlowMath.FLOW_RATE_EPSILON) {
                    continue;
                }

                FlowEdge edge = graph.edges.get(edgeIndex);
                components.union(edge.from, edge.to);
            }
            return components;
        }

        int componentOf(int nodeIndex) {
            return find(nodeIndex);
        }

        private int find(int nodeIndex) {
            int root = nodeIndex;
            while (parent[root] != root) {
                root = parent[root];
            }
            while (parent[nodeIndex] != nodeIndex) {
                int next = parent[nodeIndex];
                parent[nodeIndex] = root;
                nodeIndex = next;
            }
            return root;
        }

        private void union(int first, int second) {
            int firstRoot = find(first);
            int secondRoot = find(second);
            if (firstRoot == secondRoot) {
                return;
            }

            if (rank[firstRoot] < rank[secondRoot]) {
                parent[firstRoot] = secondRoot;
                return;
            }

            if (rank[firstRoot] > rank[secondRoot]) {
                parent[secondRoot] = firstRoot;
                return;
            }

            parent[secondRoot] = firstRoot;
            rank[firstRoot]++;
        }
    }

    @Internal
    public static final class PressureNode {
        final boolean fixed;
        double pressurePa;

        private PressureNode(boolean fixed, double pressurePa) {
            this.fixed = fixed;
            this.pressurePa = pressurePa;
        }
    }

    @Internal
    public static final class FlowEdge {
        final int from;
        @Nullable
        final PipeSegment pipeSegment;
        final int to;
        @Nullable BlockPos flowBudgetPos;
        private long resistanceUnits;
        private GasTransportPressureDrive pressureDrive;
        private long flowRateLimit;
        private long flowLimitReferencePressurePa;
        private double conductanceOverride = Double.NaN;

        private FlowEdge(int from, int to, GasTransportEdgeProperties properties, @Nullable PipeSegment pipeSegment, @Nullable BlockPos flowBudgetPos) {
            this.from = from;
            this.to = to;
            this.pipeSegment = pipeSegment;
            updateDynamics(properties, properties.flowRateLimit(), flowBudgetPos);
        }

        @Internal
        public EdgeFlow calculate(List<PressureNode> nodes) {
            if (flowRateLimit <= 0) {
                return EdgeFlow.ZERO;
            }

            double drivePa = drivePa(nodes);
            double conductance = conductancePerPascal();
            if (!Double.isFinite(drivePa) || conductance <= 0 || drivePa <= 0) {
                return EdgeFlow.ZERO;
            }

            double unconstrained = conductance * drivePa;
            if (!Double.isFinite(unconstrained)) {
                return EdgeFlow.actual(actualFlowLimit(nodes));
            }

            return EdgeFlow.actual(Math.min(unconstrained, actualFlowLimit(nodes)));
        }

        @Internal
        public EdgeFlow linearize(List<PressureNode> nodes, EdgeConstraintState constraintState) {
            double limit = flowRateLimit;
            if (limit <= 0 || constraintState == EdgeConstraintState.CLOSED) {
                return EdgeFlow.ZERO;
            }

            if (constraintState == EdgeConstraintState.CAPPED) {
                return EdgeFlow.fixed(limit);
            }

            if (constraintState == EdgeConstraintState.PRESSURE_LIMITED) {
                double pressureLimitConductance = pressureLimitConductancePerPascal();
                if (pressureLimitConductance <= 0) {
                    return EdgeFlow.ZERO;
                }

                double estimatedFlowRate = pressureLimitConductance * Math.max(GasPressure.VACUUM_PA, nodes.get(from).pressurePa);
                return EdgeFlow.linearized(estimatedFlowRate, pressureLimitConductance, 0, 0);
            }

            double conductance = conductancePerPascal();
            if (conductance <= 0) {
                return EdgeFlow.ZERO;
            }

            double drivePa = drivePa(nodes);
            double estimatedFlowRate = Double.isFinite(drivePa) ? Math.min(limit, conductance * Math.max(0, drivePa)) : 0;
            return pressureDriveLinearization(nodes, conductance, estimatedFlowRate);
        }

        @Internal
        public EdgeConstraintState nextConstraintState(List<PressureNode> nodes, EdgeConstraintState currentState) {
            double limit = flowRateLimit;
            if (limit <= 0) {
                return EdgeConstraintState.CLOSED;
            }

            double conductance = conductancePerPascal();
            double drivePa = drivePa(nodes);
            if (conductance <= 0 || !Double.isFinite(drivePa)) {
                return EdgeConstraintState.CLOSED;
            }

            double curveFlowRate = pressureCurveFlowRate(nodes);
            double flowEpsilon = Math.max(GasFlowMath.FLOW_RATE_EPSILON, conductance * ACTIVE_SET_PRESSURE_EPSILON_PA);
            double pressureScaledLimit = hasPressureScaledFlowLimit() ? pressureScaledFlowLimit(nodes) : limit;
            return switch (currentState) {
                case CLOSED -> {
                    if (drivePa > ACTIVE_SET_PRESSURE_EPSILON_PA) {
                        yield EdgeConstraintState.LINEAR;
                    }

                    yield EdgeConstraintState.CLOSED;
                }
                case LINEAR -> {
                    if (drivePa < -ACTIVE_SET_PRESSURE_EPSILON_PA) {
                        yield EdgeConstraintState.CLOSED;
                    }
                    if (hasPressureScaledFlowLimit() && pressureScaledLimit + flowEpsilon < Math.min(curveFlowRate, limit)) {
                        yield EdgeConstraintState.PRESSURE_LIMITED;
                    }
                    if (curveFlowRate > limit + flowEpsilon) {
                        yield EdgeConstraintState.CAPPED;
                    }
                    yield EdgeConstraintState.LINEAR;
                }
                case PRESSURE_LIMITED -> {
                    if (drivePa < -ACTIVE_SET_PRESSURE_EPSILON_PA) {
                        yield EdgeConstraintState.LINEAR;
                    }
                    double rawPressureScaledFlowRate = pressureScaledFlowRate(nodes);
                    if (rawPressureScaledFlowRate >= limit - flowEpsilon && curveFlowRate >= limit - flowEpsilon) {
                        yield EdgeConstraintState.CAPPED;
                    }
                    if (rawPressureScaledFlowRate > curveFlowRate + flowEpsilon) {
                        yield EdgeConstraintState.LINEAR;
                    }
                    yield EdgeConstraintState.PRESSURE_LIMITED;
                }
                case CAPPED -> {
                    if (curveFlowRate < limit - flowEpsilon) {
                        yield EdgeConstraintState.LINEAR;
                    }
                    if (hasPressureScaledFlowLimit() && pressureScaledFlowRate(nodes) < limit - flowEpsilon) {
                        yield EdgeConstraintState.PRESSURE_LIMITED;
                    }
                    yield EdgeConstraintState.CAPPED;
                }
            };
        }

        private void updateDynamics(GasTransportEdgeProperties properties, long flowRateLimit, @Nullable BlockPos flowBudgetPos) {
            resistanceUnits = properties.resistanceUnits();
            pressureDrive = properties.pressureDrive();
            this.flowRateLimit = Math.max(0, flowRateLimit);
            flowLimitReferencePressurePa = properties.flowLimitReferencePressurePa();
            this.flowBudgetPos = flowBudgetPos == null ? null : flowBudgetPos.immutable();
            conductanceOverride = properties.conductancePerPascal();
        }

        private EdgeFlow pressureDriveLinearization(List<PressureNode> nodes, double conductance, double estimatedFlowRate) {
            Linearization linearization = pressureDrive.linearize(nodes.get(from).pressurePa, conductance);
            return EdgeFlow.linearized(estimatedFlowRate, linearization.fromPressureConductance(), linearization.toPressureConductance(), linearization.constantFlowRate());
        }

        private double pressureCurveFlowRate(List<PressureNode> nodes) {
            double conductance = conductancePerPascal();
            double drivePa = drivePa(nodes);
            if (conductance <= 0 || !Double.isFinite(drivePa)) {
                return 0;
            }

            return conductance * Math.max(0, drivePa);
        }

        private double actualFlowLimit(List<PressureNode> nodes) {
            if (!hasPressureScaledFlowLimit()) {
                return flowRateLimit;
            }

            return Math.min(flowRateLimit, pressureScaledFlowLimit(nodes));
        }

        private boolean hasPressureScaledFlowLimit() {
            return flowLimitReferencePressurePa > GasPressure.VACUUM_PA && flowRateLimit != Long.MAX_VALUE;
        }

        private double pressureScaledFlowLimit(List<PressureNode> nodes) {
            return Math.min(flowRateLimit, pressureScaledFlowRate(nodes));
        }

        private double pressureScaledFlowRate(List<PressureNode> nodes) {
            double pressureLimitConductance = pressureLimitConductancePerPascal();
            if (pressureLimitConductance <= 0) {
                return flowRateLimit;
            }

            double inletPressurePa = Math.max(GasPressure.VACUUM_PA, nodes.get(from).pressurePa);
            return pressureLimitConductance * inletPressurePa;
        }

        private double pressureLimitConductancePerPascal() {
            if (!hasPressureScaledFlowLimit()) {
                return Double.NaN;
            }

            return (double) flowRateLimit / flowLimitReferencePressurePa;
        }

        private double drivePa(List<PressureNode> nodes) {
            return pressureDrive.drivePressurePa(nodes.get(from).pressurePa, nodes.get(to).pressurePa);
        }

        private double conductancePerPascal() {
            if (Double.isFinite(conductanceOverride) && conductanceOverride > 0) {
                return conductanceOverride;
            }

            return GasFlowResistance.conductancePerPascal(resistanceUnits);
        }
    }

    @Internal
    public record EdgeFlow(double flowRate, double fromPressureConductance, double toPressureConductance, double constantFlowRate) {
        static final EdgeFlow ZERO = new EdgeFlow(0, 0, 0, 0);

        @Internal
        public static EdgeFlow linearized(double flowRate, double fromPressureConductance, double toPressureConductance, double constantFlowRate) {
            return new EdgeFlow(Math.max(0, flowRate), Math.max(0, fromPressureConductance), Math.max(0, toPressureConductance), Math.max(0, constantFlowRate));
        }

        private static EdgeFlow actual(double flowRate) {
            return new EdgeFlow(Math.max(0, flowRate), 0, 0, 0);
        }

        private static EdgeFlow fixed(double flowRate) {
            double fixedFlowRate = Math.max(0, flowRate);
            return new EdgeFlow(fixedFlowRate, 0, 0, fixedFlowRate);
        }

        double symmetricConductance() {
            if (fromPressureConductance == toPressureConductance) {
                return fromPressureConductance;
            }

            return 0;
        }

        double linearizedFlowRate(double fromPressurePa, double toPressurePa) {
            return constantFlowRate + fromPressureConductance * fromPressurePa - toPressureConductance * toPressurePa;
        }
    }

    @Internal
    public static final class SolverWork {
        int regularizedSolves;
        int regularizedIterations;
        int activeSetIterations;
        int forestDirectSolveAttempts;
        int forestDirectSolveSuccesses;
        int conjugateGradientIterations;
        int biConjugateGradientStabilizedIterations;
        int sorSweeps;

        public int getRegularizedSolves() {
            return regularizedSolves;
        }

        public int getForestDirectSolveAttempts() {
            return forestDirectSolveAttempts;
        }

        public int getConjugateGradientIterations() {
            return conjugateGradientIterations;
        }

        public int getBiConjugateGradientStabilizedIterations() {
            return biConjugateGradientStabilizedIterations;
        }

        public int getSorSweeps() {
            return sorSweeps;
        }
    }

    @Internal
    public record FlowGraphSolution(double[] pressures, double[] flowRates, boolean converged, SolverWork work) {
        @Internal
        public double flowRate(int edgeIndex) {
            if (edgeIndex < 0 || edgeIndex >= flowRates.length) {
                return 0;
            }

            return flowRates[edgeIndex];
        }

        double pressure(int nodeIndex) {
            if (nodeIndex < 0 || nodeIndex >= pressures.length) {
                return 0;
            }

            return pressures[nodeIndex];
        }
    }
}
