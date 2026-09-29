package net.ty.createcraftedbeginning.gametests.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasDensePressureSolver;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasDensePressureSolverGameTests {
    private GasDensePressureSolverGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void asymmetricFlowsPreserveNodeBalance(GameTestHelper helper) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        int source = graph.addFixedNode(10);
        int first = graph.addUnknownNode(0);
        int second = graph.addUnknownNode(0);
        int sink = graph.addFixedNode(0);
        graph.addEdge(source, first, 1, Long.MAX_VALUE, null);
        graph.addEdge(first, second, 1, Long.MAX_VALUE, null);
        graph.addEdge(second, sink, 1, Long.MAX_VALUE, null);
        EdgeFlow[] flows = {EdgeFlow.linearized(0, 2, 1, 1), new EdgeFlow(0, 1, 3, -2), EdgeFlow.linearized(0, 4, 4, 0)};
        helper.assertTrue(new GasDensePressureSolver(graph).projectPressures(flows), "Asymmetric anchored system was rejected");
        double firstPressure = graph.getNodePressure(first);
        double secondPressure = graph.getNodePressure(second);
        helper.assertTrue(Math.abs(firstPressure - 155.0 / 11) < 1.0E-10 && Math.abs(secondPressure - 19.0 / 11) < 1.0E-10, "Asymmetric pressure solution is incorrect");
        double incoming = 21 - firstPressure;
        double internal = firstPressure - 3 * secondPressure - 2;
        double outgoing = 4 * secondPressure;
        helper.assertTrue(Math.abs(incoming - internal) < 1.0E-10 && Math.abs(internal - outgoing) < 1.0E-10, "Asymmetric solution violates node balance");
        helper.assertValueEqual(graph.getNodePressure(source), 10.0, "fixed source pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void rejectedSystemsDoNotPartiallyChangePressures(GameTestHelper helper) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        graph.addUnknownNode(7);
        graph.addUnknownNode(9);
        int fixed = graph.addFixedNode(10);
        graph.addEdge(0, fixed, 1, Long.MAX_VALUE, null);
        GasDensePressureSolver solver = new GasDensePressureSolver(graph);
        helper.assertTrue(!solver.projectPressures(new EdgeFlow[]{EdgeFlow.linearized(0, 1, 1, 0)}), "Singular system was accepted");
        helper.assertValueEqual(graph.getNodePressure(0), 7.0, "pressure before a singular row");
        graph.addEdge(1, fixed, 1, Long.MAX_VALUE, null);
        helper.assertTrue(!solver.projectPressures(new EdgeFlow[]{EdgeFlow.linearized(0, 1, 1, 0), EdgeFlow.linearized(0, Double.NaN, 1, 0)}), "Non-finite system was accepted");
        helper.assertValueEqual(graph.getNodePressure(0), 7.0, "pressure before a non-finite row");
        helper.assertValueEqual(graph.getNodePressure(1), 9.0, "rejected second pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void projectedCandidateAndLargeGraphKeepTheirBoundaries(GameTestHelper helper) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        graph.addUnknownNode(7);
        int fixed = graph.addFixedNode(0);
        graph.addEdge(0, fixed, 1, Long.MAX_VALUE, null);
        GasDensePressureSolver solver = new GasDensePressureSolver(graph);
        helper.assertTrue(solver.projectPressures(new EdgeFlow[]{EdgeFlow.linearized(0, 1, 1, 1)}), "Finite candidate was rejected");
        helper.assertValueEqual(graph.getNodePressure(0), 0.0, "non-negative projected pressure");
        for (int i = 1; i < 129; i++) {
            graph.addUnknownNode(7);
        }
        helper.assertTrue(!solver.projectPressures(new EdgeFlow[]{EdgeFlow.linearized(0, 1, 1, -5)}), "Dense solver accepted more than 128 unknown pressures");
        helper.assertValueEqual(graph.getNodePressure(0), 0.0, "pressure after large graph rejection");
        helper.assertValueEqual(graph.getNodePressure(129), 7.0, "last untouched pressure");
        helper.succeed();
    }
}
