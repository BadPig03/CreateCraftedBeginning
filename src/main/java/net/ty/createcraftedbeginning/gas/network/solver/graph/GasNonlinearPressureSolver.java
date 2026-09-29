package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasLinearPressureSolver.Result;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeConstraintState;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.PressureNode;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.SolverWork;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasNonlinearPressureSolver {
    private static final int MAX_ACTIVE_SET_ITERATIONS = 16;
    private static final int MAX_REGULARIZED_ITERATIONS = 128;
    private static final int MAX_REGULARIZATION_ATTEMPTS = 12;
    private static final double INITIAL_REGULARIZATION = 1.0E-4;
    private static final double MIN_REGULARIZATION = 1.0E-12;

    private final GasPressureFlowGraph graph;
    private final List<PressureNode> nodes;
    private final List<FlowEdge> edges;
    private final List<List<Integer>> incidentEdges;
    private final GasPressureResiduals residuals;
    private final GasLinearPressureSolver linearSolver;
    private final GasDensePressureSolver denseSolver;

    GasNonlinearPressureSolver(GasPressureFlowGraph graph) {
        this.graph = graph;
        nodes = graph.nodes;
        edges = graph.edges;
        incidentEdges = graph.incidentEdges;
        residuals = new GasPressureResiduals(graph);
        linearSolver = new GasLinearPressureSolver(graph, residuals);
        denseSolver = new GasDensePressureSolver(graph);
    }

    private static boolean sameLinearization(EdgeFlow[] first, EdgeFlow[] second) {
        for (int edgeIndex = 0; edgeIndex < first.length; edgeIndex++) {
            EdgeFlow firstFlow = first[edgeIndex];
            EdgeFlow secondFlow = second[edgeIndex];
            if (firstFlow.fromPressureConductance() != secondFlow.fromPressureConductance() || firstFlow.toPressureConductance() != secondFlow.toPressureConductance() || firstFlow.constantFlowRate() != secondFlow.constantFlowRate()) {
                return false;
            }
        }
        return true;
    }

    FlowGraphSolution solve() {
        SolverWork work = new SolverWork();
        if (nodes.isEmpty() || edges.isEmpty()) {
            return new FlowGraphSolution(new double[nodes.size()], new double[edges.size()], true, work);
        }

        double defaultPressure = GasPressure.VACUUM_PA;
        for (PressureNode node : nodes) {
            if (node.fixed || Double.isFinite(node.pressurePa) && node.pressurePa >= 0) {
                continue;
            }

            node.pressurePa = defaultPressure;
        }

        EdgeConstraintState[] constraintStates = new EdgeConstraintState[edges.size()];
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            constraintStates[edgeIndex] = edges.get(edgeIndex).nextConstraintState(nodes, EdgeConstraintState.LINEAR);
        }
        EdgeFlow[] finalFlows = calculateActualFlows();
        List<EdgeFlow[]> attemptedLinearizations = new ArrayList<>();
        boolean converged = residuals.calculateUnknownResidual(finalFlows, false).converged(residuals.flowResidualTolerance(calculateLinearizedFlows(constraintStates)));
        boolean tryEarlyRecovery = true;
        double[] failedRecoveryPressures = null;
        if (!converged && denseSolver.isApplicable() && Arrays.asList(constraintStates).contains(EdgeConstraintState.PRESSURE_LIMITED)) {
            double[] previous = new double[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                previous[i] = nodes.get(i).pressurePa;
            }
            converged = solveRegularizedPressures(defaultPressure, work);
            finalFlows = calculateActualFlows();
            if (!converged) {
                for (int i = 0; i < nodes.size(); i++) {
                    PressureNode node = nodes.get(i);
                    double recoveredPressure = node.pressurePa;
                    node.pressurePa = previous[i];
                    previous[i] = recoveredPressure;
                }
                failedRecoveryPressures = previous;
                tryEarlyRecovery = false;
            }
        }
        for (int activeSetIteration = 0; !converged && activeSetIteration < MAX_ACTIVE_SET_ITERATIONS; activeSetIteration++) {
            work.activeSetIterations++;
            EdgeFlow[] linearizedFlows = calculateLinearizedFlows(constraintStates);
            if (attemptedLinearizations.stream().anyMatch(previous -> sameLinearization(previous, linearizedFlows))) {
                break;
            }

            attemptedLinearizations.add(linearizedFlows);
            if (linearSolver.solve(linearizedFlows, work, tryEarlyRecovery) == Result.INCONSISTENT) {
                double[] previous = new double[nodes.size()];
                for (int i = 0; i < nodes.size(); i++) {
                    previous[i] = nodes.get(i).pressurePa;
                }
                if (solveRegularizedPressures(defaultPressure, work)) {
                    finalFlows = calculateActualFlows();
                    converged = true;
                    break;
                }

                for (int i = 0; i < nodes.size(); i++) {
                    PressureNode node = nodes.get(i);
                    double recoveredPressure = node.pressurePa;
                    node.pressurePa = previous[i];
                    previous[i] = recoveredPressure;
                }
                failedRecoveryPressures = previous;
                tryEarlyRecovery = false;
                linearSolver.solve(linearizedFlows, work, false);
            }
            finalFlows = calculateActualFlows();
            boolean constraintStateChanged = updateConstraintStates(constraintStates);
            double residualTolerance = residuals.flowResidualTolerance(linearizedFlows);
            if (constraintStateChanged || !residuals.calculateUnknownResidual(finalFlows, false).converged(residualTolerance)) {
                continue;
            }

            converged = true;
            break;
        }
        if (!converged) {
            if (failedRecoveryPressures == null) {
                converged = solveRegularizedPressures(defaultPressure, work);
            }
            else {
                for (int i = 0; i < nodes.size(); i++) {
                    nodes.get(i).pressurePa = failedRecoveryPressures[i];
                }
            }
            finalFlows = calculateActualFlows();
        }
        if (!converged) {
            Arrays.fill(finalFlows, EdgeFlow.ZERO);
        }

        double[] pressures = new double[nodes.size()];
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            pressures[nodeIndex] = nodes.get(nodeIndex).pressurePa;
        }
        double[] flowRates = new double[edges.size()];
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            flowRates[edgeIndex] = finalFlows[edgeIndex].flowRate();
        }
        return new FlowGraphSolution(pressures, flowRates, converged, work);
    }

    private boolean solveRegularizedPressures(double initialPressure, SolverWork work) {
        work.regularizedSolves++;
        int nodeCount = nodes.size();
        int edgeCount = edges.size();
        for (PressureNode node : nodes) {
            if (node.fixed) {
                continue;
            }

            node.pressurePa = initialPressure;
        }
        double regularization = INITIAL_REGULARIZATION;
        for (int iteration = 0; iteration < MAX_REGULARIZED_ITERATIONS; iteration++) {
            work.regularizedIterations++;
            work.activeSetIterations++;
            EdgeConstraintState[] states = new EdgeConstraintState[edgeCount];
            for (int i = 0; i < edgeCount; i++) {
                states[i] = edges.get(i).nextConstraintState(nodes, EdgeConstraintState.LINEAR);
            }
            EdgeFlow[] flows = calculateLinearizedFlows(states);
            EdgeFlow[] actualFlows = calculateActualFlows();
            double residual = residuals.nonlinearResidualNorm(actualFlows);
            double tolerance = residuals.flowResidualTolerance(flows);
            if (residuals.calculateUnknownResidual(actualFlows, false).converged(tolerance)) {
                double[] convergedPressures = new double[nodeCount];
                for (int i = 0; i < nodeCount; i++) {
                    convergedPressures[i] = nodes.get(i).pressurePa;
                }
                if (!denseSolver.projectPressures(flows)) {
                    linearSolver.solve(flows, work, false);
                }
                EdgeFlow[] polishedFlows = calculateActualFlows();
                double polishedResidual = residuals.nonlinearResidualNorm(polishedFlows);
                if (!Double.isFinite(polishedResidual) || polishedResidual > residual || !residuals.calculateUnknownResidual(polishedFlows, false).converged(tolerance)) {
                    for (int i = 0; i < nodeCount; i++) {
                        nodes.get(i).pressurePa = convergedPressures[i];
                    }
                }
                return true;
            }

            double[] previous = new double[nodeCount];
            int[] counts = new int[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                previous[i] = nodes.get(i).pressurePa;
                counts[i] = incidentEdges.get(i).size();
            }
            boolean accepted = false;
            for (int attempt = 0; attempt < MAX_REGULARIZATION_ATTEMPTS; attempt++) {
                for (int i = 0; i < nodeCount; i++) {
                    if (nodes.get(i).fixed) {
                        continue;
                    }

                    int anchor = graph.addFixedNode(previous[i]);
                    graph.addEdge(i, anchor, new GasTransportEdgeProperties(GasFlowResistance.MIN_RESISTANCE_UNITS, GasTransportPressureDrive.pressureBoost(0), Long.MAX_VALUE, 0, regularization, false, false), null, null);
                }
                EdgeFlow[] regularized = Arrays.copyOf(flows, edges.size());
                for (int i = edgeCount; i < edges.size(); i++) {
                    regularized[i] = EdgeFlow.linearized(0, regularization, regularization, 0);
                }
                try {
                    if (!denseSolver.projectPressures(regularized)) {
                        linearSolver.solve(regularized, work, false);
                    }
                }
                finally {
                    edges.subList(edgeCount, edges.size()).clear();
                    nodes.subList(nodeCount, nodes.size()).clear();
                    incidentEdges.subList(nodeCount, incidentEdges.size()).clear();
                    for (int i = 0; i < nodeCount; i++) {
                        List<Integer> incident = incidentEdges.get(i);
                        incident.subList(counts[i], incident.size()).clear();
                    }
                }
                double updatedResidual = residuals.nonlinearResidualNorm(calculateActualFlows());
                if (Double.isFinite(updatedResidual) && updatedResidual <= residual + nodeCount * tolerance) {
                    accepted = true;
                    regularization = Math.max(MIN_REGULARIZATION, regularization * 0.25);
                    break;
                }

                for (int i = 0; i < nodeCount; i++) {
                    nodes.get(i).pressurePa = previous[i];
                }
                regularization *= 10;
            }
            if (!accepted) {
                return false;
            }
        }
        return false;
    }

    private EdgeFlow[] calculateLinearizedFlows(EdgeConstraintState[] constraintStates) {
        return calculateFlows(constraintStates);
    }

    private EdgeFlow[] calculateActualFlows() {
        return calculateFlows(null);
    }

    private EdgeFlow[] calculateFlows(EdgeConstraintState @Nullable [] constraintStates) {
        EdgeFlow[] calculated = new EdgeFlow[edges.size()];
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            calculated[edgeIndex] = constraintStates == null ? edge.calculate(nodes) : edge.linearize(nodes, constraintStates[edgeIndex]);
        }
        return calculated;
    }

    private boolean updateConstraintStates(EdgeConstraintState[] constraintStates) {
        boolean changed = false;
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            EdgeConstraintState currentState = constraintStates[edgeIndex];
            EdgeConstraintState nextState = edges.get(edgeIndex).nextConstraintState(nodes, currentState);
            if (nextState == currentState) {
                continue;
            }

            constraintStates[edgeIndex] = nextState;
            changed = true;
        }
        return changed;
    }
}
