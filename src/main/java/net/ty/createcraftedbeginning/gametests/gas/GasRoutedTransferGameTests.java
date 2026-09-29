package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.Recovery;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferLimits;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.Edge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.RoutedPlan;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.EndpointFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PipeSegment;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasQuantizedEquilibriumPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasRoutedTransferExecutor;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasRoutedTransferExecutor.ExecutedRoute;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedFill;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasRoutedTransferGameTests {
    private static final BlockPos COMMON = new BlockPos(0, 1, 1);
    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(1, 1, 2);

    private GasRoutedTransferGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void failedHypotheticalSolveCannotCountAsEquilibrium(GameTestHelper helper) {
        GasNetworkPressureEndpoint source = endpoint(tank(100));
        GasPressureGraphSolution current = new GasPressureGraphSolution(List.of(new TransferComponent(10, List.of(new EndpointFlow(source, 10, 0, 0)), List.of(), Map.of(), new GasFlowRouting(Map.of(), List.of()))), List.of());
        helper.assertTrue(!GasQuantizedEquilibriumPlanner.improvesFlowNorm(current, GasPressureGraphSolution.FAILED), "Failed solve was accepted as zero-flow equilibrium");
        helper.assertTrue(!GasQuantizedEquilibriumPlanner.improvesFlowNorm(GasPressureGraphSolution.FAILED, GasPressureGraphSolution.EMPTY), "Failed current solution admitted a candidate");
        helper.assertTrue(GasQuantizedEquilibriumPlanner.improvesFlowNorm(current, GasPressureGraphSolution.EMPTY), "A genuinely converged zero-flow solution was rejected");
        helper.assertTrue(!GasQuantizedEquilibriumPlanner.improvesFlowNorm(current, current), "Unchanged flow was accepted as an improvement");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void partiallyAcceptingBranchIsChargedOnlyForItsOwnGas(GameTestHelper helper) {
        assertForkUnderfill(helper, 20);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void rejectedBranchLeavesItsBudgetAndTelemetryUnused(GameTestHelper helper) {
        assertForkUnderfill(helper, 0);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void partialSourceDrainDoesNotScaleUnrelatedSourceBranches(GameTestHelper helper) {
        GasTank firstTank = new GasTank(1000) {
            @Override
            public GasStack drain(GasStack resource, GasAction action) {
                if (!action.execute()) {
                    return super.drain(resource, action);
                }

                return super.drain(resource.copyWithAmount(Math.min(20, resource.getAmount())), action);
            }
        };
        firstTank.tryReplaceContents(gas(60)).requireAccepted();
        GasTank secondTank = tank(40);
        GasTank sinkTank = tank(0);
        GasNetworkPressureEndpoint first = endpoint(firstTank);
        GasNetworkPressureEndpoint second = endpoint(secondTank);
        GasNetworkPressureEndpoint sink = endpoint(sinkTank);
        GasFlowRouting routing = new GasFlowRouting(Map.of(first, 0, second, 1, sink, 3), List.of(edge(0, 2, 60, FIRST), edge(1, 2, 40, SECOND), edge(2, 3, 100, COMMON)));
        GasTransferPlan plan = new GasTransferPlan(List.of(new PlannedDrain(first, 60), new PlannedDrain(second, 40)), List.of(new PlannedFill(sink, 100, -1)), 100);
        GasTransportFlowBudget budget = budget(60, 40);
        List<ExecutedRoute> execution = GasRoutedTransferExecutor.execute(helper.getLevel(), gas(1), routing.route(plan, budget), budget);
        helper.assertValueEqual(firstTank.getStoredAmount(), 40L, "partially drained source amount");
        helper.assertValueEqual(secondTank.getStoredAmount(), 0L, "fully drained source amount");
        helper.assertValueEqual(sinkTank.getStoredAmount(), 60L, "merged actual fill");
        assertSegmentAmount(helper, execution, FIRST, 20);
        assertSegmentAmount(helper, execution, SECOND, 40);
        assertSegmentAmount(helper, execution, COMMON, 60);
        helper.assertValueEqual(budget.remainingKnown(FIRST), 40L, "partial source pump remainder");
        helper.assertValueEqual(budget.remainingKnown(SECOND), 0L, "other source pump remainder");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void choosingOneSinkCannotBorrowAnotherBranchPumpBudget(GameTestHelper helper) {
        GasNetworkPressureEndpoint source = endpoint(tank(100));
        GasNetworkPressureEndpoint first = endpoint(tank(0));
        GasNetworkPressureEndpoint second = endpoint(tank(0));
        GasFlowRouting routing = new GasFlowRouting(Map.of(source, 0, first, 2, second, 3), List.of(edge(0, 1, 100, COMMON), edge(1, 2, 10, FIRST), edge(1, 3, 90, SECOND)));
        GasTransferPlan plan = new GasTransferPlan(List.of(new PlannedDrain(source, 80)), List.of(new PlannedFill(first, 80, -1)), 80);
        GasTransportFlowBudget budget = budget(10, 90);
        RoutedPlan routed = routing.route(plan, budget);
        helper.assertValueEqual(routed.totalAmount(), 10L, "selected branch limit instead of component average");
        List<ExecutedRoute> execution = GasRoutedTransferExecutor.execute(helper.getLevel(), gas(1), routed, budget);
        assertSegmentAmount(helper, execution, FIRST, 10);
        assertSegmentAmount(helper, execution, SECOND, 0);
        helper.assertValueEqual(budget.remainingKnown(FIRST), 0L, "selected pump exhausted");
        helper.assertValueEqual(budget.remainingKnown(SECOND), 90L, "unselected pump budget preserved");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void parallelPathsKeepTheirSharesAndRecheckBudgetBeforeExecution(GameTestHelper helper) {
        GasNetworkPressureEndpoint source = endpoint(tank(100));
        GasNetworkPressureEndpoint sink = endpoint(tank(0));
        GasFlowRouting routing = new GasFlowRouting(Map.of(source, 0, sink, 1), List.of(edge(0, 1, 60, FIRST), edge(0, 1, 40, SECOND)));
        GasTransferPlan plan = new GasTransferPlan(List.of(new PlannedDrain(source, 100)), List.of(new PlannedFill(sink, 100, -1)), 100);
        GasTransportFlowBudget budget = budget(60, 40);
        RoutedPlan routed = routing.route(plan, budget);
        helper.assertValueEqual(routed.totalAmount(), 100L, "parallel routed total");
        budget.consume(FIRST, 50);
        List<ExecutedRoute> execution = GasRoutedTransferExecutor.execute(helper.getLevel(), gas(1), routed, budget);
        assertSegmentAmount(helper, execution, FIRST, 10);
        assertSegmentAmount(helper, execution, SECOND, 40);
        helper.assertValueEqual(budget.remainingKnown(FIRST), 0L, "changed budget rechecked before drain");
        helper.succeed();
    }

    private static void assertForkUnderfill(GameTestHelper helper, long accepted) {
        GasTank sourceTank = tank(100);
        GasTank firstTank = new GasTank(1000) {
            @Override
            public long fill(GasStack resource, GasAction action) {
                if (!action.execute()) {
                    return super.fill(resource, action);
                }

                return super.fill(resource.copyWithAmount(Math.min(accepted, resource.getAmount())), action);
            }
        };
        GasTank secondTank = tank(0);
        GasNetworkPressureEndpoint source = endpoint(sourceTank);
        GasNetworkPressureEndpoint first = endpoint(firstTank);
        GasNetworkPressureEndpoint second = endpoint(secondTank);
        GasFlowRouting routing = new GasFlowRouting(Map.of(source, 0, first, 2, second, 3), List.of(edge(0, 1, 100, COMMON), edge(1, 2, 60, FIRST), edge(1, 3, 40, SECOND)));
        GasTransferPlan plan = new GasTransferPlan(List.of(new PlannedDrain(source, 100)), List.of(new PlannedFill(first, 60, -1), new PlannedFill(second, 40, -1)), 100);
        GasTransportFlowBudget budget = budget(60, 40);
        RoutedPlan routed = routing.route(plan, budget);
        helper.assertValueEqual(routed.totalAmount(), 100L, "fully routable fork plan");
        List<ExecutedRoute> execution = GasRoutedTransferExecutor.execute(helper.getLevel(), gas(1), routed, budget);
        helper.assertValueEqual(firstTank.getStoredAmount(), accepted, "partial branch received amount");
        helper.assertValueEqual(secondTank.getStoredAmount(), 40L, "successful branch received amount");
        helper.assertValueEqual(sourceTank.getStoredAmount(), 60 - accepted, "unused gas restored to its source");
        assertSegmentAmount(helper, execution, COMMON, accepted + 40);
        assertSegmentAmount(helper, execution, FIRST, accepted);
        assertSegmentAmount(helper, execution, SECOND, 40);
        helper.assertValueEqual(budget.remainingKnown(COMMON), 60 - accepted, "shared pump remainder");
        helper.assertValueEqual(budget.remainingKnown(FIRST), 60 - accepted, "partial branch pump remainder");
        helper.assertValueEqual(budget.remainingKnown(SECOND), 0L, "successful branch pump remainder");
        helper.succeed();
    }

    private static void assertSegmentAmount(GameTestHelper helper, List<ExecutedRoute> execution, BlockPos pos, long expected) {
        long amount = 0;
        for (ExecutedRoute transfer : execution) {
            for (PipeSegment segment : transfer.route().segments()) {
                if (!(segment.pos().equals(pos))) {
                    continue;
                }

                amount += transfer.amount();
            }
        }
        helper.assertValueEqual(amount, expected, "exact segment amount at " + pos);
    }

    private static Edge edge(int from, int to, double rate, BlockPos pos) {
        return new Edge(from, to, rate, new PipeSegment(pos, Direction.WEST, Direction.EAST), pos);
    }

    private static GasTransportFlowBudget budget(long first, long second) {
        GasTransportFlowBudget budget = new GasTransportFlowBudget();
        budget.remaining(COMMON, 100);
        budget.remaining(FIRST, first);
        budget.remaining(SECOND, second);
        return budget;
    }

    private static GasTank tank(long amount) {
        GasTank tank = new GasTank(1000);
        tank.tryReplaceContents(gas(amount)).requireAccepted();
        return tank;
    }

    private static GasStack gas(long amount) {
        return new GasStack(CCBGases.NATURAL_AIR.get(), amount);
    }

    private static GasNetworkPressureEndpoint endpoint(GasTank tank) {
        return new GasNetworkPressureEndpoint(new TransferAccess(tank, tank, List.of(), List.of()), new PressureState(tank.getPressurePa(), false, tank.getStoredAmount(), tank.getVolume(), GasPressure.REFERENCE_PRESSURE_PA, tank.getMaxAmount()), new TransferLimits(tank.getStoredAmount(), tank.getMaxAmount() - tank.getStoredAmount()), new Recovery(List.of(tank), null, null, false));
    }
}
