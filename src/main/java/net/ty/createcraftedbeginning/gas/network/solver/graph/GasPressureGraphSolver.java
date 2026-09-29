package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.Edge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.ActiveFlowComponents;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.SolverWork;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.EndpointFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.FacePressureSamples;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PipeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PipeSegment;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphTemplate.AdjacentEdgeTemplate;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphTemplate.ManifoldSpokeTemplate;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphTemplate.PortKey;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphTemplate.TransportEdgeTemplate;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureGraphSolver {
    private static final int MAX_PREPARED_GRAPHS_PER_TOPOLOGY = 16;
    private static final long ENDPOINT_RESISTANCE_UNITS = GasFlowResistance.UNITS_PER_STANDARD_SEGMENT / 50;
    private static final long JUNCTION_RESISTANCE_UNITS = GasFlowResistance.UNITS_PER_STANDARD_SEGMENT / 1000;
    private static final Object NETWORK_TEMPLATE_CACHE_KEY = new Object();
    private static final Object PREPARED_GRAPH_CACHE_KEY = new Object();

    private GasPressureGraphSolver() {
    }

    public static PreparedGraph prepare(Level level, Snapshot topology, GasStack gas) {
        GasPressureGraphTemplate template = topology.cachedArtifact(NETWORK_TEMPLATE_CACHE_KEY, () -> GasPressureGraphTemplate.create(level, topology));
        PreparedGraphCache cache = topology.cachedArtifact(PREPARED_GRAPH_CACHE_KEY, PreparedGraphCache::new);
        return cache.get(level, template, gas);
    }

    private static final class PreparedGraphCache {
        private final Map<GasStack, PreparedGraph> byGas = new LinkedHashMap<>(MAX_PREPARED_GRAPHS_PER_TOPOLOGY, 0.75F, true);

        private PreparedGraph get(Level level, GasPressureGraphTemplate template, GasStack gas) {
            GasStack key = gas.copyWithAmount(1);
            PreparedGraph cached = byGas.get(key);
            if (cached != null) {
                return cached;
            }

            PreparedGraph created = new PreparedGraph(level, template, key);
            byGas.put(key, created);
            if (byGas.size() <= MAX_PREPARED_GRAPHS_PER_TOPOLOGY) {
                return created;
            }

            GasStack eldest = byGas.keySet().iterator().next();
            byGas.remove(eldest);
            return created;
        }
    }

    public static final class PreparedGraph {
        private final Level level;
        private final GasStack gas;
        private final GasPressureGraphTemplate template;
        private final GasPressureFlowGraph graph;
        private final List<DynamicTransportEdgeBinding> dynamicTransportEdges = new ArrayList<>();
        private double @Nullable [] warmStartPressures;

        private PreparedGraph(Level level, GasPressureGraphTemplate template, GasStack gas) {
            this.level = level;
            this.gas = gas.copyWithAmount(1);
            this.template = template;
            graph = new GasPressureFlowGraph();
            compileStaticGraph();
            graph.captureStaticState();
        }

        public GasPressureGraphSolution solve(List<GasNetworkPressureEndpoint> endpoints, GasTransportFlowBudget transportBudget) {
            return solve(endpoints, transportBudget, true);
        }

        public GasPressureGraphSolution solve(List<GasNetworkPressureEndpoint> endpoints, GasTransportFlowBudget transportBudget, boolean collectFacePressures) {
            return solve(endpoints, transportBudget, true, collectFacePressures, false);
        }

        public GasPressureGraphSolution solveHypothetical(List<GasNetworkPressureEndpoint> endpoints, GasTransportFlowBudget transportBudget) {
            return solve(endpoints, transportBudget, false, false, true);
        }

        private static double warmStartPressurePa(double[] initialPressures, int nodeIndex) {
            if (nodeIndex < 0 || nodeIndex >= initialPressures.length) {
                return -1;
            }

            return initialPressures[nodeIndex];
        }

        private GasPressureGraphSolution solve(List<GasNetworkPressureEndpoint> endpoints, GasTransportFlowBudget transportBudget, boolean updateWarmStart, boolean collectFacePressures, boolean hypothetical) {
            boolean profiling = GasSolverProfiler.isProfilingCurrentNetwork();
            long graphSetupStart = profiling ? System.nanoTime() : 0;
            graph.resetDynamicState(warmStartPressures);
            refreshDynamicTransportEdges(transportBudget);

            List<EndpointNodeBinding> terminals = new ArrayList<>();
            for (GasNetworkPressureEndpoint endpoint : endpoints) {
                EndpointNodeBinding terminal = addEndpointNode(endpoint);
                if (terminal == null) {
                    continue;
                }

                terminals.add(terminal);
            }
            boolean[] pressureAnchoredNodes = collectFacePressures ? graph.findPressureAnchoredNodes() : null;
            if (profiling) {
                GasSolverProfiler.recordGraphSetup(System.nanoTime() - graphSetupStart);
            }

            long graphSolveStart = profiling ? System.nanoTime() : 0;
            FlowGraphSolution graphSolution = graph.solve();
            if (profiling) {
                SolverWork work = graphSolution.work();
                GasSolverProfiler.recordRegularizationWork(work.regularizedSolves, work.regularizedIterations);
                GasSolverProfiler.recordGraphSolve(System.nanoTime() - graphSolveStart, graph.nodes.size(), graph.edges.size(), hypothetical, graphSolution.converged(), work.activeSetIterations, work.forestDirectSolveAttempts, work.forestDirectSolveSuccesses, work.conjugateGradientIterations, work.biConjugateGradientStabilizedIterations, work.sorSweeps);
            }
            if (!graphSolution.converged()) {
                return GasPressureGraphSolution.FAILED;
            }

            long stageStart = profiling ? System.nanoTime() : 0;
            if (updateWarmStart) {
                warmStartPressures = graph.copyStaticPressures();
            }

            long warmStartCopyNanos = profiling ? System.nanoTime() - stageStart : 0;
            stageStart = profiling ? System.nanoTime() : 0;
            ActiveFlowComponents activeComponents = ActiveFlowComponents.from(graph, graphSolution);
            long activeComponentsNanos = profiling ? System.nanoTime() - stageStart : 0;

            stageStart = profiling ? System.nanoTime() : 0;
            Map<Integer, MutableTransferComponent> components = new LinkedHashMap<>();
            for (EndpointNodeBinding terminal : terminals) {
                double drainFlowRate = terminal.drainEdgeIndex < 0 ? 0 : graphSolution.flowRate(terminal.drainEdgeIndex);
                double fillFlowRate = terminal.fillEdgeIndex < 0 ? 0 : graphSolution.flowRate(terminal.fillEdgeIndex);
                double netDrainFlowRate = Math.max(0, drainFlowRate - fillFlowRate);
                double netFillFlowRate = Math.max(0, fillFlowRate - drainFlowRate);
                if (netDrainFlowRate <= GasFlowMath.FLOW_RATE_EPSILON && netFillFlowRate <= GasFlowMath.FLOW_RATE_EPSILON) {
                    continue;
                }

                int componentId = activeComponents.componentOf(terminal.manifoldNode);
                MutableTransferComponent component = components.computeIfAbsent(componentId, ignored -> new MutableTransferComponent());
                component.addEndpointFlow(new EndpointFlow(terminal.endpoint, netDrainFlowRate, netFillFlowRate, graphSolution.pressure(terminal.manifoldNode)));
                component.endpointNodes.put(terminal.endpoint, terminal.manifoldNode);
            }

            long endpointFlowsNanos = profiling ? System.nanoTime() - stageStart : 0;
            stageStart = profiling ? System.nanoTime() : 0;
            FacePressureSamples facePressures = pressureAnchoredNodes == null ? FacePressureSamples.EMPTY : resolveFacePressures(endpoints, pressureAnchoredNodes, graphSolution);
            long facePressuresNanos = profiling ? System.nanoTime() - stageStart : 0;
            stageStart = profiling ? System.nanoTime() : 0;

            Set<Integer> endpointEdges = new HashSet<>();
            for (EndpointNodeBinding terminal : terminals) {
                endpointEdges.add(terminal.drainEdgeIndex);
                endpointEdges.add(terminal.fillEdgeIndex);
            }
            for (int edgeIndex = 0; edgeIndex < graph.edges.size(); edgeIndex++) {
                double flow = graphSolution.flowRate(edgeIndex);
                if (flow <= GasFlowMath.FLOW_RATE_EPSILON) {
                    continue;
                }

                FlowEdge edge = graph.edges.get(edgeIndex);
                int componentId = activeComponents.componentOf(edge.from);
                MutableTransferComponent component = components.get(componentId);
                if (component == null) {
                    continue;
                }

                if (!endpointEdges.contains(edgeIndex)) {
                    component.routingEdges.add(new Edge(edge.from, edge.to, flow, edge.pipeSegment, edge.flowBudgetPos));
                }

                if (edge.pipeSegment != null) {
                    component.pipeFlows.add(new PipeFlow(edge.pipeSegment, flow));
                }
                if (edge.flowBudgetPos == null) {
                    continue;
                }

                component.budgetedFlowRates.merge(edge.flowBudgetPos, flow, Double::sum);
            }

            long edgeFlowsNanos = profiling ? System.nanoTime() - stageStart : 0;
            stageStart = profiling ? System.nanoTime() : 0;
            List<TransferComponent> transferComponents = new ArrayList<>();
            for (MutableTransferComponent component : components.values()) {
                TransferComponent solvedComponent = component.freeze();
                if (solvedComponent.isEmpty()) {
                    continue;
                }

                transferComponents.add(solvedComponent);
            }

            GasPressureGraphSolution result = new GasPressureGraphSolution(List.copyOf(transferComponents), facePressures);
            long materializationNanos = profiling ? System.nanoTime() - stageStart : 0;
            if (profiling) {
                GasSolverProfiler.recordGraphPostSolve(warmStartCopyNanos, activeComponentsNanos, endpointFlowsNanos, facePressuresNanos, edgeFlowsNanos, materializationNanos);
            }
            return result;
        }

        private void compileStaticGraph() {
            for (int i = 0; i < template.staticNodeCount(); i++) {
                graph.addUnknownNode(-1);
            }

            for (TransportEdgeTemplate edge : template.transportEdges()) {
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, edge.pipePos());
                if (behaviour == null) {
                    continue;
                }

                BlockState state = level.getBlockState(edge.pipePos());
                if (!behaviour.acceptsInboundGas(state, edge.entryFace(), gas) || !behaviour.allowsOutboundGas(state, edge.exitFace(), gas)) {
                    continue;
                }

                GasTransportEdgeProperties properties = behaviour.getTransportEdgeProperties(state, edge.entryFace(), edge.exitFace(), gas);
                PipeSegment segment = PipeSegment.direct(edge.pipePos(), edge.entryFace(), edge.exitFace());
                int edgeIndex = graph.addEdge(edge.fromNode(), edge.toNode(), properties, segment, properties.sharedFlowBudget() ? edge.pipePos() : null);
                if (!properties.dynamic() && !properties.sharedFlowBudget()) {
                    continue;
                }

                dynamicTransportEdges.add(new DynamicTransportEdgeBinding(edgeIndex, edge.pipePos(), edge.entryFace(), edge.exitFace()));
            }

            for (ManifoldSpokeTemplate spoke : template.manifoldSpokes()) {
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, spoke.pipePos());
                if (behaviour == null) {
                    continue;
                }

                BlockState state = level.getBlockState(spoke.pipePos());
                long resistance = Math.max(GasFlowResistance.MIN_RESISTANCE_UNITS, behaviour.getManifoldSpokeResistanceUnits(state, spoke.face()));
                if (behaviour.acceptsInboundGas(state, spoke.face(), gas)) {
                    graph.addEdge(spoke.portNode(), spoke.manifoldNode(), resistance, Long.MAX_VALUE, PipeSegment.inboundSpoke(spoke.pipePos(), spoke.face()));
                }
                if (!behaviour.allowsOutboundGas(state, spoke.face(), gas)) {
                    continue;
                }

                graph.addEdge(spoke.manifoldNode(), spoke.portNode(), resistance, Long.MAX_VALUE, PipeSegment.outboundSpoke(spoke.pipePos(), spoke.face()));
            }

            for (AdjacentEdgeTemplate edge : template.adjacentEdges()) {
                GasTransportBehaviour sourceBehaviour = GasConnectionResolver.getTransportBehaviour(level, edge.source().pos());
                GasTransportBehaviour targetBehaviour = GasConnectionResolver.getTransportBehaviour(level, edge.target().pos());
                if (sourceBehaviour == null || targetBehaviour == null) {
                    continue;
                }

                BlockState sourceState = level.getBlockState(edge.source().pos());
                BlockState targetState = level.getBlockState(edge.target().pos());
                if (!sourceBehaviour.allowsOutboundGas(sourceState, edge.source().face(), gas) || !targetBehaviour.acceptsInboundGas(targetState, edge.target().face(), gas)) {
                    continue;
                }

                graph.addEdge(edge.fromNode(), edge.toNode(), JUNCTION_RESISTANCE_UNITS, Long.MAX_VALUE, null);
            }
        }

        private void refreshDynamicTransportEdges(GasTransportFlowBudget transportBudget) {
            for (DynamicTransportEdgeBinding binding : dynamicTransportEdges) {
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, binding.pipePos);
                if (behaviour == null) {
                    graph.updateEdgeDynamics(binding.edgeIndex, GasTransportEdgeProperties.passive(GasFlowResistance.MIN_RESISTANCE_UNITS), 0, null);
                    continue;
                }

                BlockState state = level.getBlockState(binding.pipePos);
                GasTransportEdgeProperties properties = behaviour.getTransportEdgeProperties(state, binding.entryFace, binding.exitFace, gas);
                long flowRateLimit = properties.flowRateLimit();
                BlockPos flowBudgetPos = null;
                if (properties.sharedFlowBudget()) {
                    flowBudgetPos = binding.pipePos;
                    if (transportBudget.remaining(flowBudgetPos, flowRateLimit) <= 0) {
                        flowRateLimit = 0;
                    }
                }
                graph.updateEdgeDynamics(binding.edgeIndex, properties, flowRateLimit, flowBudgetPos);
            }
        }

        private FacePressureSamples resolveFacePressures(List<GasNetworkPressureEndpoint> endpoints, boolean[] pressureAnchoredNodes, FlowGraphSolution graphSolution) {
            double[] observedPressures = new double[template.ports().size()];
            Arrays.fill(observedPressures, Double.NaN);

            for (int portIndex = 0; portIndex < template.ports().size(); portIndex++) {
                if (!pressureAnchoredNodes[portIndex]) {
                    continue;
                }

                observedPressures[portIndex] = graphSolution.pressure(portIndex);
            }

            for (GasNetworkPressureEndpoint endpoint : endpoints) {
                observeEndpointPressure(observedPressures, endpoint.access().drainFaces(), endpoint.pressureState().pressurePa());
                observeEndpointPressure(observedPressures, endpoint.access().fillFaces(), endpoint.pressureState().pressurePa());
            }

            for (AdjacentEdgeTemplate edge : template.adjacentEdges()) {
                if (!template.observablePressurePorts()[edge.fromNode()] || !template.observablePressurePorts()[edge.toNode()]) {
                    continue;
                }

                double fromPressure = observedPressures[edge.fromNode()];
                double toPressure = observedPressures[edge.toNode()];
                if (Double.isFinite(fromPressure) && !Double.isFinite(toPressure)) {
                    observedPressures[edge.toNode()] = fromPressure;
                    continue;
                }

                if (!Double.isFinite(toPressure) || Double.isFinite(fromPressure)) {
                    continue;
                }

                observedPressures[edge.fromNode()] = toPressure;
            }

            return new FacePressureSamples(template.pressureLayout(), observedPressures);
        }

        private void observeEndpointPressure(double[] observedPressures, List<BlockFace> faces, double pressurePa) {
            if (!Double.isFinite(pressurePa) || pressurePa < GasPressure.VACUUM_PA) {
                return;
            }

            for (BlockFace face : faces) {
                Integer portNode = template.portNodes().get(new PortKey(face.getPos(), face.getFace()));
                if (portNode == null || !template.observablePressurePorts()[portNode] || Double.isFinite(observedPressures[portNode])) {
                    continue;
                }

                observedPressures[portNode] = pressurePa;
            }
        }

        @Nullable
        private EndpointNodeBinding addEndpointNode(GasNetworkPressureEndpoint endpoint) {
            List<Integer> drainPortNodes = new ArrayList<>();
            if (endpoint.transferLimits().drainLimit() > 0) {
                for (BlockFace face : endpoint.access().drainFaces()) {
                    BlockPos pos = face.getPos();
                    Direction direction = face.getFace();
                    Integer portNode = template.portNodes().get(new PortKey(pos, direction));
                    GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pos);
                    if (portNode == null || behaviour == null) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (!behaviour.acceptsInboundGas(state, direction, gas)) {
                        continue;
                    }

                    drainPortNodes.add(portNode);
                }
            }

            List<Integer> fillPortNodes = new ArrayList<>();
            if (endpoint.transferLimits().fillLimit() > 0) {
                for (BlockFace face : endpoint.access().fillFaces()) {
                    BlockPos pos = face.getPos();
                    Direction direction = face.getFace();
                    Integer portNode = template.portNodes().get(new PortKey(pos, direction));
                    GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pos);
                    if (portNode == null || behaviour == null) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (!behaviour.allowsOutboundGas(state, direction, gas)) {
                        continue;
                    }

                    fillPortNodes.add(portNode);
                }
            }

            if (drainPortNodes.isEmpty() && fillPortNodes.isEmpty()) {
                return null;
            }

            PressureState pressureState = endpoint.pressureState();
            int fixedNode = graph.addFixedNode(pressureState.pressurePa());
            int manifoldNode = graph.addUnknownNode(endpointManifoldInitialPressurePa(endpoint, drainPortNodes, fillPortNodes));
            for (int portNode : drainPortNodes) {
                graph.addEdge(manifoldNode, portNode, JUNCTION_RESISTANCE_UNITS, Long.MAX_VALUE, null);
            }
            for (int portNode : fillPortNodes) {
                graph.addEdge(portNode, manifoldNode, JUNCTION_RESISTANCE_UNITS, Long.MAX_VALUE, null);
            }
            long drainFlowRateLimit = pressureState.fixedPressure() ? endpoint.transferLimits().drainLimit() : Long.MAX_VALUE;
            long fillFlowRateLimit = pressureState.fixedPressure() ? endpoint.transferLimits().fillLimit() : Long.MAX_VALUE;
            long drainResistanceUnits = BoundedMath.saturatedAdd(ENDPOINT_RESISTANCE_UNITS, endpoint.flowResistance().drainResistanceUnits());
            long fillResistanceUnits = BoundedMath.saturatedAdd(ENDPOINT_RESISTANCE_UNITS, endpoint.flowResistance().fillResistanceUnits());
            int drainEdge = drainPortNodes.isEmpty() ? -1 : graph.addEdge(fixedNode, manifoldNode, drainResistanceUnits, drainFlowRateLimit, null);
            int fillEdge = fillPortNodes.isEmpty() ? -1 : graph.addEdge(manifoldNode, fixedNode, fillResistanceUnits, fillFlowRateLimit, null);
            return new EndpointNodeBinding(endpoint, manifoldNode, drainEdge, fillEdge);
        }

        private double endpointManifoldInitialPressurePa(GasNetworkPressureEndpoint endpoint, List<Integer> drainPortNodes, List<Integer> fillPortNodes) {
            PressureState pressureState = endpoint.pressureState();
            double[] initialPressures = warmStartPressures;
            if (initialPressures == null) {
                return pressureState.pressurePa();
            }

            double junctionConductance = GasFlowResistance.conductancePerPascal(JUNCTION_RESISTANCE_UNITS);
            double drainEndpointConductance = GasFlowResistance.conductancePerPascal(BoundedMath.saturatedAdd(ENDPOINT_RESISTANCE_UNITS, endpoint.flowResistance().drainResistanceUnits()));
            double fillEndpointConductance = GasFlowResistance.conductancePerPascal(BoundedMath.saturatedAdd(ENDPOINT_RESISTANCE_UNITS, endpoint.flowResistance().fillResistanceUnits()));
            double weightedPressureSum = 0;
            double totalConductance = 0;
            if (!drainPortNodes.isEmpty()) {
                weightedPressureSum += pressureState.pressurePa() * drainEndpointConductance;
                totalConductance += drainEndpointConductance;
            }
            if (!fillPortNodes.isEmpty()) {
                weightedPressureSum += pressureState.pressurePa() * fillEndpointConductance;
                totalConductance += fillEndpointConductance;
            }

            for (int portNode : drainPortNodes) {
                double pressure = warmStartPressurePa(initialPressures, portNode);
                if (!Double.isFinite(pressure) || pressure < 0) {
                    continue;
                }

                weightedPressureSum += pressure * junctionConductance;
                totalConductance += junctionConductance;
            }
            for (int portNode : fillPortNodes) {
                double pressure = warmStartPressurePa(initialPressures, portNode);
                if (!Double.isFinite(pressure) || pressure < 0) {
                    continue;
                }

                weightedPressureSum += pressure * junctionConductance;
                totalConductance += junctionConductance;
            }

            if (totalConductance > 0) {
                return weightedPressureSum / totalConductance;
            }

            return pressureState.pressurePa();
        }
    }

    private record DynamicTransportEdgeBinding(int edgeIndex, BlockPos pipePos, Direction entryFace, Direction exitFace) {
        private DynamicTransportEdgeBinding {
            pipePos = pipePos.immutable();
        }
    }

    private record EndpointNodeBinding(GasNetworkPressureEndpoint endpoint, int manifoldNode, int drainEdgeIndex, int fillEdgeIndex) {}

    private static final class MutableTransferComponent {
        private final List<EndpointFlow> endpointFlows = new ArrayList<>();
        private final List<PipeFlow> pipeFlows = new ArrayList<>();
        private final Map<BlockPos, Double> budgetedFlowRates = new HashMap<>();
        private final Map<GasNetworkPressureEndpoint, Integer> endpointNodes = new IdentityHashMap<>();
        private final List<Edge> routingEdges = new ArrayList<>();
        private double totalDrainFlowRate;
        private double totalFillFlowRate;

        private void addEndpointFlow(EndpointFlow flow) {
            endpointFlows.add(flow);
            totalDrainFlowRate += flow.drainFlowRate();
            totalFillFlowRate += flow.fillFlowRate();
        }

        private TransferComponent freeze() {
            double referenceFlowRate = Math.min(totalDrainFlowRate, totalFillFlowRate);
            return new TransferComponent(referenceFlowRate, endpointFlows, pipeFlows, budgetedFlowRates, new GasFlowRouting(endpointNodes, routingEdges));
        }
    }

}
