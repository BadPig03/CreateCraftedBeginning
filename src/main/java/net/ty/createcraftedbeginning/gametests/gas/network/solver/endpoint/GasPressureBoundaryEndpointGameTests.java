package net.ty.createcraftedbeginning.gametests.gas.network.solver.endpoint;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointDiscovery.DiscoveredEndpoints;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointDiscovery.PressureBoundaryAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.RoutedPlan;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.EndpointFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.FacePressure;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver.PreparedGraph;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasEndpointTransferPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasRoutedTransferExecutor;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasRoutedTransferExecutor.ExecutedRoute;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPressureBoundaryEndpointGameTests {
    private static final double PRESSURE_TOLERANCE_PA = 1.0E-6;
    private static final BlockPos WEST_PIPE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos EAST_PIPE_POS = new BlockPos(2, 1, 1);
    private static final BlockPos SOURCE_PIPE_POS = new BlockPos(1, 1, 0);

    private GasPressureBoundaryEndpointGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void boundaryFacesKeepIndependentDrainAndFillPermissions(GameTestHelper helper) {
        RecordingBoundary source = new RecordingBoundary(new GasStack(CCBGases.NATURAL_AIR.get(), 80), 80, 0, GasPressure.pascals(4), 1);
        RecordingBoundary target = new RecordingBoundary(GasStack.EMPTY, 0, 80, GasPressure.REFERENCE_PRESSURE_PA, 2);
        List<PressureBoundaryAccess> accesses = placeBoundaryFaces(helper, source, target);
        GasEndpointPlanner planner = GasEndpointPlanner.prepare(new DiscoveredEndpoints(List.of(), accesses, List.of()));
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        List<GasNetworkPressureEndpoint> endpoints = planner.planPressureEndpoints(helper.getLevel(), gas);
        helper.assertValueEqual(endpoints.size(), 2, "Directional boundary endpoint count");
        GasNetworkPressureEndpoint drain = endpointForFace(endpoints, accesses.getFirst().pipeFace(), true);
        GasNetworkPressureEndpoint fill = endpointForFace(endpoints, accesses.getLast().pipeFace(), false);
        helper.assertTrue(drain.access().drainFaces().equals(List.of(accesses.getFirst().pipeFace())), "Drain endpoint opened the fill-only face");
        helper.assertTrue(fill.access().fillFaces().equals(List.of(accesses.getLast().pipeFace())), "Fill endpoint opened the drain-only face");
        helper.assertTrue(drain.access().drainHandler() == source && fill.access().fillHandler() == target, "Directional endpoints replaced their handlers");
        helper.assertValueEqual(drain.flowResistance().drainResistanceUnits(), GasFlowResistance.fromFactor(1), "Drain face resistance");
        helper.assertValueEqual(drain.pressureState().pressurePa(), (double) GasPressure.pascals(4), "Drain face pressure");
        helper.assertValueEqual(executeNetwork(helper, gas, endpoints), 80L, "Directional boundary transfer");
        helper.assertValueEqual(source.drained, 80L, "Gas drained through the source face");
        helper.assertValueEqual(target.received, 80L, "Gas received through the target face");
        helper.assertValueEqual(source.contents.getAmount() + target.contents.getAmount(), 80L, "Directional boundary conservation");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void boundaryFacesKeepTheirPressureResistanceAndFillLimits(GameTestHelper helper) {
        assertIndependentFillConstraints(helper, GasPressure.pascals(4), 70);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void highPressureConvergesWithIndependentBoundaryFillLimits(GameTestHelper helper) {
        assertIndependentFillConstraints(helper, GasPressure.pascals(10), 70);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void lowPressureCannotUseAnotherBoundaryFacesFillPermission(GameTestHelper helper) {
        assertIndependentFillConstraints(helper, GasPressure.pascals(2), 0);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void sharedBoundaryHandlerKeepsOneLimitAcrossMultipleFaces(GameTestHelper helper) {
        assertSharedFillLimit(helper, GasPressure.pascals(2));
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void highPressureConvergesWithSharedBoundaryFillLimit(GameTestHelper helper) {
        assertSharedFillLimit(helper, GasPressure.pascals(10));
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void differentBoundaryHandlersKeepEveryGasCandidate(GameTestHelper helper) {
        RecordingBoundary air = new RecordingBoundary(new GasStack(CCBGases.NATURAL_AIR.get(), 40), 40, 0, GasPressure.pascals(4), 1);
        RecordingBoundary steam = new RecordingBoundary(new GasStack(CCBGases.STEAM.get(), 60), 60, 0, GasPressure.pascals(2), 2);
        List<PressureBoundaryAccess> accesses = placeBoundaryFaces(helper, air, steam);
        GasEndpointPlanner planner = GasEndpointPlanner.prepare(new DiscoveredEndpoints(List.of(), accesses, List.of()));
        List<GasStack> gases = planner.gasGroups();
        helper.assertValueEqual(gases.size(), 2, "Distinct boundary gas candidate count");
        helper.assertTrue(gases.stream().anyMatch(gas -> gas.is(CCBGases.NATURAL_AIR)) && gases.stream().anyMatch(gas -> gas.is(CCBGases.STEAM)), "Boundary grouping discarded a gas candidate");
        for (int index = 0; index < accesses.size(); index++) {
            PressureBoundaryAccess access = accesses.get(index);
            GasStack gas = access.handler().getGasInTank(0).copyWithAmount(1);
            List<GasNetworkPressureEndpoint> endpoints = planner.planPressureEndpoints(helper.getLevel(), gas);
            helper.assertValueEqual(endpoints.size(), 1, "Gas-specific boundary endpoint count for face " + index);
            helper.assertTrue(endpoints.getFirst().access().drainFaces().equals(List.of(access.pipeFace())), "Boundary gas leaked into another face");
        }
        helper.succeed();
    }

    private static void assertSharedFillLimit(GameTestHelper helper, long sourcePressurePa) {
        RecordingBoundary target = new RecordingBoundary(GasStack.EMPTY, 0, 75, GasPressure.REFERENCE_PRESSURE_PA, 1);
        List<PressureBoundaryAccess> accesses = new ArrayList<>(placeBoundaryFaces(helper, target, target));
        GasEndpointPlanner planner = GasEndpointPlanner.prepare(new DiscoveredEndpoints(List.of(), accesses, List.of()));
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        List<GasNetworkPressureEndpoint> targets = planner.planPressureEndpoints(helper.getLevel(), gas);
        helper.assertValueEqual(targets.size(), 1, "Shared boundary endpoint count");
        helper.assertValueEqual(targets.getFirst().access().fillFaces().size(), 2, "Shared boundary connected face count");
        helper.assertValueEqual(targets.getFirst().transferLimits().fillLimit(), 75L, "Shared boundary fill limit");

        RecordingBoundary source = new RecordingBoundary(gas.copyWithAmount(500), 500, 0, sourcePressurePa, 0);
        accesses.add(new PressureBoundaryAccess(new BlockFace(helper.absolutePos(SOURCE_PIPE_POS), Direction.UP), source));
        List<GasNetworkPressureEndpoint> endpoints = GasEndpointPlanner.prepare(new DiscoveredEndpoints(List.of(), accesses, List.of())).planPressureEndpoints(helper.getLevel(), gas);
        helper.assertValueEqual(executeNetwork(helper, gas, endpoints), 75L, "Shared handler transfer was multiplied by its face count");
        helper.assertValueEqual(target.received, 75L, "Shared handler received gas");
        helper.assertValueEqual(source.contents.getAmount() + target.contents.getAmount(), 500L, "Shared handler conservation");
        helper.succeed();
    }

    private static void assertIndependentFillConstraints(GameTestHelper helper, long sourcePressurePa, long expectedEastAmount) {
        RecordingBoundary west = new RecordingBoundary(GasStack.EMPTY, 0, 20, GasPressure.REFERENCE_PRESSURE_PA, 2);
        RecordingBoundary east = new RecordingBoundary(GasStack.EMPTY, 0, 70, GasPressure.pascals(3), 7);
        List<PressureBoundaryAccess> accesses = new ArrayList<>(placeBoundaryFaces(helper, west, east));
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        RecordingBoundary source = new RecordingBoundary(gas.copyWithAmount(500), 500, 0, sourcePressurePa, 0);
        accesses.add(new PressureBoundaryAccess(new BlockFace(helper.absolutePos(SOURCE_PIPE_POS), Direction.UP), source));
        List<GasNetworkPressureEndpoint> endpoints = GasEndpointPlanner.prepare(new DiscoveredEndpoints(List.of(), accesses, List.of())).planPressureEndpoints(helper.getLevel(), gas);
        helper.assertValueEqual(endpoints.size(), 3, "Independent pressure boundary endpoint count");
        for (PressureBoundaryAccess access : accesses.subList(0, 2)) {
            RecordingBoundary boundary = (RecordingBoundary) access.handler();
            GasNetworkPressureEndpoint endpoint = endpointForFace(endpoints, access.pipeFace(), false);
            helper.assertTrue(endpoint.access().fillHandler() == boundary, "Boundary face replaced its fill handler");
            helper.assertTrue(endpoint.access().fillFaces().equals(List.of(access.pipeFace())), "Boundary face inherited another handler's faces");
            helper.assertValueEqual(endpoint.pressureState().pressurePa(), (double) boundary.pressurePa, "Boundary face pressure");
            helper.assertValueEqual(endpoint.flowResistance().fillResistanceUnits(), GasFlowResistance.fromFactor(boundary.resistanceFactor), "Boundary face resistance");
            helper.assertValueEqual(endpoint.transferLimits().fillLimit(), boundary.fillLimit, "Boundary face fill limit");
        }
        helper.assertValueEqual(executeNetwork(helper, gas, endpoints), 20 + expectedEastAmount, "Pressure-constrained routed transfer");
        helper.assertValueEqual(west.received, 20L, "West face received gas through its own handler");
        helper.assertValueEqual(east.received, expectedEastAmount, "East face received gas through its own handler");
        helper.assertValueEqual(source.contents.getAmount() + west.contents.getAmount() + east.contents.getAmount(), 500L, "Independent boundary conservation");
        helper.succeed();
    }

    private static List<PressureBoundaryAccess> placeBoundaryFaces(GameTestHelper helper, GasPressureBoundary west, GasPressureBoundary east) {
        helper.setBlock(WEST_PIPE_POS, encasedPipe(Direction.EAST, Direction.NORTH));
        helper.setBlock(WEST_PIPE_POS.north(), encasedPipe(Direction.SOUTH, Direction.EAST));
        helper.setBlock(SOURCE_PIPE_POS, encasedPipe(Direction.WEST, Direction.EAST, Direction.UP));
        helper.setBlock(EAST_PIPE_POS.north(), encasedPipe(Direction.WEST, Direction.SOUTH));
        helper.setBlock(EAST_PIPE_POS, encasedPipe(Direction.NORTH, Direction.WEST));
        return List.of(new PressureBoundaryAccess(new BlockFace(helper.absolutePos(WEST_PIPE_POS), Direction.EAST), west), new PressureBoundaryAccess(new BlockFace(helper.absolutePos(EAST_PIPE_POS), Direction.WEST), east));
    }

    private static BlockState encasedPipe(Direction... faces) {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.getDefaultState();
        List<Direction> connectedFaces = List.of(faces);
        for (Direction direction : Iterate.directions) {
            state = state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction), connectedFaces.contains(direction));
        }
        return state;
    }

    private static GasNetworkPressureEndpoint endpointForFace(List<GasNetworkPressureEndpoint> endpoints, BlockFace face, boolean draining) {
        for (GasNetworkPressureEndpoint endpoint : endpoints) {
            List<BlockFace> faces = draining ? endpoint.access().drainFaces() : endpoint.access().fillFaces();
            if (!faces.contains(face)) {
                continue;
            }

            return endpoint;
        }
        throw new IllegalStateException("Pressure boundary endpoint was not found for " + face + "; draining=" + draining + '.');
    }

    private static long executeNetwork(GameTestHelper helper, GasStack gas, List<GasNetworkPressureEndpoint> endpoints) {
        ServerLevel level = helper.getLevel();
        PreparedGraph graph = GasPressureGraphSolver.prepare(level, GasNetworkTopology.get(level, helper.absolutePos(SOURCE_PIPE_POS)), gas);
        GasTransportFlowBudget budget = new GasTransportFlowBudget();
        List<GasPressureGraphSolution> solutions = List.of(graph.solve(endpoints, budget, true), graph.solveHypothetical(endpoints, budget), graph.solve(endpoints, budget, true));
        double pressureCeiling = endpoints.stream().mapToDouble(endpoint -> endpoint.pressureState().pressurePa()).max().orElseThrow();
        for (GasPressureGraphSolution solution : solutions) {
            helper.assertTrue(solution.converged() && !solution.isEmpty(), "Boundary pressure graph failed during cold, hypothetical or warm solve");
            for (FacePressure pressure : solution.facePressures()) {
                double pressurePa = pressure.pressurePa();
                helper.assertTrue(Double.isFinite(pressurePa) && pressurePa >= 0 && pressurePa <= pressureCeiling + PRESSURE_TOLERANCE_PA, "Passive pipe pressure exceeded its boundary pressure range");
            }
            for (EndpointFlow flow : solution.endpointFlows()) {
                double pressurePa = flow.manifoldPressurePa();
                helper.assertTrue(Double.isFinite(pressurePa) && pressurePa >= 0 && pressurePa <= pressureCeiling + PRESSURE_TOLERANCE_PA, "Endpoint manifold pressure exceeded its boundary pressure range");
            }
        }
        long transferred = 0;
        for (TransferComponent component : solutions.getFirst().transferComponents()) {
            GasTransferPlan plan = GasEndpointTransferPlanner.plan(level, gas, component, 1, 0);
            RoutedPlan routed = component.routing().route(plan, budget);
            for (ExecutedRoute execution : GasRoutedTransferExecutor.execute(level, gas, routed, budget)) {
                transferred += execution.amount();
            }
        }
        return transferred;
    }

    private static final class RecordingBoundary implements GasPressureBoundary {
        private final long drainLimit;
        private final long fillLimit;
        private final long pressurePa;
        private final double resistanceFactor;
        private GasStack contents;
        private long drained;
        private long received;

        private RecordingBoundary(GasStack contents, long drainLimit, long fillLimit, long pressurePa, double resistanceFactor) {
            this.contents = contents.copy();
            this.drainLimit = drainLimit;
            this.fillLimit = fillLimit;
            this.pressurePa = pressurePa;
            this.resistanceFactor = resistanceFactor;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && !stack.isEmpty() && (contents.isEmpty() || GasStack.isSameGasSameComponents(contents, stack));
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (contents.isEmpty() || !GasStack.isSameGasSameComponents(contents, resource)) {
                return GasStack.EMPTY;
            }

            long amount = Math.min(resource.getAmount(), Math.min(contents.getAmount(), drainLimit));
            GasStack drainedGas = contents.copyWithAmount(amount);
            if (action.execute()) {
                contents.shrink(amount);
                drained += amount;
            }
            return drainedGas;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return drain(contents.copyWithAmount(maxDrain), action);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return contents.copy();
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return fillFromPressure(resource, GasPressure.REFERENCE_PRESSURE_PA, action);
        }

        @Override
        public long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
            if (!isGasValid(0, resource) || sourcePressurePa < pressurePa) {
                return 0;
            }

            long accepted = Math.min(resource.getAmount(), fillLimit);
            if (action.execute() && accepted > 0) {
                if (contents.isEmpty()) {
                    contents = resource.copyWithAmount(accepted);
                }
                else {
                    contents.grow(accepted);
                }
                received += accepted;
            }
            return accepted;
        }

        @Override
        public long getDrainPressurePa(int tank, GasStack gas) {
            return pressurePa;
        }

        @Override
        public long getFillPressurePa(int tank, GasStack gas) {
            return pressurePa;
        }

        @Override
        public double getDrainFlowResistanceFactor(int tank, GasStack gas) {
            return resistanceFactor;
        }

        @Override
        public double getFillFlowResistanceFactor(int tank, GasStack gas) {
            return resistanceFactor;
        }
    }
}
