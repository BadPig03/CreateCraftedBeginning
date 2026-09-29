package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.PressureNode;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.SolverWork;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.ACTIVE_SET_PRESSURE_EPSILON_PA;
import static net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureResiduals.FLOW_RESIDUAL_ABSOLUTE_TOLERANCE;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasLinearPressureSolver {
    private static final double CONJUGATE_GRADIENT_BREAKDOWN_EPSILON = 1.0E-30;
    private static final double FOREST_DIRECT_PIVOT_RELATIVE_EPSILON = 1.0E-12;
    private static final double PRESSURE_SOR_FACTOR = 1.5;
    private static final int MAX_ASYMMETRIC_LINEAR_SOLVER_SWEEPS = 4096;
    private static final int MAX_CONJUGATE_GRADIENT_ITERATIONS = 4096;
    private static final int MAX_LINEAR_SOLVER_SWEEPS = 256;
    private static final int MIN_CONJUGATE_GRADIENT_ITERATIONS = 64;

    private final List<PressureNode> nodes;
    private final List<FlowEdge> edges;
    private final List<List<Integer>> incidentEdges;
    private final GasPressureResiduals residuals;
    private boolean inconsistentLinearization;
    private boolean detectInconsistency;

    @Internal
    public enum Result {
        SOLVED,
        UNRESOLVED,
        INCONSISTENT
    }

    @Internal
    public GasLinearPressureSolver(GasPressureFlowGraph graph, GasPressureResiduals residuals) {
        nodes = graph.nodes;
        edges = graph.edges;
        incidentEdges = graph.incidentEdges;
        this.residuals = residuals;
    }

    @Internal
    public Result solve(EdgeFlow[] linearizedFlows, SolverWork work) {
        return solve(linearizedFlows, work, true);
    }

    Result solve(EdgeFlow[] linearizedFlows, SolverWork work, boolean detectInconsistency) {
        this.detectInconsistency = detectInconsistency;
        inconsistentLinearization = false;
        if (containsAsymmetricLinearization(linearizedFlows)) {
            if (solveWithBiConjugateGradientStabilized(linearizedFlows, work)) {
                return Result.SOLVED;
            }

            if (inconsistentLinearization) {
                return Result.INCONSISTENT;
            }

            return solveWithSor(linearizedFlows, 1.8, MAX_ASYMMETRIC_LINEAR_SOLVER_SWEEPS, work);
        }

        if (solveWithConjugateGradient(linearizedFlows, work)) {
            return Result.SOLVED;
        }

        if (inconsistentLinearization) {
            return Result.INCONSISTENT;
        }

        return solveWithSor(linearizedFlows, PRESSURE_SOR_FACTOR, MAX_LINEAR_SOLVER_SWEEPS, work);
    }

    private static double dot(double[] first, double[] second) {
        double result = 0;
        for (int index = 0; index < first.length; index++) {
            result += first[index] * second[index];
        }
        return result;
    }

    private static boolean containsAsymmetricLinearization(EdgeFlow[] linearizedFlows) {
        for (EdgeFlow flow : linearizedFlows) {
            if (flow.fromPressureConductance() == flow.toPressureConductance()) {
                continue;
            }

            return true;
        }
        return false;
    }

    private static void applyJacobiPreconditioner(double[] input, double[] output, double[] diagonal) {
        for (int index = 0; index < input.length; index++) {
            double divisor = diagonal[index];
            output[index] = divisor > 0 && Double.isFinite(divisor) ? input[index] / divisor : input[index];
        }
    }

    private boolean solveWithBiConjugateGradientStabilized(EdgeFlow[] linearizedFlows, SolverWork work) {
        int[] unknownIndexByNode = new int[nodes.size()];
        Arrays.fill(unknownIndexByNode, -1);
        int unknownCount = 0;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            if (nodes.get(nodeIndex).fixed) {
                continue;
            }

            unknownIndexByNode[nodeIndex] = unknownCount++;
        }
        if (unknownCount == 0) {
            return true;
        }

        double[] diagonal = new double[unknownCount];
        double[] rightHandSide = new double[unknownCount];
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            EdgeFlow linearized = linearizedFlows[edgeIndex];
            int fromUnknown = unknownIndexByNode[edge.from];
            int toUnknown = unknownIndexByNode[edge.to];
            double fromPressureConductance = linearized.fromPressureConductance();
            double toPressureConductance = linearized.toPressureConductance();
            double constantFlowRate = linearized.constantFlowRate();
            if (fromUnknown >= 0) {
                diagonal[fromUnknown] += fromPressureConductance;
                rightHandSide[fromUnknown] -= constantFlowRate;
                if (toUnknown < 0) {
                    rightHandSide[fromUnknown] += toPressureConductance * nodes.get(edge.to).pressurePa;
                }
            }
            if (toUnknown < 0) {
                continue;
            }

            diagonal[toUnknown] += toPressureConductance;
            rightHandSide[toUnknown] += constantFlowRate;
            if (fromUnknown >= 0) {
                continue;
            }

            rightHandSide[toUnknown] += fromPressureConductance * nodes.get(edge.from).pressurePa;
        }

        if (solveWithForestElimination(linearizedFlows, work, unknownIndexByNode, diagonal, rightHandSide)) {
            return true;
        }

        if (inconsistentLinearization) {
            return false;
        }

        double[] pressure = new double[unknownCount];
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int unknownIndex = unknownIndexByNode[nodeIndex];
            if (unknownIndex < 0) {
                continue;
            }

            pressure[unknownIndex] = nodes.get(nodeIndex).pressurePa;
        }

        double[] operatorPressure = new double[unknownCount];
        applyAsymmetricLinearPressureOperator(pressure, operatorPressure, unknownIndexByNode, linearizedFlows);
        double[] residual = new double[unknownCount];
        double maxResidual = 0;
        for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
            residual[unknownIndex] = rightHandSide[unknownIndex] - operatorPressure[unknownIndex];
            maxResidual = Mth.absMax(maxResidual, residual[unknownIndex]);
        }
        if (maxResidual <= residuals.flowResidualTolerance(pressure, unknownIndexByNode, linearizedFlows)) {
            return applySolvedPressures(pressure, unknownIndexByNode, linearizedFlows);
        }

        double[] shadowResidual = residual.clone();
        double[] searchDirection = new double[unknownCount];
        double[] operatorDirection = new double[unknownCount];
        double[] preconditionedDirection = new double[unknownCount];
        double[] intermediateResidual = new double[unknownCount];
        double[] preconditionedIntermediate = new double[unknownCount];
        double[] operatorIntermediate = new double[unknownCount];
        double previousDotResult = 1;
        double alpha = 1;
        double omega = 1;
        int maxIterations = (int) Mth.clamp(unknownCount * 2L, MIN_CONJUGATE_GRADIENT_ITERATIONS, MAX_CONJUGATE_GRADIENT_ITERATIONS);
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            work.biConjugateGradientStabilizedIterations++;
            double dotResult = dot(shadowResidual, residual);
            if (!Double.isFinite(dotResult) || Math.abs(dotResult) <= CONJUGATE_GRADIENT_BREAKDOWN_EPSILON) {
                return false;
            }

            if (iteration == 0) {
                System.arraycopy(residual, 0, searchDirection, 0, unknownCount);
            }
            else {
                if (!Double.isFinite(omega) || Math.abs(omega) <= CONJUGATE_GRADIENT_BREAKDOWN_EPSILON) {
                    return false;
                }

                double beta = dotResult / previousDotResult * alpha / omega;
                if (!Double.isFinite(beta)) {
                    return false;
                }

                for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
                    searchDirection[unknownIndex] = residual[unknownIndex] + beta * (searchDirection[unknownIndex] - omega * operatorDirection[unknownIndex]);
                }
            }

            applyJacobiPreconditioner(searchDirection, preconditionedDirection, diagonal);
            applyAsymmetricLinearPressureOperator(preconditionedDirection, operatorDirection, unknownIndexByNode, linearizedFlows);
            double shadowDotOperator = dot(shadowResidual, operatorDirection);
            if (!Double.isFinite(shadowDotOperator) || Math.abs(shadowDotOperator) <= CONJUGATE_GRADIENT_BREAKDOWN_EPSILON) {
                return false;
            }

            alpha = dotResult / shadowDotOperator;
            if (!Double.isFinite(alpha)) {
                return false;
            }

            maxResidual = 0;
            for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
                intermediateResidual[unknownIndex] = residual[unknownIndex] - alpha * operatorDirection[unknownIndex];
                maxResidual = Mth.absMax(maxResidual, intermediateResidual[unknownIndex]);
            }
            if (maxResidual <= residuals.flowResidualTolerance(pressure, unknownIndexByNode, linearizedFlows)) {
                for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
                    pressure[unknownIndex] += alpha * preconditionedDirection[unknownIndex];
                }
                return applySolvedPressures(pressure, unknownIndexByNode, linearizedFlows);
            }

            applyJacobiPreconditioner(intermediateResidual, preconditionedIntermediate, diagonal);
            applyAsymmetricLinearPressureOperator(preconditionedIntermediate, operatorIntermediate, unknownIndexByNode, linearizedFlows);
            double operatorDotOperator = dot(operatorIntermediate, operatorIntermediate);
            if (!Double.isFinite(operatorDotOperator) || operatorDotOperator <= CONJUGATE_GRADIENT_BREAKDOWN_EPSILON) {
                return false;
            }

            omega = dot(operatorIntermediate, intermediateResidual) / operatorDotOperator;
            if (!Double.isFinite(omega) || Math.abs(omega) <= CONJUGATE_GRADIENT_BREAKDOWN_EPSILON) {
                return false;
            }

            maxResidual = 0;
            for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
                pressure[unknownIndex] += alpha * preconditionedDirection[unknownIndex] + omega * preconditionedIntermediate[unknownIndex];
                residual[unknownIndex] = intermediateResidual[unknownIndex] - omega * operatorIntermediate[unknownIndex];
                maxResidual = Mth.absMax(maxResidual, residual[unknownIndex]);
            }
            if (maxResidual <= residuals.flowResidualTolerance(pressure, unknownIndexByNode, linearizedFlows)) {
                return applySolvedPressures(pressure, unknownIndexByNode, linearizedFlows);
            }

            previousDotResult = dotResult;
        }
        return false;
    }

    private void applyAsymmetricLinearPressureOperator(double[] pressure, double[] result, int[] unknownIndexByNode, EdgeFlow[] linearizedFlows) {
        Arrays.fill(result, 0);
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            EdgeFlow linearized = linearizedFlows[edgeIndex];
            int fromUnknown = unknownIndexByNode[edge.from];
            int toUnknown = unknownIndexByNode[edge.to];
            double fromPressure = fromUnknown < 0 ? 0 : pressure[fromUnknown];
            double toPressure = toUnknown < 0 ? 0 : pressure[toUnknown];
            double variableFlowRate = linearized.fromPressureConductance() * fromPressure - linearized.toPressureConductance() * toPressure;
            if (fromUnknown >= 0) {
                result[fromUnknown] += variableFlowRate;
            }
            if (toUnknown < 0) {
                continue;
            }

            result[toUnknown] -= variableFlowRate;
        }
    }

    private boolean solveWithConjugateGradient(EdgeFlow[] linearizedFlows, SolverWork work) {
        int[] unknownIndexByNode = new int[nodes.size()];
        Arrays.fill(unknownIndexByNode, -1);
        int unknownCount = 0;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            if (nodes.get(nodeIndex).fixed) {
                continue;
            }

            unknownIndexByNode[nodeIndex] = unknownCount++;
        }
        if (unknownCount == 0) {
            return true;
        }

        double[] diagonal = new double[unknownCount];
        double[] rightHandSide = new double[unknownCount];
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            EdgeFlow linearized = linearizedFlows[edgeIndex];
            int fromUnknown = unknownIndexByNode[edge.from];
            int toUnknown = unknownIndexByNode[edge.to];
            double conductance = linearized.symmetricConductance();
            double constantFlowRate = linearized.constantFlowRate();
            if (fromUnknown >= 0) {
                diagonal[fromUnknown] += conductance;
                rightHandSide[fromUnknown] -= constantFlowRate;
                if (toUnknown < 0) {
                    rightHandSide[fromUnknown] += conductance * nodes.get(edge.to).pressurePa;
                }
            }
            if (toUnknown < 0) {
                continue;
            }

            diagonal[toUnknown] += conductance;
            rightHandSide[toUnknown] += constantFlowRate;
            if (fromUnknown >= 0) {
                continue;
            }

            rightHandSide[toUnknown] += conductance * nodes.get(edge.from).pressurePa;
        }

        double[] pressure = new double[unknownCount];
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int unknownIndex = unknownIndexByNode[nodeIndex];
            if (unknownIndex < 0) {
                continue;
            }

            pressure[unknownIndex] = nodes.get(nodeIndex).pressurePa;
        }

        double[] operatorPressure = new double[unknownCount];
        applyLinearPressureOperator(pressure, operatorPressure, unknownIndexByNode, linearizedFlows);
        double[] residual = new double[unknownCount];
        double maxResidual = 0;
        for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
            residual[unknownIndex] = rightHandSide[unknownIndex] - operatorPressure[unknownIndex];
            maxResidual = Mth.absMax(maxResidual, residual[unknownIndex]);
            if (diagonal[unknownIndex] > 0 || Math.abs(residual[unknownIndex]) <= FLOW_RESIDUAL_ABSOLUTE_TOLERANCE) {
                continue;
            }

            return false;
        }

        if (maxResidual <= residuals.flowResidualTolerance(pressure, unknownIndexByNode, linearizedFlows)) {
            return applySolvedPressures(pressure, unknownIndexByNode, linearizedFlows);
        }

        if (solveWithForestElimination(linearizedFlows, work, unknownIndexByNode, diagonal, rightHandSide)) {
            return true;
        }

        if (inconsistentLinearization) {
            return false;
        }

        double[] preconditionedResidual = new double[unknownCount];
        double[] searchDirection = new double[unknownCount];
        for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
            if (diagonal[unknownIndex] <= 0) {
                continue;
            }

            preconditionedResidual[unknownIndex] = residual[unknownIndex] / diagonal[unknownIndex];
            searchDirection[unknownIndex] = preconditionedResidual[unknownIndex];
        }
        double residualDotPreconditioned = dot(residual, preconditionedResidual);
        if (!Double.isFinite(residualDotPreconditioned) || residualDotPreconditioned <= CONJUGATE_GRADIENT_BREAKDOWN_EPSILON) {
            return false;
        }

        double[] operatorDirection = new double[unknownCount];
        int maxIterations = (int) Mth.clamp(unknownCount * 2L, MIN_CONJUGATE_GRADIENT_ITERATIONS, MAX_CONJUGATE_GRADIENT_ITERATIONS);
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            work.conjugateGradientIterations++;
            applyLinearPressureOperator(searchDirection, operatorDirection, unknownIndexByNode, linearizedFlows);
            double directionDotOperator = dot(searchDirection, operatorDirection);
            if (!Double.isFinite(directionDotOperator) || directionDotOperator <= CONJUGATE_GRADIENT_BREAKDOWN_EPSILON) {
                return false;
            }

            double alpha = residualDotPreconditioned / directionDotOperator;
            if (!Double.isFinite(alpha)) {
                return false;
            }

            maxResidual = 0;
            for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
                pressure[unknownIndex] += alpha * searchDirection[unknownIndex];
                residual[unknownIndex] -= alpha * operatorDirection[unknownIndex];
                maxResidual = Mth.absMax(maxResidual, residual[unknownIndex]);
            }
            if (maxResidual <= residuals.flowResidualTolerance(pressure, unknownIndexByNode, linearizedFlows)) {
                return applySolvedPressures(pressure, unknownIndexByNode, linearizedFlows);
            }

            double nextResidualDotPreconditioned = 0;
            for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
                if (diagonal[unknownIndex] <= 0) {
                    preconditionedResidual[unknownIndex] = 0;
                    continue;
                }

                preconditionedResidual[unknownIndex] = residual[unknownIndex] / diagonal[unknownIndex];
                nextResidualDotPreconditioned += residual[unknownIndex] * preconditionedResidual[unknownIndex];
            }
            if (!Double.isFinite(nextResidualDotPreconditioned) || nextResidualDotPreconditioned <= CONJUGATE_GRADIENT_BREAKDOWN_EPSILON) {
                return false;
            }

            double beta = nextResidualDotPreconditioned / residualDotPreconditioned;
            for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
                searchDirection[unknownIndex] = preconditionedResidual[unknownIndex] + beta * searchDirection[unknownIndex];
            }
            residualDotPreconditioned = nextResidualDotPreconditioned;
        }
        return false;
    }

    private boolean solveWithForestElimination(EdgeFlow[] linearizedFlows, SolverWork work, int[] unknownIndexByNode, double[] diagonal, double[] rightHandSide) {
        work.forestDirectSolveAttempts++;
        int unknownCount = diagonal.length;
        Map<Long, double[]> conductanceByPair = new HashMap<>();
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            EdgeFlow flow = linearizedFlows[edgeIndex];
            if (flow.fromPressureConductance() <= 0 && flow.toPressureConductance() <= 0) {
                continue;
            }

            FlowEdge edge = edges.get(edgeIndex);
            int fromUnknown = unknownIndexByNode[edge.from];
            int toUnknown = unknownIndexByNode[edge.to];
            if (fromUnknown < 0 || toUnknown < 0) {
                continue;
            }

            if (fromUnknown == toUnknown) {
                return false;
            }

            int first = Math.min(fromUnknown, toUnknown);
            int second = Math.max(fromUnknown, toUnknown);
            long pairKey = (long) first << 32 | second & 0xffffffffL;
            double[] conductances = conductanceByPair.computeIfAbsent(pairKey, ignored -> new double[2]);
            conductances[0] += fromUnknown == first ? flow.toPressureConductance() : flow.fromPressureConductance();
            conductances[1] += fromUnknown == first ? flow.fromPressureConductance() : flow.toPressureConductance();
            if (!Double.isFinite(conductances[0]) || !Double.isFinite(conductances[1])) {
                return false;
            }
        }

        int[] degree = new int[unknownCount];
        for (long pairKey : conductanceByPair.keySet()) {
            int first = (int) (pairKey >>> 32);
            int second = (int) pairKey;
            degree[first]++;
            degree[second]++;
        }

        int[] offsets = new int[unknownCount + 1];
        for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
            offsets[unknownIndex + 1] = offsets[unknownIndex] + degree[unknownIndex];
        }
        int[] cursor = offsets.clone();
        int[] neighbors = new int[offsets[unknownCount]];
        double[] neighborConductance = new double[neighbors.length];
        double[] reverseNeighborConductance = new double[neighbors.length];
        for (Entry<Long, double[]> entry : conductanceByPair.entrySet()) {
            long pairKey = entry.getKey();
            int first = (int) (pairKey >>> 32);
            int second = (int) pairKey;
            double[] conductances = entry.getValue();
            int firstSlot = cursor[first]++;
            neighbors[firstSlot] = second;
            neighborConductance[firstSlot] = conductances[0];
            reverseNeighborConductance[firstSlot] = conductances[1];
            int secondSlot = cursor[second]++;
            neighbors[secondSlot] = first;
            neighborConductance[secondSlot] = conductances[1];
            reverseNeighborConductance[secondSlot] = conductances[0];
        }

        double[] reducedDiagonal = diagonal.clone();
        double[] reducedRightHandSide = rightHandSide.clone();
        int[] remainingDegree = degree.clone();
        boolean[] eliminated = new boolean[unknownCount];
        int[] pending = new int[unknownCount];
        int pendingCount = 0;
        for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
            if (remainingDegree[unknownIndex] > 1) {
                continue;
            }

            pending[pendingCount++] = unknownIndex;
        }

        int[] eliminationOrder = new int[unknownCount];
        int[] parent = new int[unknownCount];
        Arrays.fill(parent, -1);
        double[] parentConductance = new double[unknownCount];
        double[] eliminatedDiagonal = new double[unknownCount];
        double[] eliminatedRightHandSide = new double[unknownCount];
        int eliminatedCount = 0;
        for (int pendingIndex = 0; pendingIndex < pendingCount; pendingIndex++) {
            int unknownIndex = pending[pendingIndex];
            if (eliminated[unknownIndex]) {
                continue;
            }

            int parentIndex = -1;
            double connectionConductance = 0;
            double reverseConnectionConductance = 0;
            for (int slot = offsets[unknownIndex]; slot < offsets[unknownIndex + 1]; slot++) {
                int adjacent = neighbors[slot];
                if (eliminated[adjacent]) {
                    continue;
                }

                if (parentIndex >= 0) {
                    return false;
                }

                parentIndex = adjacent;
                connectionConductance = neighborConductance[slot];
                reverseConnectionConductance = reverseNeighborConductance[slot];
            }

            double pivot = reducedDiagonal[unknownIndex];
            double pivotTolerance = Mth.absMax(CONJUGATE_GRADIENT_BREAKDOWN_EPSILON, diagonal[unknownIndex] * FOREST_DIRECT_PIVOT_RELATIVE_EPSILON);
            if (!Double.isFinite(pivot) || pivot <= pivotTolerance || !Double.isFinite(reducedRightHandSide[unknownIndex])) {
                inconsistentLinearization = detectInconsistency && GasLinearComponentBalance.isInconsistent(nodes, edges, linearizedFlows, residuals.flowResidualTolerance(linearizedFlows));
                return false;
            }

            parent[unknownIndex] = parentIndex;
            parentConductance[unknownIndex] = connectionConductance;
            eliminatedDiagonal[unknownIndex] = pivot;
            eliminatedRightHandSide[unknownIndex] = reducedRightHandSide[unknownIndex];
            eliminationOrder[eliminatedCount++] = unknownIndex;
            eliminated[unknownIndex] = true;
            if (parentIndex < 0) {
                continue;
            }

            double multiplier = reverseConnectionConductance / pivot;
            double nextDiagonal = reducedDiagonal[parentIndex] - connectionConductance * multiplier;
            double nextRightHandSide = reducedRightHandSide[parentIndex] + reducedRightHandSide[unknownIndex] * multiplier;
            if (!Double.isFinite(nextDiagonal) || !Double.isFinite(nextRightHandSide)) {
                return false;
            }

            reducedDiagonal[parentIndex] = nextDiagonal;
            reducedRightHandSide[parentIndex] = nextRightHandSide;
            remainingDegree[parentIndex]--;
            if (remainingDegree[parentIndex] != 1) {
                continue;
            }

            pending[pendingCount++] = parentIndex;
        }

        if (eliminatedCount != unknownCount) {
            return false;
        }

        double[] pressure = new double[unknownCount];
        for (int orderIndex = eliminatedCount - 1; orderIndex >= 0; orderIndex--) {
            int unknownIndex = eliminationOrder[orderIndex];
            int parentIndex = parent[unknownIndex];
            double numerator = eliminatedRightHandSide[unknownIndex];
            if (parentIndex >= 0) {
                numerator += parentConductance[unknownIndex] * pressure[parentIndex];
            }
            double solvedPressure = numerator / eliminatedDiagonal[unknownIndex];
            if (!Double.isFinite(solvedPressure)) {
                return false;
            }

            pressure[unknownIndex] = solvedPressure;
        }
        if (!applySolvedPressures(pressure, unknownIndexByNode, linearizedFlows)) {
            return false;
        }

        work.forestDirectSolveSuccesses++;
        return true;
    }

    private void applyLinearPressureOperator(double[] pressure, double[] result, int[] unknownIndexByNode, EdgeFlow[] linearizedFlows) {
        Arrays.fill(result, 0);
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            double conductance = linearizedFlows[edgeIndex].symmetricConductance();
            if (conductance <= 0) {
                continue;
            }

            FlowEdge edge = edges.get(edgeIndex);
            int fromUnknown = unknownIndexByNode[edge.from];
            int toUnknown = unknownIndexByNode[edge.to];
            double fromPressure = fromUnknown < 0 ? 0 : pressure[fromUnknown];
            double toPressure = toUnknown < 0 ? 0 : pressure[toUnknown];
            double pressureDifference = conductance * (fromPressure - toPressure);
            if (fromUnknown >= 0) {
                result[fromUnknown] += pressureDifference;
            }
            if (toUnknown < 0) {
                continue;
            }

            result[toUnknown] -= pressureDifference;
        }
    }

    private boolean applySolvedPressures(double[] pressure, int[] unknownIndexByNode, EdgeFlow[] linearizedFlows) {
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int unknownIndex = unknownIndexByNode[nodeIndex];
            if (unknownIndex < 0) {
                continue;
            }

            double solvedPressure = pressure[unknownIndex];
            if (Double.isFinite(solvedPressure) && solvedPressure >= -ACTIVE_SET_PRESSURE_EPSILON_PA) {
                continue;
            }

            return false;
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int unknownIndex = unknownIndexByNode[nodeIndex];
            if (unknownIndex < 0) {
                continue;
            }

            nodes.get(nodeIndex).pressurePa = Math.max(GasPressure.VACUUM_PA, pressure[unknownIndex]);
        }
        return residuals.linearizedFlowsConverged(linearizedFlows);
    }

    private Result solveWithSor(EdgeFlow[] linearizedFlows, double relaxationFactor, int maxSweeps, SolverWork work) {
        SorRows rows = compileSorRows(linearizedFlows);
        double[] pressures = new double[nodes.size()];
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            pressures[nodeIndex] = nodes.get(nodeIndex).pressurePa;
        }
        for (int sweep = 0; sweep < maxSweeps; sweep++) {
            work.sorSweeps++;
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                PressureNode node = nodes.get(nodeIndex);
                if (node.fixed) {
                    continue;
                }

                double targetPressure = rows.pressureTarget(nodeIndex, pressures);
                if (!Double.isFinite(targetPressure)) {
                    continue;
                }

                double updated = Mth.lerp(relaxationFactor, node.pressurePa, targetPressure);
                if (!Double.isFinite(updated)) {
                    continue;
                }

                node.pressurePa = Math.max(GasPressure.VACUUM_PA, updated);
                pressures[nodeIndex] = node.pressurePa;
            }
            if (!residuals.linearizedFlowsConverged(linearizedFlows)) {
                continue;
            }

            return Result.SOLVED;
        }
        return Result.UNRESOLVED;
    }

    private SorRows compileSorRows(EdgeFlow[] linearizedFlows) {
        int nodeCount = nodes.size();
        int[] offsets = new int[nodeCount + 1];
        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            offsets[nodeIndex + 1] = offsets[nodeIndex] + (nodes.get(nodeIndex).fixed ? 0 : incidentEdges.get(nodeIndex).size());
        }
        int termCount = offsets[nodeCount];
        double[] diagonal = new double[nodeCount];
        int[] adjacentNodes = new int[termCount];
        double[] conductances = new double[termCount];
        double[] constants = new double[termCount];
        boolean[] outgoing = new boolean[termCount];
        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            if (nodes.get(nodeIndex).fixed) {
                continue;
            }

            int slot = offsets[nodeIndex];
            for (int edgeIndex : incidentEdges.get(nodeIndex)) {
                FlowEdge edge = edges.get(edgeIndex);
                EdgeFlow linearized = linearizedFlows[edgeIndex];
                boolean from = edge.from == nodeIndex;
                diagonal[nodeIndex] += from ? linearized.fromPressureConductance() : linearized.toPressureConductance();
                adjacentNodes[slot] = from ? edge.to : edge.from;
                conductances[slot] = from ? linearized.toPressureConductance() : linearized.fromPressureConductance();
                constants[slot] = linearized.constantFlowRate();
                outgoing[slot] = from;
                slot++;
            }
        }
        return new SorRows(offsets, diagonal, adjacentNodes, conductances, constants, outgoing);
    }

    private record SorRows(int[] offsets, double[] diagonal, int[] adjacentNodes, double[] conductances, double[] constants, boolean[] outgoing) {
        private double pressureTarget(int nodeIndex, double[] pressures) {
            if (diagonal[nodeIndex] <= 0) {
                return Double.NaN;
            }

            double numerator = 0;
            for (int slot = offsets[nodeIndex]; slot < offsets[nodeIndex + 1]; slot++) {
                double adjacentPressure = pressures[adjacentNodes[slot]];
                if (outgoing[slot]) {
                    numerator += conductances[slot] * adjacentPressure - constants[slot];
                    continue;
                }

                numerator += conductances[slot] * adjacentPressure + constants[slot];
            }
            return numerator / diagonal[nodeIndex];
        }
    }
}
