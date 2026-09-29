package net.ty.createcraftedbeginning.gametests.gas;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightFlowmeterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.Capture;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.NetworkProfile;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.FacePressure;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasSolverCharacterizationGameTests {
    private static final BlockPos SINGLE_SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos SINGLE_PIPE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SINGLE_SINK_POS = new BlockPos(2, 1, 1);

    private static final BlockPos MULTI_SOURCE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos FIRST_MULTI_PIPE_POS = new BlockPos(2, 1, 1);
    private static final BlockPos MULTI_SINK_POS = new BlockPos(5, 1, 1);
    private static final int MULTI_PIPE_COUNT = 3;
    private static final int PROFILE_SAMPLE_TICKS = 8;
    private static final int PROFILE_WARMUP_TICKS = 2;
    private static final int PROFILE_ENDPOINT_FACE_COUNT = 2;
    private static final int PROFILE_PRESSURE_ENDPOINT_COUNT = 2;
    private static final int PROFILE_NODE_COUNT = 10;
    private static final int PROFILE_EDGE_COUNT = 18;
    private static final int PROFILE_ACTIVE_SET_ITERATIONS = 0;

    private static final long SOURCE_PRESSURE_PA = GasPressure.pascals(2);
    private static final long SINK_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final long ENDPOINT_RESISTANCE_UNITS = GasFlowResistance.UNITS_PER_STANDARD_SEGMENT / 50;
    private static final long JUNCTION_RESISTANCE_UNITS = GasFlowResistance.UNITS_PER_STANDARD_SEGMENT / 1000;
    private static final double PRESSURE_TOLERANCE_PA = 1;
    private static final double MIN_FLOW_TOLERANCE = 1.0E-4;

    private GasSolverCharacterizationGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void singlePipePressureSolutionMatchesCurrentResistanceModel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeBoundaryTank(helper, SINGLE_SOURCE_POS, SOURCE_PRESSURE_PA);
        helper.setBlock(SINGLE_PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        placeBoundaryTank(helper, SINGLE_SINK_POS, SINK_PRESSURE_PA);

        helper.succeedWhen(() -> {
            BlockPos absolutePipePos = helper.absolutePos(SINGLE_PIPE_POS);
            Snapshot topology = GasNetworkTopology.get(level, absolutePipePos);
            helper.assertValueEqual(topology.pipePositions().size(), 1, "single-pipe characterization topology size");
            helper.assertValueEqual(topology.endpointFaces().size(), 2, "single-pipe characterization endpoint face count");

            GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
            GasEndpointPlanner endpointPlanner = GasEndpointPlanner.prepare(level, topology);
            List<GasNetworkPressureEndpoint> endpoints = endpointPlanner.planPressureEndpoints(level, gas);
            helper.assertValueEqual(endpoints.size(), 2, "single-pipe characterization pressure endpoint count");

            GasPressureGraphSolution solution = GasPressureGraphSolver.prepare(level, topology, gas).solve(endpoints, new GasTransportFlowBudget());
            helper.assertValueEqual(solution.transferComponents().size(), 1, "single-pipe characterization transfer component count");
            if (solution.transferComponents().size() != 1) {
                return;
            }

            long pipeResistanceUnits = 958000;
            long totalResistanceUnits = pipeResistanceUnits + 2 * ENDPOINT_RESISTANCE_UNITS + 2 * JUNCTION_RESISTANCE_UNITS;
            double pressureDifferencePa = SOURCE_PRESSURE_PA - SINK_PRESSURE_PA;
            double expectedFlowRate = 1000;
            double endpointSideResistanceUnits = ENDPOINT_RESISTANCE_UNITS + JUNCTION_RESISTANCE_UNITS;
            double expectedEndPressureDropPa = pressureDifferencePa * endpointSideResistanceUnits / totalResistanceUnits;
            double expectedWestPressurePa = SOURCE_PRESSURE_PA - expectedEndPressureDropPa;
            double expectedEastPressurePa = SINK_PRESSURE_PA + expectedEndPressureDropPa;

            TransferComponent component = solution.transferComponents().getFirst();
            assertClose(helper, component.referenceFlowRate(), expectedFlowRate, MIN_FLOW_TOLERANCE, "single-pipe continuous flow rate");

            double westPressurePa = facePressure(solution, new BlockFace(absolutePipePos, Direction.WEST));
            double eastPressurePa = facePressure(solution, new BlockFace(absolutePipePos, Direction.EAST));
            assertClose(helper, westPressurePa, expectedWestPressurePa, PRESSURE_TOLERANCE_PA, "single-pipe west face pressure");
            assertClose(helper, eastPressurePa, expectedEastPressurePa, PRESSURE_TOLERANCE_PA, "single-pipe east face pressure");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void singleFlowmeterTransfersThousandUnitsAtHighAbsolutePressure(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeBoundaryTank(helper, SINGLE_SOURCE_POS, GasPressure.pascals(16));
        helper.setBlock(SINGLE_PIPE_POS, CCBBlocks.AIRTIGHT_FLOWMETER_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        placeBoundaryTank(helper, SINGLE_SINK_POS, GasPressure.pascals(15));
        CreativeAirtightTankBlockEntity sink = (CreativeAirtightTankBlockEntity) level.getBlockEntity(helper.absolutePos(SINGLE_SINK_POS));
        helper.assertTrue(sink != null, "Creative sink was not initialized");
        if (sink == null) {
            return;
        }

        sink.getTankInventory().setContainedGas(GasStack.EMPTY);
        helper.succeedWhen(() -> {
            BlockPos pipePos = helper.absolutePos(SINGLE_PIPE_POS);
            AirtightFlowmeterBlockEntity meter = (AirtightFlowmeterBlockEntity) level.getBlockEntity(pipePos);
            helper.assertTrue(meter != null, "Flowmeter was not initialized");
            if (meter == null) {
                return;
            }

            helper.assertValueEqual(meter.getFlowRate(), 1000L, "single flowmeter throughput at 16 to 15 atm");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void wholeFlowAmountsTolerateNumericalNoiseWithoutRoundingFractions(GameTestHelper helper) {
        helper.assertValueEqual(GasFlowMath.toWholeAmount(999.999999998603), 1000L, "whole flow with pressure solver roundoff");
        helper.assertValueEqual(GasFlowMath.toWholeAmount(999.999999), 999L, "flow outside the integer tolerance");
        helper.assertValueEqual(GasFlowMath.toWholeAmount(999.5), 999L, "fractional flow must still round down");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", batch = "gasSolverProfilerCapture")
    public static void connectedNetworkIsSolvedOncePerGameTick(GameTestHelper helper) {
        buildProfiledNetwork(helper, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        SolveCountCharacterization run = new SolveCountCharacterization(helper, 0, 0, false);
        helper.onEachTick(run::tick);
    }

    @GameTest(template = "gametest/empty_8x3x7", batch = "gasSolverProfilerPressureCapture")
    public static void manometerOnlyAddsPressureObservationWork(GameTestHelper helper) {
        buildProfiledNetwork(helper, CCBBlocks.AIRTIGHT_MANOMETER_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        SolveCountCharacterization run = new SolveCountCharacterization(helper, 1, 0, true);
        helper.onEachTick(run::tick);
    }

    @GameTest(template = "gametest/empty_8x3x7", batch = "gasSolverProfilerFlowCapture")
    public static void flowmeterOnlyAddsFlowObservationTarget(GameTestHelper helper) {
        buildProfiledNetwork(helper, CCBBlocks.AIRTIGHT_FLOWMETER_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        SolveCountCharacterization run = new SolveCountCharacterization(helper, 0, 1, false);
        helper.onEachTick(run::tick);
    }

    private static void buildProfiledNetwork(GameTestHelper helper, BlockState middlePipeState) {
        placeBoundaryTank(helper, MULTI_SOURCE_POS, SOURCE_PRESSURE_PA);
        for (int offset = 0; offset < MULTI_PIPE_COUNT; offset++) {
            BlockPos pipePos = FIRST_MULTI_PIPE_POS.offset(offset, 0, 0);
            BlockState pipeState = offset == 1 ? middlePipeState : CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X);
            helper.setBlock(pipePos, pipeState);
        }
        placeBoundaryTank(helper, MULTI_SINK_POS, SINK_PRESSURE_PA);
    }

    private static void placeBoundaryTank(GameTestHelper helper, BlockPos pos, long pressurePa) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Creative airtight tank block entity missing at " + pos + '.');
        }

        tank.getTankInventory().setFixedPressurePa(pressurePa);
        tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
    }

    private static double facePressure(GasPressureGraphSolution solution, BlockFace face) {
        for (FacePressure pressure : solution.facePressures()) {
            if (pressure.face().equals(face)) {
                return pressure.pressurePa();
            }
        }
        return Double.NaN;
    }

    private static void assertClose(GameTestHelper helper, double actual, double expected, double tolerance, String label) {
        helper.assertTrue(Double.isFinite(actual), label + " was not finite: " + actual);
        helper.assertTrue(Math.abs(actual - expected) <= tolerance, label + " changed outside the characterization tolerance: expected " + expected + " +/- " + tolerance + ", got " + actual);
    }

    private static final class SolveCountCharacterization {
        private final GameTestHelper helper;
        private Capture capture;
        private Set<BlockPos> networkPipes = Set.of();
        private long topologyReadyGameTime = Long.MIN_VALUE;
        private long captureStartGameTime = Long.MIN_VALUE;
        private final int expectedPressureTargetCount;
        private final int expectedFlowTargetCount;
        private final boolean expectPressureSamples;
        private boolean finished;

        private SolveCountCharacterization(GameTestHelper helper, int expectedPressureTargetCount, int expectedFlowTargetCount, boolean expectPressureSamples) {
            this.helper = helper;
            this.expectedPressureTargetCount = expectedPressureTargetCount;
            this.expectedFlowTargetCount = expectedFlowTargetCount;
            this.expectPressureSamples = expectPressureSamples;
        }

        private void tick() {
        ServerLevel level = helper.getLevel();
            if (finished) {
                return;
            }

            long gameTime = level.getGameTime();
            if (capture == null) {
                BlockPos absoluteFirstPipePos = helper.absolutePos(FIRST_MULTI_PIPE_POS);
                Snapshot topology = GasNetworkTopology.get(level, absoluteFirstPipePos);
                if (topology.pipePositions().size() != MULTI_PIPE_COUNT) {
                    topologyReadyGameTime = Long.MIN_VALUE;
                    return;
                }

                if (topologyReadyGameTime == Long.MIN_VALUE) {
                    topologyReadyGameTime = gameTime;
                    return;
                }

                if (gameTime - topologyReadyGameTime < PROFILE_WARMUP_TICKS) {
                    return;
                }

                networkPipes = topology.pipePositions();
                capture = GasSolverProfiler.capture();
                captureStartGameTime = gameTime;
                return;
            }

            long elapsedTicks = gameTime - captureStartGameTime;
            if (elapsedTicks < PROFILE_SAMPLE_TICKS) {
                return;
            }

            Capture completedCapture = capture;
            capture = null;
            completedCapture.close();
            List<NetworkProfile> samples = new ArrayList<>();
            for (NetworkProfile sample : completedCapture.samples()) {
                if (!(sample.shape().pipeCount() == MULTI_PIPE_COUNT && networkPipes.contains(sample.startPos()))) {
                    continue;
                }

                samples.add(sample);
            }
            helper.assertValueEqual(samples.size(), Math.toIntExact(elapsedTicks), "connected-network solver profile count over elapsed game ticks");
            for (NetworkProfile sample : samples) {
                helper.assertValueEqual(sample.topologyCache().hits(), 1L, "connected-network topology cache hits per profile");
                helper.assertValueEqual(sample.topologyCache().misses(), 0L, "connected-network topology cache misses per profile");
                helper.assertValueEqual(sample.shape().pipeCount(), MULTI_PIPE_COUNT, "connected-network profiled pipe count");
                helper.assertValueEqual(sample.shape().endpointFaceCount(), PROFILE_ENDPOINT_FACE_COUNT, "connected-network profiled endpoint face count");
                helper.assertValueEqual(sample.shape().gasGroupCount(), 1, "connected-network profiled gas group count");
                helper.assertValueEqual(sample.shape().pressureEndpointCount(), PROFILE_PRESSURE_ENDPOINT_COUNT, "connected-network profiled pressure endpoint count");
                helper.assertValueEqual(sample.shape().nodeCount(), PROFILE_NODE_COUNT, "connected-network pressure graph node count");
                helper.assertValueEqual(sample.shape().edgeCount(), PROFILE_EDGE_COUNT, "connected-network pressure graph edge count");

                helper.assertValueEqual(sample.simulation().gasSolveCount(), 1, "connected-network gas solve count per network profile");
                helper.assertValueEqual(sample.simulation().dynamicSubsteps(), 1, "connected-network dynamic substeps per profile");
                helper.assertValueEqual(sample.simulation().substepCapHitCount(), 0, "connected-network substep-cap hits per profile");

                helper.assertValueEqual(sample.telemetry().pressureTargetCount(), expectedPressureTargetCount, "connected-network pressure telemetry target count");
                helper.assertValueEqual(sample.telemetry().flowTargetCount(), expectedFlowTargetCount, "connected-network flow telemetry target count");
                if (expectPressureSamples) {
                    helper.assertTrue(sample.telemetry().pressureFaceSampleCount() > 0, "manometer network did not collect pressure face samples");
                }
                else {
                    helper.assertValueEqual(sample.telemetry().pressureFaceSampleCount(), 0, "network collected pressure face samples without a manometer");
                }
                if (expectedFlowTargetCount == 0) {
                    helper.assertValueEqual(sample.telemetry().flowTargetUpdateCount(), 0, "ordinary network published flow telemetry without a flowmeter");
                }

                helper.assertValueEqual(sample.graph().solveCount(), 1, "connected-network graph solve count per profile");
                helper.assertValueEqual(sample.graph().hypotheticalSolveCount(), 0, "connected-network hypothetical graph solves per profile");
                helper.assertValueEqual(sample.graph().nonConvergedSolveCount(), 0, "connected-network non-converged graph solves per profile");
                helper.assertValueEqual(sample.graph().activeSetIterations(), PROFILE_ACTIVE_SET_ITERATIONS, "connected-network active-set iterations per profile");
                helper.assertValueEqual(sample.graph().forestDirectSolveSuccesses(), sample.graph().forestDirectSolveAttempts(), "connected-network forest solve success/attempt parity");
                helper.assertValueEqual(sample.graph().conjugateGradientIterations(), 0, "connected-network CG iterations per profile");
                helper.assertValueEqual(sample.graph().biConjugateGradientStabilizedIterations(), 0, "connected-network BiCGStab iterations per profile");
                helper.assertValueEqual(sample.graph().sorSweeps(), 0, "connected-network SOR sweeps per profile");
            }

            finished = true;
            helper.succeed();
        }
    }
}
