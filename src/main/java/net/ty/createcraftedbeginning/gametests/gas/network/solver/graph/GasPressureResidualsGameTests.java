package net.ty.createcraftedbeginning.gametests.gas.network.solver.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureResiduals;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPressureResidualsGameTests {
    private GasPressureResidualsGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void residualChecksStayIndependentAfterResizeAndFailure(GameTestHelper helper) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        graph.addUnknownNode(100000);
        graph.addFixedNode(100000);
        graph.addEdge(0, 1, 1000, Long.MAX_VALUE, null);
        graph.captureStaticState();
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        assertResiduals(helper, residuals, new EdgeFlow[]{flow(3)}, 3, 3);

        int temporary = graph.addUnknownNode(100000);
        graph.addEdge(0, temporary, 1000, Long.MAX_VALUE, null);
        EdgeFlow[] expanded = {flow(3), flow(7)};
        assertResiduals(helper, residuals, expanded, 10, 17);

        EdgeFlow[] invalid = {flow(3), flow(Double.NaN)};
        helper.assertTrue(!residuals.calculateUnknownResidual(invalid, false).converged(1.0E-8), "Nonfinite actual flow must fail");
        helper.assertTrue(!residuals.calculateUnknownResidual(invalid, true).converged(1.0E-8), "Nonfinite linear flow must fail");
        assertResiduals(helper, residuals, expanded, 10, 17);

        graph.resetDynamicState(null);
        assertResiduals(helper, residuals, new EdgeFlow[]{flow(3)}, 3, 3);
        temporary = graph.addUnknownNode(100000);
        graph.addEdge(0, temporary, 1000, Long.MAX_VALUE, null);
        assertResiduals(helper, residuals, new EdgeFlow[]{flow(4), flow(2)}, 6, 8);
        assertResiduals(helper, residuals, new EdgeFlow[]{flow(0), flow(0)}, 0, 0);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void linearConvergencePreservesToleranceAndNonfiniteRejection(GameTestHelper helper) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        graph.addUnknownNode(1.0E12);
        graph.addFixedNode(1.0E12);
        graph.addEdge(0, 1, 1000, Long.MAX_VALUE, null);
        graph.addEdge(0, 1, 1000, Long.MAX_VALUE, null);
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        EdgeFlow cancelling = EdgeFlow.linearized(0, 1, 1, 0);

        helper.assertTrue(residuals.linearizedFlowsConverged(new EdgeFlow[]{cancelling, flow(1)}), "Cancelling flow was omitted from the relative tolerance");
        helper.assertTrue(!residuals.linearizedFlowsConverged(new EdgeFlow[]{cancelling, flow(201)}), "Residual above relative tolerance was accepted");

        graph.setNodePressure(0, 0);
        graph.setNodePressure(1, 0);
        helper.assertTrue(residuals.linearizedFlowsConverged(new EdgeFlow[]{flow(0), flow(5.0E-9)}), "Absolute tolerance was lost");
        helper.assertTrue(!residuals.linearizedFlowsConverged(new EdgeFlow[]{flow(0), flow(2.0E-8)}), "Residual above absolute tolerance was accepted");
        helper.assertTrue(!residuals.linearizedFlowsConverged(new EdgeFlow[]{flow(Double.MAX_VALUE), flow(Double.MAX_VALUE)}), "Overflowed node residual was accepted");

        graph.setNodePressure(0, 1.0E308);
        graph.setNodePressure(1, 1.0E308);
        helper.assertTrue(!residuals.linearizedFlowsConverged(new EdgeFlow[]{cancelling, flow(1)}), "Overflowed scale widened the tolerance");
        helper.assertTrue(residuals.linearizedFlowsConverged(new EdgeFlow[]{cancelling, flow(5.0E-9)}), "Overflowed scale incorrectly rejected an acceptable residual");
        helper.assertTrue(!residuals.linearizedFlowsConverged(new EdgeFlow[]{flow(1), flow(Double.NaN)}), "Nonfinite flow was accepted");
        helper.assertTrue(!residuals.linearizedFlowsConverged(new EdgeFlow[]{flow(1), flow(Double.POSITIVE_INFINITY)}), "Infinite flow was accepted");
        helper.assertTrue(residuals.linearizedFlowsConverged(new EdgeFlow[]{cancelling, flow(0)}), "Failed check contaminated the next convergence check");
        helper.succeed();
    }

    private static EdgeFlow flow(double rate) {
        return new EdgeFlow(rate, 0, 0, rate);
    }

    private static void assertResiduals(GameTestHelper helper, GasPressureResiduals residuals, EdgeFlow[] flows, double maximum, double norm) {
        helper.assertTrue(residuals.calculateUnknownResidual(flows, false).maxResidual() == maximum, "Actual maximum residual");
        helper.assertTrue(residuals.nonlinearResidualNorm(flows) == norm, "Actual residual norm");
        helper.assertTrue(residuals.calculateUnknownResidual(flows, true).maxResidual() == maximum, "Linear maximum residual after actual checks");
    }
}
