package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.PressureNode;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasLinearComponentBalance {
    private GasLinearComponentBalance() {
    }

    @Internal
    public static boolean isInconsistent(List<PressureNode> nodes, List<FlowEdge> edges, EdgeFlow[] flows, double tolerance) {
        if (!Double.isFinite(tolerance) || tolerance < 0) {
            return false;
        }

        int count = nodes.size();
        int[] parent = new int[count];
        for (int i = 0; i < count; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < edges.size(); i++) {
            FlowEdge edge = edges.get(i);
            EdgeFlow flow = flows[i];
            if (!Double.isFinite(flow.fromPressureConductance()) || !Double.isFinite(flow.toPressureConductance()) || !Double.isFinite(flow.constantFlowRate()) || flow.fromPressureConductance() < 0 || flow.toPressureConductance() < 0) {
                return false;
            }

            if (nodes.get(edge.from).fixed || nodes.get(edge.to).fixed || flow.fromPressureConductance() <= 0 && flow.toPressureConductance() <= 0) {
                continue;
            }

            int from = find(parent, edge.from);
            int to = find(parent, edge.to);
            parent[from] = to;
        }
        int[] sizes = new int[count];
        boolean[] anchored = new boolean[count];
        double[] outgoing = new double[count];
        for (int i = 0; i < count; i++) {
            parent[i] = find(parent, i);
            if (!(!nodes.get(i).fixed)) {
                continue;
            }

            sizes[parent[i]]++;
        }
        for (int i = 0; i < edges.size(); i++) {
            FlowEdge edge = edges.get(i);
            EdgeFlow flow = flows[i];
            boolean fromFixed = nodes.get(edge.from).fixed;
            boolean toFixed = nodes.get(edge.to).fixed;
            int from = parent[edge.from];
            int to = parent[edge.to];
            if (fromFixed && toFixed || !fromFixed && !toFixed && from == to) {
                continue;
            }

            double boundaryFlow = flow.constantFlowRate()
                + (fromFixed ? flow.fromPressureConductance() * nodes.get(edge.from).pressurePa : 0)
                - (toFixed ? flow.toPressureConductance() * nodes.get(edge.to).pressurePa : 0);
            if (Math.abs(boundaryFlow) <= GasFlowMath.FLOW_RATE_EPSILON) {
                boundaryFlow = 0;
            }
            if (!fromFixed) {
                anchored[from] |= flow.fromPressureConductance() > 0;
                outgoing[from] += boundaryFlow;
            }
            if (toFixed) {
                continue;
            }

            anchored[to] |= flow.toPressureConductance() > 0;
            outgoing[to] -= boundaryFlow;
        }
        for (int i = 0; i < count; i++) {
            if (sizes[i] > 0 && !anchored[i] && Double.isFinite(outgoing[i]) && Math.abs(outgoing[i]) > sizes[i] * tolerance) {
                return true;
            }
        }
        return false;
    }

    private static int find(int[] parent, int node) {
        int root = node;
        while (parent[root] != root) {
            root = parent[root];
        }
        while (parent[node] != node) {
            int next = parent[node];
            parent[node] = root;
            node = next;
        }
        return root;
    }
}
