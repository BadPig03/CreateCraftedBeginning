package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.PressureNode;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureResiduals {
    static final double FLOW_RESIDUAL_ABSOLUTE_TOLERANCE = 1.0E-8;
    private static final double FLOW_RESIDUAL_RELATIVE_TOLERANCE = 1.0E-10;

    private final List<PressureNode> nodes;
    private final List<FlowEdge> edges;
    private double[] residualByNode = new double[0];

    @Internal
    public GasPressureResiduals(GasPressureFlowGraph graph) {
        nodes = graph.nodes;
        edges = graph.edges;
    }

    private static double linearizedFlowScale(EdgeFlow linearized, double fromPressurePa, double toPressurePa) {
        return Math.abs(linearized.constantFlowRate()) + linearized.fromPressureConductance() * Math.abs(fromPressurePa) + linearized.toPressureConductance() * Math.abs(toPressurePa);
    }

    @Internal
    public double nonlinearResidualNorm(EdgeFlow[] flows) {
        double[] residuals = clearResiduals();
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            residuals[edge.from] -= flows[edgeIndex].flowRate();
            residuals[edge.to] += flows[edgeIndex].flowRate();
        }
        double norm = 0;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            if (nodes.get(nodeIndex).fixed) {
                continue;
            }

            norm += Math.abs(residuals[nodeIndex]);
        }
        return norm;
    }

    @Internal
    public double flowResidualTolerance(EdgeFlow[] linearizedFlows) {
        double maxFlowScale = 0;
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            EdgeFlow linearized = linearizedFlows[edgeIndex];
            double flowScale = linearizedFlowScale(linearized, nodes.get(edge.from).pressurePa, nodes.get(edge.to).pressurePa);
            if (!Double.isFinite(flowScale)) {
                return FLOW_RESIDUAL_ABSOLUTE_TOLERANCE;
            }

            maxFlowScale = Math.max(maxFlowScale, flowScale);
        }
        return Math.max(FLOW_RESIDUAL_ABSOLUTE_TOLERANCE, maxFlowScale * FLOW_RESIDUAL_RELATIVE_TOLERANCE);
    }

    @Internal
    public FlowResidual calculateUnknownResidual(EdgeFlow[] flows, boolean linearized) {
        double[] residuals = clearResiduals();
        double maxFlowRate = 0;
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            EdgeFlow solved = flows[edgeIndex];
            double flowRate = linearized ? solved.linearizedFlowRate(nodes.get(edge.from).pressurePa, nodes.get(edge.to).pressurePa) : solved.flowRate();
            if (!Double.isFinite(flowRate)) {
                return FlowResidual.NOT_CONVERGED;
            }

            maxFlowRate = Mth.absMax(maxFlowRate, flowRate);
            if (Math.abs(flowRate) <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            residuals[edge.from] -= flowRate;
            residuals[edge.to] += flowRate;
        }

        return new FlowResidual(maxUnknownResidual(residuals), maxFlowRate);
    }

    @Internal
    public boolean linearizedFlowsConverged(EdgeFlow[] flows) {
        double[] residuals = clearResiduals();
        double maxFlowScale = 0;
        boolean finiteScale = true;
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            EdgeFlow flow = flows[edgeIndex];
            double fromPressure = nodes.get(edge.from).pressurePa;
            double toPressure = nodes.get(edge.to).pressurePa;
            double flowRate = flow.linearizedFlowRate(fromPressure, toPressure);
            if (!Double.isFinite(flowRate)) {
                return false;
            }

            if (finiteScale) {
                double flowScale = linearizedFlowScale(flow, fromPressure, toPressure);
                finiteScale = Double.isFinite(flowScale);
                if (finiteScale) {
                    maxFlowScale = Math.max(maxFlowScale, flowScale);
                }
            }
            if (Math.abs(flowRate) <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            residuals[edge.from] -= flowRate;
            residuals[edge.to] += flowRate;
        }
        double tolerance = finiteScale ? Math.max(FLOW_RESIDUAL_ABSOLUTE_TOLERANCE, maxFlowScale * FLOW_RESIDUAL_RELATIVE_TOLERANCE) : FLOW_RESIDUAL_ABSOLUTE_TOLERANCE;
        double maxResidual = maxUnknownResidual(residuals);
        return Double.isFinite(maxResidual) && maxResidual <= tolerance;
    }

    double flowResidualTolerance(double[] pressure, int[] unknownIndexByNode, EdgeFlow[] linearizedFlows) {
        double maxFlowScale = 0;
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowEdge edge = edges.get(edgeIndex);
            EdgeFlow linearized = linearizedFlows[edgeIndex];
            int fromUnknown = unknownIndexByNode[edge.from];
            int toUnknown = unknownIndexByNode[edge.to];
            double fromPressure = fromUnknown < 0 ? nodes.get(edge.from).pressurePa : pressure[fromUnknown];
            double toPressure = toUnknown < 0 ? nodes.get(edge.to).pressurePa : pressure[toUnknown];
            double flowScale = linearizedFlowScale(linearized, fromPressure, toPressure);
            if (!Double.isFinite(flowScale)) {
                return FLOW_RESIDUAL_ABSOLUTE_TOLERANCE;
            }

            maxFlowScale = Math.max(maxFlowScale, flowScale);
        }
        return Math.max(FLOW_RESIDUAL_ABSOLUTE_TOLERANCE, maxFlowScale * FLOW_RESIDUAL_RELATIVE_TOLERANCE);
    }

    private double[] clearResiduals() {
        int nodeCount = nodes.size();
        if (residualByNode.length < nodeCount) {
            residualByNode = new double[nodeCount];
        }
        else {
            Arrays.fill(residualByNode, 0, nodeCount, 0);
        }
        return residualByNode;
    }

    private double maxUnknownResidual(double[] residuals) {
        double maxResidual = 0;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            if (nodes.get(nodeIndex).fixed) {
                continue;
            }

            maxResidual = Mth.absMax(maxResidual, residuals[nodeIndex]);
        }
        return maxResidual;
    }

    @Internal
    public record FlowResidual(double maxResidual, double maxFlowRate) {
        private static final FlowResidual NOT_CONVERGED = new FlowResidual(Double.POSITIVE_INFINITY, 0);

        @Internal
        public boolean converged(double tolerance) {
            return Double.isFinite(maxResidual) && Double.isFinite(maxFlowRate) && Double.isFinite(tolerance) && maxResidual <= Math.max(FLOW_RESIDUAL_ABSOLUTE_TOLERANCE, tolerance);
        }
    }
}
