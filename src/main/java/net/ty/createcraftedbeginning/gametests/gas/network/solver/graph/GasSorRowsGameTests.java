package net.ty.createcraftedbeginning.gametests.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasLinearPressureSolver;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.SolverWork;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureResiduals;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasSorRowsGameTests {
    private GasSorRowsGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void sorRowsPreserveDirectionsAndRefreshBetweenLinearizations(GameTestHelper helper) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        graph.addUnknownNode(0);
        graph.addUnknownNode(0);
        graph.addFixedNode(0);
        graph.addEdge(0, 2, 1000, Long.MAX_VALUE, null);
        graph.addEdge(2, 1, 1000, Long.MAX_VALUE, null);
        graph.addEdge(1, 2, 1000, Long.MAX_VALUE, null);
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        GasLinearPressureSolver solver = new GasLinearPressureSolver(graph, residuals);

        EdgeFlow[] first = {row(1, 1), row(1, 6), row(1, 2)};
        assertSolve(helper, graph, solver, residuals, first, 2);

        graph.addFixedNode(10);
        graph.addEdge(3, 1, 1000, Long.MAX_VALUE, null);
        graph.addEdge(1, 2, 1000, Long.MAX_VALUE, null);
        EdgeFlow[] second = {row(1, 1), row(1, 10), row(1, 4), row(2, 0), row(1, 0)};
        assertSolve(helper, graph, solver, residuals, second, 3.2);
        helper.assertTrue(graph.getNodePressure(3) == 10, "SOR changed the added pressure boundary");
        helper.succeed();
    }

    private static EdgeFlow row(double to, double constant) {
        return EdgeFlow.linearized(0, 1, to, constant);
    }

    private static void assertSolve(GameTestHelper helper, GasPressureFlowGraph graph, GasLinearPressureSolver solver, GasPressureResiduals residuals, EdgeFlow[] flows, double expectedPressure) {
        SolverWork work = new SolverWork();
        solver.solve(flows, work);
        helper.assertTrue(work.getSorSweeps() > 0, "Fixture did not exercise SOR");
        helper.assertTrue(Math.abs(graph.getNodePressure(1) - expectedPressure) < 1.0E-9, "SOR did not preserve the hand-calculated row balance");
        helper.assertTrue(graph.getNodePressure(0) == 0, "SOR accepted a negative pressure");
        helper.assertTrue(graph.getNodePressure(2) == 0, "SOR changed the original pressure boundary");
        helper.assertTrue(!residuals.calculateUnknownResidual(flows, true).converged(1.0E-8), "Unmatched constant outflow was mistaken for equilibrium");
    }
}
