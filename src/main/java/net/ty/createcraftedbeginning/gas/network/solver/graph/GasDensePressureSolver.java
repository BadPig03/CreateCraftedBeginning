package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.PressureNode;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class GasDensePressureSolver {
    private static final int MAX_UNKNOWN_NODES = 128;
    private static final double MIN_PIVOT = 1.0E-30;
    private final List<PressureNode> nodes;
    private final List<FlowEdge> edges;

    public GasDensePressureSolver(GasPressureFlowGraph graph) {
        nodes = graph.nodes;
        edges = graph.edges;
    }

    public boolean projectPressures(EdgeFlow[] flows) {
        int[] unknownIndices = new int[nodes.size()];
        Arrays.fill(unknownIndices, -1);
        int count = 0;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            if (nodes.get(nodeIndex).fixed) {
                continue;
            }

            if (count == MAX_UNKNOWN_NODES) {
                return false;
            }

            unknownIndices[nodeIndex] = count++;
        }
        double[][] rows = new double[count][count + 1];
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            EdgeFlow flow = flows[edgeIndex];
            int from = unknownIndices[edge.from];
            int to = unknownIndices[edge.to];
            double fromConductance = flow.fromPressureConductance();
            double toConductance = flow.toPressureConductance();
            double constantFlow = flow.constantFlowRate();
            if (from >= 0) {
                rows[from][from] += fromConductance;
                rows[from][count] -= constantFlow;
                if (to >= 0) {
                    rows[from][to] -= toConductance;
                }
                else {
                    rows[from][count] += toConductance * nodes.get(edge.to).pressurePa;
                }
            }
            if (to < 0) {
                continue;
            }

            rows[to][to] += toConductance;
            rows[to][count] += constantFlow;
            if (from >= 0) {
                rows[to][from] -= fromConductance;
                continue;
            }

            rows[to][count] += fromConductance * nodes.get(edge.from).pressurePa;
        }
        for (int column = 0; column < count; column++) {
            int pivot = column;
            for (int row = column + 1; row < count; row++) {
                if (Math.abs(rows[row][column]) <= Math.abs(rows[pivot][column])) {
                    continue;
                }

                pivot = row;
            }
            double[] swapped = rows[column];
            rows[column] = rows[pivot];
            rows[pivot] = swapped;
            double divisor = rows[column][column];
            if (!Double.isFinite(divisor) || Math.abs(divisor) <= MIN_PIVOT) {
                return false;
            }

            for (int row = column + 1; row < count; row++) {
                double factor = rows[row][column] / divisor;
                rows[row][column] = 0;
                if (factor == 0) {
                    continue;
                }

                for (int entry = column + 1; entry <= count; entry++) {
                    rows[row][entry] -= factor * rows[column][entry];
                }
            }
        }
        double[] pressure = new double[count];
        for (int row = count - 1; row >= 0; row--) {
            double rightHandSide = rows[row][count];
            for (int column = row + 1; column < count; column++) {
                rightHandSide -= rows[row][column] * pressure[column];
            }
            pressure[row] = rightHandSide / rows[row][row];
            if (!Double.isFinite(pressure[row])) {
                return false;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int unknownIndex = unknownIndices[nodeIndex];
            if (unknownIndex < 0) {
                continue;
            }

            nodes.get(nodeIndex).pressurePa = Math.max(GasPressure.VACUUM_PA, pressure[unknownIndex]);
        }
        return true;
    }

    boolean isApplicable() {
        int unknownCount = 0;
        for (PressureNode node : nodes) {
            if (!node.fixed && ++unknownCount > MAX_UNKNOWN_NODES) {
                return false;
            }
        }
        return true;
    }
}
