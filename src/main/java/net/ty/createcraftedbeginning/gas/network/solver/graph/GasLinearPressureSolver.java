package net.ty.createcraftedbeginning.gas.network.solver.graph;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.PressureNode;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.SolverWork;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

import static net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.ACTIVE_SET_PRESSURE_EPSILON_PA;
import static net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureResiduals.FLOW_RESIDUAL_ABSOLUTE_TOLERANCE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasLinearPressureSolver {
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
    private final ConjugateGradientWorkspace conjugateGradient = new ConjugateGradientWorkspace();
    private final ForestWorkspace forest = new ForestWorkspace();
    private boolean inconsistentLinearization;
    private boolean detectInconsistency;

    enum Result {
        SOLVED,
        UNRESOLVED,
        INCONSISTENT
    }

    GasLinearPressureSolver(GasPressureFlowGraph graph, GasPressureResiduals residuals) {
        nodes = graph.nodes;
        edges = graph.edges;
        incidentEdges = graph.incidentEdges;
        this.residuals = residuals;
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

    Result solve(EdgeFlow[] linearizedFlows, SolverWork work) {
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

        conjugateGradient.prepare(unknownCount, unknownIndexByNode, edges, linearizedFlows);
        double[] preconditionedResidual = conjugateGradient.preconditionedResidual;
        double[] searchDirection = conjugateGradient.searchDirection;
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

        double[] operatorDirection = conjugateGradient.operatorDirection;
        int maxIterations = (int) Mth.clamp(unknownCount * 2L, MIN_CONJUGATE_GRADIENT_ITERATIONS, MAX_CONJUGATE_GRADIENT_ITERATIONS);
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            work.conjugateGradientIterations++;
            conjugateGradient.apply(searchDirection, operatorDirection);
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
        forest.prepare(unknownCount, edges.size());
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
            int pair = forest.pairIndices.putIfAbsent(pairKey, forest.pairCount);
            if (pair < 0) {
                pair = forest.pairCount++;
                forest.pairKeys[pair] = pairKey;
                forest.forward[pair] = 0;
                forest.reverse[pair] = 0;
            }
            forest.forward[pair] += fromUnknown == first ? flow.toPressureConductance() : flow.fromPressureConductance();
            forest.reverse[pair] += fromUnknown == first ? flow.fromPressureConductance() : flow.toPressureConductance();
            if (!Double.isFinite(forest.forward[pair]) || !Double.isFinite(forest.reverse[pair])) {
                return false;
            }
        }

        int[] remainingDegree = forest.degree;
        for (int pair = 0; pair < forest.pairCount; pair++) {
            long pairKey = forest.pairKeys[pair];
            int first = (int) (pairKey >>> 32);
            int second = (int) pairKey;
            remainingDegree[first]++;
            remainingDegree[second]++;
        }

        int[] offsets = forest.offsets;
        for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
            offsets[unknownIndex + 1] = offsets[unknownIndex] + remainingDegree[unknownIndex];
        }
        int[] cursor = forest.cursor;
        System.arraycopy(offsets, 0, cursor, 0, unknownCount + 1);
        int[] neighbors = forest.neighbors;
        double[] neighborConductance = forest.neighborConductance;
        double[] reverseNeighborConductance = forest.reverseNeighborConductance;
        for (int pair = 0; pair < forest.pairCount; pair++) {
            long pairKey = forest.pairKeys[pair];
            int first = (int) (pairKey >>> 32);
            int second = (int) pairKey;
            int firstSlot = cursor[first]++;
            neighbors[firstSlot] = second;
            neighborConductance[firstSlot] = forest.forward[pair];
            reverseNeighborConductance[firstSlot] = forest.reverse[pair];
            int secondSlot = cursor[second]++;
            neighbors[secondSlot] = first;
            neighborConductance[secondSlot] = forest.reverse[pair];
            reverseNeighborConductance[secondSlot] = forest.forward[pair];
        }

        double[] reducedDiagonal = forest.reducedDiagonal;
        double[] reducedRightHandSide = forest.reducedRightHandSide;
        System.arraycopy(diagonal, 0, reducedDiagonal, 0, unknownCount);
        System.arraycopy(rightHandSide, 0, reducedRightHandSide, 0, unknownCount);
        boolean[] eliminated = forest.eliminated;
        int[] pending = forest.pending;
        int pendingCount = 0;
        for (int unknownIndex = 0; unknownIndex < unknownCount; unknownIndex++) {
            if (remainingDegree[unknownIndex] > 1) {
                continue;
            }

            pending[pendingCount++] = unknownIndex;
        }

        int[] eliminationOrder = forest.eliminationOrder;
        int[] parent = forest.parent;
        double[] parentConductance = forest.parentConductance;
        double[] eliminatedDiagonal = forest.eliminatedDiagonal;
        double[] eliminatedRightHandSide = forest.eliminatedRightHandSide;
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

        double[] pressure = forest.pressure;
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

    private static final class ForestWorkspace {
        private final Long2IntOpenHashMap pairIndices = new Long2IntOpenHashMap();
        private long[] pairKeys = {};
        private double[] forward = {};
        private double[] reverse = {};
        private int[] degree = {};
        private int[] offsets = {0};
        private int[] cursor = {0};
        private int[] neighbors = {};
        private double[] neighborConductance = {};
        private double[] reverseNeighborConductance = {};
        private double[] reducedDiagonal = {};
        private double[] reducedRightHandSide = {};
        private boolean[] eliminated = {};
        private int[] pending = {};
        private int[] eliminationOrder = {};
        private int[] parent = {};
        private double[] parentConductance = {};
        private double[] eliminatedDiagonal = {};
        private double[] eliminatedRightHandSide = {};
        private double[] pressure = {};
        private int pairCount;

        private ForestWorkspace() {
            pairIndices.defaultReturnValue(-1);
        }

        private void prepare(int unknownCount, int edgeCount) {
            pairIndices.clear();
            pairCount = 0;
            if (pairKeys.length < edgeCount) {
                pairKeys = new long[edgeCount];
                forward = new double[edgeCount];
                reverse = new double[edgeCount];
                neighbors = new int[edgeCount * 2];
                neighborConductance = new double[edgeCount * 2];
                reverseNeighborConductance = new double[edgeCount * 2];
            }
            if (degree.length < unknownCount) {
                degree = new int[unknownCount];
                offsets = new int[unknownCount + 1];
                cursor = new int[unknownCount + 1];
                reducedDiagonal = new double[unknownCount];
                reducedRightHandSide = new double[unknownCount];
                eliminated = new boolean[unknownCount];
                pending = new int[unknownCount];
                eliminationOrder = new int[unknownCount];
                parent = new int[unknownCount];
                parentConductance = new double[unknownCount];
                eliminatedDiagonal = new double[unknownCount];
                eliminatedRightHandSide = new double[unknownCount];
                pressure = new double[unknownCount];
            }
            Arrays.fill(degree, 0, unknownCount, 0);
            Arrays.fill(eliminated, 0, unknownCount, false);
        }
    }

    private static final class ConjugateGradientWorkspace {
        private int[] fromUnknown = new int[0];
        private int[] toUnknown = new int[0];
        private double[] conductances = new double[0];
        private int termCount;
        private double[] preconditionedResidual = new double[0];
        private double[] searchDirection = new double[0];
        private double[] operatorDirection = new double[0];

        private void prepare(int unknownCount, int[] unknownIndexByNode, List<FlowEdge> edges, EdgeFlow[] linearizedFlows) {
            if (fromUnknown.length < edges.size()) {
                fromUnknown = new int[edges.size()];
                toUnknown = new int[edges.size()];
                conductances = new double[edges.size()];
            }
            termCount = 0;
            for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
                double conductance = linearizedFlows[edgeIndex].symmetricConductance();
                if (conductance <= 0) {
                    continue;
                }

                FlowEdge edge = edges.get(edgeIndex);
                fromUnknown[termCount] = unknownIndexByNode[edge.from];
                toUnknown[termCount] = unknownIndexByNode[edge.to];
                conductances[termCount++] = conductance;
            }
            if (preconditionedResidual.length != unknownCount) {
                preconditionedResidual = new double[unknownCount];
                searchDirection = new double[unknownCount];
                operatorDirection = new double[unknownCount];
                return;
            }

            Arrays.fill(preconditionedResidual, 0);
            Arrays.fill(searchDirection, 0);
        }

        private void apply(double[] pressure, double[] result) {
            Arrays.fill(result, 0);
            for (int term = 0; term < termCount; term++) {
                int from = fromUnknown[term];
                int to = toUnknown[term];
                double fromPressure = from < 0 ? 0 : pressure[from];
                double toPressure = to < 0 ? 0 : pressure[to];
                double pressureDifference = conductances[term] * (fromPressure - toPressure);
                if (from >= 0) {
                    result[from] += pressureDifference;
                }
                if (to < 0) {
                    continue;
                }

                result[to] -= pressureDifference;
            }
        }
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
