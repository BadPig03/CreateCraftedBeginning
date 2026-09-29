package net.ty.createcraftedbeginning.gametests.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeConstraintState;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.PressureNode;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.SolverWork;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureResiduals;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPumpEquilibriumGameTests {
    private GasPumpEquilibriumGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void exactPumpVacuumEquilibriumSurvivesRepeatedSolves(GameTestHelper helper) {
        GasPressureFlowGraph graph = graph(0, GasPressure.REFERENCE_PRESSURE_PA);
        for (int attempt = 0; attempt < 3; attempt++) {
            FlowGraphSolution solution = graph.solve();
            helper.assertTrue(solution.converged(), "Exact pump equilibrium was lost");
            helper.assertTrue(Arrays.stream(solution.flowRates()).allMatch(flow -> flow == 0), "Static equilibrium produced gas flow");
            helper.assertValueEqual(graph.getNodePressure(0), 0.0, "vacuum inlet pressure");
            helper.assertValueEqual(graph.getNodePressure(1), (double) GasPressure.REFERENCE_PRESSURE_PA, "ambient outlet pressure");
            SolverWork work = solution.work();
            helper.assertValueEqual(work.getRegularizedSolves(), 0, "exact equilibrium recovery count");
            helper.assertValueEqual(work.getSorSweeps(), 0, "exact equilibrium SOR work");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void coldAndChangedPumpGraphsRevalidateActualBalance(GameTestHelper helper) {
        GasPressureFlowGraph graph = graph(-1, -1);
        assertConverged(helper, graph);
        graph.addEdge(1, 0, pump(), null, null);
        FlowGraphSolution circulating = assertConverged(helper, graph);
        helper.assertTrue(circulating.flowRate(0) > 0 && circulating.flowRate(4) > 0, "Added reverse pump was ignored as a stale zero-flow equilibrium");
        int[] incidentCounts = graph.getIncidentEdgeCounts();
        FlowGraphSolution warm = assertConverged(helper, graph);
        helper.assertValueEqual(warm.work().getRegularizedSolves(), 0, "converged circulation repeated recovery");
        helper.assertTrue(Arrays.equals(graph.getIncidentEdgeCounts(), incidentCounts), "Recovery leaked graph adjacency entries");
        helper.assertValueEqual(graph.getNodes().size(), 4, "permanent pressure node count");
        helper.assertValueEqual(graph.getEdges().size(), 5, "permanent pressure edge count");
        helper.succeed();
    }

    private static GasPressureFlowGraph graph(double inletPressure, double outletPressure) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        graph.addUnknownNode(inletPressure);
        graph.addUnknownNode(outletPressure);
        graph.addFixedNode(GasPressure.pascals(16));
        graph.addFixedNode(GasPressure.REFERENCE_PRESSURE_PA);
        graph.addEdge(0, 1, pump(), null, null);
        graph.addEdge(0, 2, 1000000, Long.MAX_VALUE, null);
        graph.addEdge(1, 3, 1000000, Long.MAX_VALUE, null);
        graph.addEdge(3, 1, 1000000, Long.MAX_VALUE, null);
        return graph;
    }

    private static GasTransportEdgeProperties pump() {
        return new GasTransportEdgeProperties(1000000, GasTransportPressureDrive.pressureBoost(GasPressure.pascals(4)), 4000, GasPressure.REFERENCE_PRESSURE_PA, 0.01, true, true);
    }

    private static FlowGraphSolution assertConverged(GameTestHelper helper, GasPressureFlowGraph graph) {
        FlowGraphSolution solution = graph.solve();
        helper.assertTrue(solution.converged(), "Pump network did not converge");
        List<FlowEdge> edges = graph.getEdges();
        List<PressureNode> nodes = graph.getNodes();
        EdgeFlow[] actual = new EdgeFlow[edges.size()];
        EdgeFlow[] linearized = new EdgeFlow[actual.length];
        for (int i = 0; i < actual.length; i++) {
            FlowEdge edge = edges.get(i);
            actual[i] = edge.calculate(nodes);
            linearized[i] = edge.linearize(nodes, edge.nextConstraintState(nodes, EdgeConstraintState.LINEAR));
            helper.assertTrue(Math.abs(actual[i].flowRate() - solution.flowRate(i)) < 1.0E-8, "Reported flow differs from the pump's actual constraints");
        }
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        helper.assertTrue(residuals.calculateUnknownResidual(actual, false).converged(residuals.flowResidualTolerance(linearized)), "Pump solution violates actual flow balance");
        return solution;
    }
}
