package net.ty.createcraftedbeginning.gametests.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasLinearComponentBalance;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasLinearPressureSolver;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasLinearPressureSolver.Result;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeConstraintState;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.SolverWork;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureResiduals;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasLinearComponentBalanceGameTests {
    private GasLinearComponentBalanceGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void inconsistentFloatingComponentSkipsFutileIterations(GameTestHelper helper) {
        GasPressureFlowGraph graph = chain();
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        GasLinearPressureSolver solver = new GasLinearPressureSolver(graph, residuals);
        EdgeFlow[] flows = {row(0, 0, 1000), row(1, 1, 0), row(0, 0, 2000)};
        SolverWork work = new SolverWork();
        helper.assertTrue(solver.solve(flows, work) == Result.INCONSISTENT, "Constant boundary imbalance was not detected");
        helper.assertTrue(work.getForestDirectSolveAttempts() > 0 && work.getConjugateGradientIterations() == 0 && work.getBiConjugateGradientStabilizedIterations() == 0 && work.getSorSweeps() == 0, "Inconsistent component spent iterative solver budget");
        helper.assertTrue(graph.getNodePressure(1) == 100 && graph.getNodePressure(2) == 100, "Inconsistency detection changed the pressure iterate");
        helper.assertTrue(!residuals.linearizedFlowsConverged(flows), "Inconsistency was mistaken for equilibrium");

        EdgeFlow[] anchored = {row(1, 1, 0), row(1, 1, 0), row(1, 1, 0)};
        helper.assertTrue(solver.solve(anchored, new SolverWork()) == Result.SOLVED, "Inconsistent status leaked into the next solve");
        helper.assertTrue(residuals.linearizedFlowsConverged(anchored), "Anchored chain did not balance");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void componentBalancePreservesFloatingAndAnchoredStates(GameTestHelper helper) {
        GasPressureFlowGraph graph = chain();
        EdgeFlow[] flows = {row(0, 0, 1000), row(1, 1, 0), row(0, 0, 1000)};
        helper.assertTrue(!inconsistent(graph, flows, 1.0E-8), "Balanced floating component was rejected");
        flows[2] = row(0, 0, 2000);
        flows[0] = row(1, 1, 1000);
        helper.assertTrue(!inconsistent(graph, flows, 1.0E-8), "Pressure-dependent boundary was rejected");

        flows[0] = row(1, 0, 0);
        flows[2] = row(0, 0, 300);
        helper.assertTrue(!inconsistent(graph, flows, 1.0E-8), "Known fixed-pressure inflow was omitted");
        flows[2] = row(0, 0, 301);
        helper.assertTrue(inconsistent(graph, flows, 1.0E-8), "One-way boundary was mistaken for a pressure anchor");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void componentBalanceUsesPerComponentToleranceAndCancelsInternalFlows(GameTestHelper helper) {
        GasPressureFlowGraph graph = chain();
        EdgeFlow[] flows = {row(0, 0, 1000), row(1, 2, 9000), row(0, 0, 1002)};
        helper.assertTrue(!inconsistent(graph, flows, 1), "Summed per-node tolerance was not preserved");
        flows[2] = row(0, 0, 1003);
        helper.assertTrue(inconsistent(graph, flows, 1), "Component imbalance beyond tolerance was ignored");

        flows[1] = row(0, 0, 500);
        flows[2] = row(0, 0, 1000);
        helper.assertTrue(inconsistent(graph, flows, 1), "Separate component imbalances cancelled globally");

        GasPressureFlowGraph tiny = new GasPressureFlowGraph();
        tiny.addUnknownNode(0);
        tiny.addFixedNode(0);
        EdgeFlow[] tinyFlows = new EdgeFlow[32];
        for (int i = 0; i < tinyFlows.length; i++) {
            tiny.addEdge(0, 1, 1000, Long.MAX_VALUE, null);
            tinyFlows[i] = row(0, 0, 1.0E-9);
        }
        helper.assertTrue(!inconsistent(tiny, tinyFlows, 1.0E-8), "Sub-epsilon edges accumulated into a false imbalance");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void componentBalanceLeavesNonfiniteArithmeticToExistingSolver(GameTestHelper helper) {
        GasPressureFlowGraph graph = chain();
        EdgeFlow[] flows = {row(0, 0, 1000), row(1, 1, 0), row(0, 0, 2000)};
        helper.assertTrue(!inconsistent(graph, flows, Double.NaN), "Nonfinite tolerance proved inconsistency");
        helper.assertTrue(!inconsistent(graph, flows, -1), "Negative tolerance proved inconsistency");
        flows[1] = row(Double.POSITIVE_INFINITY, 1, 0);
        helper.assertTrue(!inconsistent(graph, flows, 0), "Nonfinite coefficient proved inconsistency");
        flows[1] = row(1, 1, 0);
        flows[0] = row(Double.MAX_VALUE, 0, 0);
        helper.assertTrue(!inconsistent(graph, flows, 0), "Overflowed boundary sum proved inconsistency");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void constrainedCyclicNetworkPreservesConvergenceAndGraphStructure(GameTestHelper helper) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        graph.addFixedNode(1400000);
        for (double pressure : new double[]{1369999.9999999998, 1369980.0199800197, 1269980.01998002, 1169980.019980018, 1169976.6866466848, 1169963.3533133562, 1169855.0199800276, 1169771.6866466943, 299999.99999600364}) {
            graph.addUnknownNode(pressure);
        }
        graph.addFixedNode(100000);
        addEdge(graph, 0, 1, 100, 0.01, 713249);
        addEdge(graph, 1, 2, 500, 0.1, 0);
        addEdge(graph, 2, 3, Long.MAX_VALUE, 1.0E-5, 0);
        addEdge(graph, 3, 4, 500, 1.0E-5, 0);
        addEdge(graph, 4, 5, Long.MAX_VALUE, 0.1, 0);
        addEdge(graph, 5, 6, 500, 0.1, 0);
        addEdge(graph, 6, 7, 500, 0.01, 0);
        addEdge(graph, 7, 8, Long.MAX_VALUE, 0.001, 0);
        addEdge(graph, 8, 9, 1, 0.1, 0);
        addEdge(graph, 9, 10, Long.MAX_VALUE, 1.0E-5, 0);
        addEdge(graph, 1, 3, 1, 0.01, 0);
        addEdge(graph, 2, 4, 1, 0.1, 0);
        addEdge(graph, 3, 5, 1, 0.01, 0);
        addEdge(graph, 4, 6, Long.MAX_VALUE, 0.1, 0);
        addEdge(graph, 5, 7, 0, 1.0E-5, 0);
        addEdge(graph, 6, 8, 500, 0.01, 0);
        addEdge(graph, 7, 9, 1, 0.001, 0);
        addEdge(graph, 8, 10, 1, 0.01, 0);
        addEdge(graph, 1, 2, 500, 1.0E-4, 0);
        int[] incidentCounts = graph.getIncidentEdgeCounts();
        FlowGraphSolution solution = graph.solve();
        helper.assertTrue(solution.converged(), "Constrained cyclic network failed to converge");
        helper.assertTrue(solution.flowRate(0) > 0 && solution.flowRate(0) <= 100, "Recovered network stopped or exceeded the inlet cap");
        helper.assertTrue(graph.getNodes().size() == 11 && graph.getEdges().size() == 19, "Recovery left temporary anchors in the graph");
        for (int i = 0; i < incidentCounts.length; i++) {
            helper.assertTrue(graph.getIncidentEdgeCounts()[i] == incidentCounts[i], "Recovery left an anchor in adjacency lists");
        }
        EdgeFlow[] actual = new EdgeFlow[graph.getEdges().size()];
        EdgeFlow[] linearized = new EdgeFlow[graph.getEdges().size()];
        for (int i = 0; i < actual.length; i++) {
            FlowEdge edge = graph.getEdges().get(i);
            actual[i] = edge.calculate(graph.getNodes());
            linearized[i] = edge.linearize(graph.getNodes(), edge.nextConstraintState(graph.getNodes(), EdgeConstraintState.LINEAR));
        }
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        helper.assertTrue(residuals.calculateUnknownResidual(actual, false).converged(residuals.flowResidualTolerance(linearized)), "Recovered network violated actual flow balance");
        helper.succeed();
    }

    private static void addEdge(GasPressureFlowGraph graph, int from, int to, long cap, double conductance, long boost) {
        graph.addEdge(from, to, new GasTransportEdgeProperties(1000, GasTransportPressureDrive.pressureBoost(boost), cap, 0, conductance, false, true), null, null);
    }

    private static GasPressureFlowGraph chain() {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        graph.addFixedNode(300);
        graph.addUnknownNode(100);
        graph.addUnknownNode(100);
        graph.addFixedNode(0);
        graph.addEdge(0, 1, 1000, Long.MAX_VALUE, null);
        graph.addEdge(1, 2, 1000, Long.MAX_VALUE, null);
        graph.addEdge(2, 3, 1000, Long.MAX_VALUE, null);
        return graph;
    }

    private static EdgeFlow row(double from, double to, double constant) {
        return EdgeFlow.linearized(0, from, to, constant);
    }

    private static boolean inconsistent(GasPressureFlowGraph graph, EdgeFlow[] flows, double tolerance) {
        return GasLinearComponentBalance.isInconsistent(graph.getNodes(), graph.getEdges(), flows, tolerance);
    }
}
