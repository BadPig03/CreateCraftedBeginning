package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkSimulator;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.Capture;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.NetworkProfile;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.Summary;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasStraightPipeSolverBenchmarkGameTest {
    private static final boolean ENABLE_BENCHMARK = GasNetworkBenchmarkSupport.ENABLED;
    private static final int[] PIPE_COUNTS = {10, 50, 100, 250, 500, 1000};
    private static final int SHAPE_VALIDATION_PIPE_COUNT = 500;
    private static final int BRANCH_TRUNK_PIPE_COUNT = 250;
    private static final int BRANCH_ARM_PIPE_COUNT = 125;
    private static final int CYCLE_WIDTH_PIPE_COUNT = 200;
    private static final int CYCLE_DEPTH_PIPE_COUNT = 52;
    private static final int WARMUP_TICKS = GasNetworkBenchmarkSupport.WARMUP_TICKS;
    private static final int SAMPLE_TICKS = GasNetworkBenchmarkSupport.SAMPLE_TICKS;
    private static final long SOURCE_PRESSURE_PA = GasPressure.pascals(12);
    private static final long CHANGING_SOURCE_MIN_PRESSURE_PA = GasPressure.pascals(6);
    private static final int CHANGING_SOURCE_PERIOD_TICKS = 40;
    private static final long SINK_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final BlockPos CORRIDOR_ORIGIN = new BlockPos(1, 1, 1);
    private static final int BENCHMARK_WORLD_OFFSET = 2048;

    private GasStraightPipeSolverBenchmarkGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 12000, required = false, batch = "gasSolverProfilerBenchmark")
    public static void straightPipeScalingBenchmark(GameTestHelper helper) {
        if (!ENABLE_BENCHMARK) {
            CCBAPI.LOGGER.debug("Gas straight-pipe solver benchmark is disabled.");
            helper.succeed();
            return;
        }

        CCBAPI.LOGGER.info("Gas straight-pipe solver benchmark: starting cases for {} pipes; steady first, then a {}-tick triangle-wave source boundary from {} Pa to {} Pa; {} warm-up ticks and {} sampled ticks per case; finally one {}-pipe branching-tree and cyclic fallback validation each.", formatPipeCounts(), CHANGING_SOURCE_PERIOD_TICKS, CHANGING_SOURCE_MIN_PRESSURE_PA, SOURCE_PRESSURE_PA, WARMUP_TICKS, SAMPLE_TICKS, SHAPE_VALIDATION_PIPE_COUNT);
        BenchmarkRun run = new BenchmarkRun(helper);
        run.startNextCase();
        helper.onEachTick(run::tick);
    }

    private static String formatPipeCounts() {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < PIPE_COUNTS.length; index++) {
            if (index > 0) {
                result.append('/');
            }
            result.append(PIPE_COUNTS[index]);
        }
        return result.toString();
    }

    private enum ShapeValidation {
        BRANCHING_TREE,
        CYCLIC
    }

    private static final class BenchmarkRun {
        private final GameTestHelper helper;
        private final ServerLevel level;
        private final List<ChunkPos> forcedChunks = new ArrayList<>();
        private final List<BlockPos> shapePlacedBlocks = new ArrayList<>();
        private int caseIndex = -1;
        private boolean changingBoundary;
        private ShapeValidation shapeValidation;
        private boolean finished;
        private int pipeCount;
        private int boundaryTick;
        private int warmupTicksRemaining;
        private int sampledTicks;
        private Capture capture;
        private BlockPos absoluteSourcePos = BlockPos.ZERO;
        private BlockPos absoluteFirstPipePos = BlockPos.ZERO;
        private BlockPos absoluteSinkPos = BlockPos.ZERO;
        private BlockPos shapeMinPos = BlockPos.ZERO;
        private BlockPos shapeMaxPos = BlockPos.ZERO;
        private CreativeAirtightTankBlockEntity sourceTank;

        private BenchmarkRun(GameTestHelper helper) {
            this.helper = helper;
            level = helper.getLevel();
        }

        private static long changingSourcePressurePa(int tick) {
            int halfPeriod = CHANGING_SOURCE_PERIOD_TICKS / 2;
            int phase = Mth.positiveModulo(tick, CHANGING_SOURCE_PERIOD_TICKS);
            int rampStep = phase <= halfPeriod ? phase : CHANGING_SOURCE_PERIOD_TICKS - phase;
            long pressureRange = SOURCE_PRESSURE_PA - CHANGING_SOURCE_MIN_PRESSURE_PA;
            return CHANGING_SOURCE_MIN_PRESSURE_PA + pressureRange * rampStep / halfPeriod;
        }

        private void startNextCase() {
            cleanupCurrentCase();
            if (shapeValidation != null) {
                if (shapeValidation == ShapeValidation.BRANCHING_TREE) {
                    shapeValidation = ShapeValidation.CYCLIC;
                    CCBAPI.LOGGER.info("Gas straight-pipe solver benchmark: starting changing-boundary cyclic fallback validation.");
                }
                else {
                    finished = true;
                    CCBAPI.LOGGER.info("Gas straight-pipe solver benchmark: completed.");
                    helper.succeed();
                    return;
                }
                pipeCount = SHAPE_VALIDATION_PIPE_COUNT;
            }
            else {
                caseIndex++;
                if (caseIndex >= PIPE_COUNTS.length) {
                    if (changingBoundary) {
                        shapeValidation = ShapeValidation.BRANCHING_TREE;
                        pipeCount = SHAPE_VALIDATION_PIPE_COUNT;
                        CCBAPI.LOGGER.info("Gas straight-pipe solver benchmark: starting changing-boundary branching-tree fast-path validation.");
                    }
                    else {
                        changingBoundary = true;
                        caseIndex = 0;
                        CCBAPI.LOGGER.info("Gas straight-pipe solver benchmark: starting changing-boundary matrix.");
                    }
                }
                if (shapeValidation == null) {
                    pipeCount = PIPE_COUNTS[caseIndex];
                }
            }

            boundaryTick = 0;
            buildCurrentCase();
            warmupTicksRemaining = WARMUP_TICKS;
            sampledTicks = 0;
            capture = GasSolverProfiler.capture();
            CCBAPI.LOGGER.info("Gas straight-pipe solver benchmark: warming {} pipes for {} ticks ({}).", pipeCount, WARMUP_TICKS, currentCaseDescription());
        }

        private void tick() {
            if (finished) {
                return;
            }

            updateChangingBoundary();
            GasNetworkSimulator.simulateTick(level, absoluteFirstPipePos);
            if (warmupTicksRemaining > 0) {
                warmupTicksRemaining--;
                if (warmupTicksRemaining > 0) {
                    return;
                }

                capture.close();
                capture = null;
                int discoveredPipeCount = GasNetworkTopology.get(level, absoluteFirstPipePos).pipePositions().size();
                if (discoveredPipeCount != pipeCount) {
                    cleanupCurrentCase();
                    helper.fail(currentCaseDescription() + " benchmark expected a connected topology of " + pipeCount + " pipes before sampling, got " + discoveredPipeCount);
                    return;
                }

                capture = GasSolverProfiler.capture();
                sampledTicks = 0;
                return;
            }

            sampledTicks++;
            if (sampledTicks < SAMPLE_TICKS) {
                return;
            }

            capture.close();
            List<NetworkProfile> samples = matchingSamples(capture.samples());
            capture = null;
            if (samples.size() < SAMPLE_TICKS) {
                int actualSamples = samples.size();
                cleanupCurrentCase();
                helper.fail(currentCaseDescription() + " benchmark expected at least " + SAMPLE_TICKS + " network samples for " + pipeCount + " pipes, got " + actualSamples);
                return;
            }

            if (!validateShapeSolverPath(samples) || !validateSubstepCapTelemetry(samples) || !validateNoInstrumentTelemetry(samples)) {
                return;
            }

            GasNetworkBenchmarkSupport.report(currentProfileLabel(), samples);
            startNextCase();
        }

        private @Unmodifiable List<NetworkProfile> matchingSamples(List<NetworkProfile> profiles) {
            List<NetworkProfile> matches = new ArrayList<>();
            for (NetworkProfile profile : profiles) {
                BlockPos startPos = profile.startPos();
                if (shapeValidation != null) {
                    if (profile.shape().pipeCount() == pipeCount && isInsideShapeBounds(startPos)) {
                        matches.add(profile);
                    }
                    continue;
                }

                if (profile.shape().pipeCount() != pipeCount || startPos.getY() != absoluteFirstPipePos.getY() || startPos.getZ() != absoluteFirstPipePos.getZ()) {
                    continue;
                }

                if (startPos.getX() < absoluteFirstPipePos.getX() || startPos.getX() >= absoluteSinkPos.getX()) {
                    continue;
                }

                matches.add(profile);
            }
            return List.copyOf(matches);
        }

        private boolean validateShapeSolverPath(List<NetworkProfile> samples) {
            if (shapeValidation == null) {
                return true;
            }

            long forestAttempts = 0;
            long forestSuccesses = 0;
            long conjugateGradientIterations = 0;
            for (NetworkProfile sample : samples) {
                forestAttempts += sample.graph().forestDirectSolveAttempts();
                forestSuccesses += sample.graph().forestDirectSolveSuccesses();
                conjugateGradientIterations += sample.graph().conjugateGradientIterations();
            }
            boolean expectedPath = shapeValidation == ShapeValidation.BRANCHING_TREE ? forestAttempts > 0 && forestSuccesses == forestAttempts && conjugateGradientIterations == 0 : forestAttempts > 0 && forestSuccesses == 0 && conjugateGradientIterations > 0;
            if (expectedPath) {
                return true;
            }

            cleanupCurrentCase();
            helper.fail(currentCaseDescription() + " benchmark followed an unexpected solver path: forest=" + forestSuccesses + '/' + forestAttempts + ", CGIterations=" + conjugateGradientIterations);
            return false;
        }

        private boolean validateNoInstrumentTelemetry(List<NetworkProfile> samples) {
            for (NetworkProfile sample : samples) {
                if (sample.telemetry().pressureTargetCount() == 0 && sample.telemetry().pressureFaceSampleCount() == 0 && sample.telemetry().flowTargetCount() == 0 && sample.telemetry().flowTargetUpdateCount() == 0) {
                    continue;
                }

                cleanupCurrentCase();
                helper.fail(currentCaseDescription() + " benchmark performed display telemetry work without instruments: pressureTargets=" + sample.telemetry().pressureTargetCount() + ", pressureFaceSamples=" + sample.telemetry().pressureFaceSampleCount() + ", flowTargets=" + sample.telemetry().flowTargetCount() + ", flowTargetUpdates=" + sample.telemetry().flowTargetUpdateCount());
                return false;
            }
            return true;
        }

        private boolean validateSubstepCapTelemetry(List<NetworkProfile> samples) {
            Summary summary = GasSolverProfiler.summarize(samples);
            boolean countsValid = summary.simulation().gasSolveCount() >= summary.simulation().substepCapHitCount() && summary.simulation().substepCapAffectedSampleCount() <= summary.sampleCount();
            boolean ratesValid = summary.substepCapHitRate() >= 0 && summary.substepCapHitRate() <= 1 && summary.substepCapAffectedSampleRate() >= 0 && summary.substepCapAffectedSampleRate() <= 1;
            boolean remainingValid = summary.simulation().maxSubstepCapRemainingTickFraction() >= 0 && summary.simulation().maxSubstepCapRemainingTickFraction() <= 1;
            boolean emptyCapStateValid = summary.simulation().substepCapHitCount() != 0 || summary.simulation().substepCapAffectedSampleCount() == 0 && summary.simulation().maxSubstepCapRemainingTickFraction() == 0;
            boolean hitCapStateValid = summary.simulation().substepCapHitCount() == 0 || summary.simulation().substepCapAffectedSampleCount() > 0 && summary.simulation().maxSubstepCapRemainingTickFraction() > 0;
            if (summary.simulation().gasSolveCount() > 0 && countsValid && ratesValid && remainingValid && emptyCapStateValid && hitCapStateValid) {
                return true;
            }

            cleanupCurrentCase();
            helper.fail(currentCaseDescription() + " benchmark produced inconsistent substep-cap telemetry: gasSolves=" + summary.simulation().gasSolveCount() + ", hits=" + summary.simulation().substepCapHitCount() + ", affectedSamples=" + summary.simulation().substepCapAffectedSampleCount() + '/' + summary.sampleCount() + ", hitRate=" + summary.substepCapHitRate() + ", affectedRate=" + summary.substepCapAffectedSampleRate() + ", worstRemaining=" + summary.simulation().maxSubstepCapRemainingTickFraction());
            return false;
        }

        private boolean isInsideShapeBounds(BlockPos pos) {
            return pos.getX() >= shapeMinPos.getX() && pos.getX() <= shapeMaxPos.getX() && pos.getY() >= shapeMinPos.getY() && pos.getY() <= shapeMaxPos.getY() && pos.getZ() >= shapeMinPos.getZ() && pos.getZ() <= shapeMaxPos.getZ();
        }

        private String currentCaseDescription() {
            if (shapeValidation == ShapeValidation.BRANCHING_TREE) {
                return "changing branching tree";
            }

            if (shapeValidation == ShapeValidation.CYCLIC) {
                return "changing cyclic fallback";
            }

            if (changingBoundary) {
                return "changing boundary";
            }

            return "steady boundary";
        }

        private String currentProfileLabel() {
            if (shapeValidation == ShapeValidation.BRANCHING_TREE) {
                return "changing-branching-tree-" + pipeCount + "-pipes";
            }

            if (shapeValidation == ShapeValidation.CYCLIC) {
                return "changing-cyclic-" + pipeCount + "-pipes";
            }

            if (changingBoundary) {
                return "changing-straight-" + pipeCount + "-pipes";
            }

            return "straight-" + pipeCount + "-pipes";
        }

        private void buildCurrentCase() {
            BlockPos anchor = helper.absolutePos(CORRIDOR_ORIGIN).offset(BENCHMARK_WORLD_OFFSET, 0, BENCHMARK_WORLD_OFFSET);
            if (shapeValidation == ShapeValidation.BRANCHING_TREE) {
                buildBranchingTreeCase(anchor);
                return;
            }

            if (shapeValidation == ShapeValidation.CYCLIC) {
                buildCyclicCase(anchor);
                return;
            }

            absoluteSourcePos = anchor;
            absoluteFirstPipePos = anchor.offset(1, 0, 0);
            absoluteSinkPos = anchor.offset(pipeCount + 1, 0, 0);
            forceCorridorChunks();

            sourceTank = placeBoundaryTank(absoluteSourcePos, changingBoundary ? changingSourcePressurePa(0) : SOURCE_PRESSURE_PA);
            for (int offset = 0; offset < pipeCount; offset++) {
                BlockPos pipePos = absoluteFirstPipePos.offset(offset, 0, 0);
                level.setBlock(pipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X), Block.UPDATE_ALL);
            }
            CreativeAirtightTankBlockEntity sink = placeBoundaryTank(absoluteSinkPos, SINK_PRESSURE_PA);
            sourceTank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
            sink.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        }

        private void buildBranchingTreeCase(BlockPos anchor) {
            absoluteSourcePos = anchor;
            absoluteFirstPipePos = anchor.offset(1, 0, 0);
            BlockPos junctionPos = anchor.offset(BRANCH_TRUNK_PIPE_COUNT, 0, 0);
            BlockPos northSinkPos = junctionPos.offset(0, 0, -126);
            BlockPos southSinkPos = junctionPos.offset(0, 0, BRANCH_ARM_PIPE_COUNT + 1);
            beginShapeBounds(absoluteSourcePos);

            sourceTank = placeShapeBoundaryTank(absoluteSourcePos, changingSourcePressurePa(0));
            for (int offset = 1; offset < BRANCH_TRUNK_PIPE_COUNT; offset++) {
                placeShapeAxisPipe(anchor.offset(offset, 0, 0), Axis.X);
            }
            placeShapeEncasedPipe(junctionPos, Direction.WEST, Direction.NORTH, Direction.SOUTH);
            for (int offset = 1; offset <= BRANCH_ARM_PIPE_COUNT; offset++) {
                placeShapeAxisPipe(junctionPos.offset(0, 0, -offset), Axis.Z);
                placeShapeAxisPipe(junctionPos.offset(0, 0, offset), Axis.Z);
            }
            CreativeAirtightTankBlockEntity northSink = placeShapeBoundaryTank(northSinkPos, SINK_PRESSURE_PA);
            CreativeAirtightTankBlockEntity southSink = placeShapeBoundaryTank(southSinkPos, SINK_PRESSURE_PA);
            sourceTank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
            northSink.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
            southSink.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        }

        private void buildCyclicCase(BlockPos anchor) {
            absoluteSourcePos = anchor;
            absoluteFirstPipePos = anchor.offset(1, 0, 0);
            int maxX = CYCLE_WIDTH_PIPE_COUNT;
            int maxZ = CYCLE_DEPTH_PIPE_COUNT - 1;
            BlockPos topLeft = anchor.offset(1, 0, 0);
            BlockPos topRight = anchor.offset(maxX, 0, 0);
            BlockPos bottomLeft = anchor.offset(1, 0, maxZ);
            BlockPos bottomRight = anchor.offset(maxX, 0, maxZ);
            BlockPos sinkPos = anchor.offset(maxX + 1, 0, maxZ);
            beginShapeBounds(absoluteSourcePos);

            sourceTank = placeShapeBoundaryTank(absoluteSourcePos, changingSourcePressurePa(0));
            placeShapeEncasedPipe(topLeft, Direction.WEST, Direction.EAST, Direction.SOUTH);
            placeShapeEncasedPipe(topRight, Direction.WEST, Direction.SOUTH);
            placeShapeEncasedPipe(bottomLeft, Direction.NORTH, Direction.EAST);
            placeShapeEncasedPipe(bottomRight, Direction.NORTH, Direction.WEST, Direction.EAST);
            for (int x = 2; x < maxX; x++) {
                placeShapeAxisPipe(anchor.offset(x, 0, 0), Axis.X);
                placeShapeAxisPipe(anchor.offset(x, 0, maxZ), Axis.X);
            }
            for (int z = 1; z < maxZ; z++) {
                placeShapeAxisPipe(anchor.offset(1, 0, z), Axis.Z);
                placeShapeAxisPipe(anchor.offset(maxX, 0, z), Axis.Z);
            }
            CreativeAirtightTankBlockEntity sink = placeShapeBoundaryTank(sinkPos, SINK_PRESSURE_PA);
            sourceTank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
            sink.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        }

        private void updateChangingBoundary() {
            if (!changingBoundary || sourceTank == null) {
                return;
            }

            boundaryTick++;
            sourceTank.getTankInventory().setFixedPressurePa(changingSourcePressurePa(boundaryTick));
        }

        private void beginShapeBounds(BlockPos pos) {
            shapeMinPos = pos;
            shapeMaxPos = pos;
        }

        private void placeShapeAxisPipe(BlockPos pos, Axis axis) {
            placeShapeBlock(pos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
        }

        private void placeShapeEncasedPipe(BlockPos pos, Direction... openDirections) {
            BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState();
            for (Direction direction : openDirections) {
                state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), true);
            }
            placeShapeBlock(pos, state);
        }

        private CreativeAirtightTankBlockEntity placeShapeBoundaryTank(BlockPos pos, long pressurePa) {
            placeShapeBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
            BlockEntity blockEntity = level.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight benchmark tank was not initialized at " + pos);
            if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank)) {
                throw new IllegalStateException("Creative airtight benchmark tank was not initialized at " + pos + '.');
            }

            tank.getTankInventory().setFixedPressurePa(pressurePa);
            return tank;
        }

        private void placeShapeBlock(BlockPos pos, BlockState state) {
            forceShapeChunk(pos);
            level.setBlock(pos, state, Block.UPDATE_ALL);
            shapePlacedBlocks.add(pos.immutable());
            shapeMinPos = new BlockPos(Math.min(shapeMinPos.getX(), pos.getX()), Math.min(shapeMinPos.getY(), pos.getY()), Math.min(shapeMinPos.getZ(), pos.getZ()));
            shapeMaxPos = new BlockPos(Math.max(shapeMaxPos.getX(), pos.getX()), Math.max(shapeMaxPos.getY(), pos.getY()), Math.max(shapeMaxPos.getZ(), pos.getZ()));
        }

        private void forceShapeChunk(BlockPos pos) {
            ChunkPos chunkPos = new ChunkPos(pos);
            if (!forcedChunks.contains(chunkPos) && level.setChunkForced(chunkPos.x, chunkPos.z, true)) {
                forcedChunks.add(chunkPos);
            }
            level.getChunk(chunkPos.x, chunkPos.z);
        }

        private CreativeAirtightTankBlockEntity placeBoundaryTank(BlockPos pos, long pressurePa) {
            level.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight benchmark tank was not initialized at " + pos);
            if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank)) {
                throw new IllegalStateException("Creative airtight benchmark tank was not initialized at " + pos + '.');
            }

            tank.getTankInventory().setFixedPressurePa(pressurePa);
            return tank;
        }

        private void forceCorridorChunks() {
            ChunkPos firstChunk = new ChunkPos(absoluteSourcePos);
            ChunkPos lastChunk = new ChunkPos(absoluteSinkPos);
            int minChunkX = Math.min(firstChunk.x, lastChunk.x);
            int maxChunkX = Math.max(firstChunk.x, lastChunk.x);
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                if (level.setChunkForced(chunkX, firstChunk.z, true)) {
                    forcedChunks.add(new ChunkPos(chunkX, firstChunk.z));
                }
                level.getChunk(chunkX, firstChunk.z);
            }
        }

        private void cleanupCurrentCase() {
            if (capture != null) {
                capture.close();
                capture = null;
            }
            if (caseIndex < 0 || pipeCount <= 0) {
                return;
            }

            if (shapeValidation != null) {
                for (BlockPos pos : shapePlacedBlocks) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                shapePlacedBlocks.clear();
            }
            else {
                for (int offset = 0; offset <= pipeCount + 1; offset++) {
                    level.setBlock(absoluteSourcePos.offset(offset, 0, 0), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            for (ChunkPos forcedChunk : forcedChunks) {
                level.setChunkForced(forcedChunk.x, forcedChunk.z, false);
            }
            forcedChunks.clear();
            sourceTank = null;
        }
    }
}
