package net.ty.createcraftedbeginning.gametests.gas;

import com.google.gson.GsonBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkSimulator;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.Capture;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.NetworkProfile;
import net.ty.createcraftedbeginning.gas.storage.CreativeGasReservoir;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasParallelPumpBenchmarkGameTest {
    private GasParallelPumpBenchmarkGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 3000, batch = "parallelPumpDiagnostic")
    public static void emptyTankParallelPumpsAndDirectionChanges(GameTestHelper helper) {
        if (!Boolean.getBoolean("createcraftedbeginning.parallel_pump_benchmark")) {
            helper.succeed();
            return;
        }

        start(helper, true);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 300, batch = "parallelPumpRegression")
    public static void emptyTankParallelPumpsConvergeAfterEveryDirectionChange(GameTestHelper helper) {
        start(helper, false);
    }

    private static void start(GameTestHelper helper, boolean benchmark) {
        Run run = new Run(helper, benchmark);
        run.nextSpeed();
        helper.onEachTick(() -> {
            try {
                run.tick();
            }
            catch (RuntimeException failure) {
                run.cleanup();
                throw failure;
            }
        });
    }

    private static final class Run {
        private static final int STARTUP_TICKS = 20;
        private final boolean benchmark;
        private final int sampleTicks;
        private final int[] speeds;
        private final int[] flippedColumns;
        private final GameTestHelper helper;
        private final ServerLevel level;
        private final BlockPos origin;
        private final List<BlockPos> placed = new ArrayList<>();
        private final Set<BlockPos> pipes = new HashSet<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<AirtightPumpBlockEntity> pumps = new ArrayList<>();
        private @Nullable Capture capture;
        private int speedIndex = -1;
        private int phase;
        private int ticks;
        private long directionChangeNanos;
        private boolean finished;

        private Run(GameTestHelper helper, boolean benchmark) {
            this.helper = helper;
            this.benchmark = benchmark;
            sampleTicks = benchmark ? Math.clamp(Integer.getInteger("createcraftedbeginning.parallel_pump_samples", 8), 2, 40) : 8;
            speeds = benchmark ? Arrays.stream(System.getProperty("createcraftedbeginning.parallel_pump_speeds", "256,128,64,0").split(",")).mapToInt(Integer::parseInt).toArray() : new int[]{256};
            flippedColumns = benchmark ? Arrays.stream(System.getProperty("createcraftedbeginning.parallel_pump_columns", "0,3,7").split(",")).mapToInt(Integer::parseInt).toArray() : new int[]{0, 1, 2, 3, 4, 5, 6, 7};
            level = helper.getLevel();
            origin = helper.absolutePos(new BlockPos(0, 1, 0)).offset(0, 0, 6144);
        }

        private String label() {
            String prefix = "parallel-pumps-" + speeds[speedIndex] + "rpm-";
            if (phase == 0) {
                return prefix + "startup";
            }
            if (phase == 1) {
                return prefix + "all-forward";
            }

            int column = flippedColumns[(phase - 2) / 2];
            if (phase % 2 == 0) {
                return prefix + "flip-" + column;
            }

            return prefix + "restore-" + column;
        }

        private void nextSpeed() {
            cleanup();
            if (++speedIndex >= speeds.length) {
                finished = true;
                helper.succeed();
                return;
            }

            ChunkPos min = new ChunkPos(origin.offset(-1, -1, -4));
            ChunkPos max = new ChunkPos(origin.offset(8, 1, 5));
            for (int x = min.x; x <= max.x; x++) {
                for (int z = min.z; z <= max.z; z++) {
                    if (level.setChunkForced(x, z, true)) {
                        forced.add(new ChunkPos(x, z));
                    }
                    level.getChunk(x, z);
                }
            }
            for (int column = 0; column < 8; column++) {
                BlockState upper = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.getDefaultState().setValue(PipeBlock.SOUTH, true).setValue(PipeBlock.NORTH, column == 0).setValue(PipeBlock.WEST, column > 0).setValue(PipeBlock.EAST, column < 7);
                BlockState lower = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.getDefaultState().setValue(PipeBlock.NORTH, true).setValue(PipeBlock.SOUTH, column == 0).setValue(PipeBlock.WEST, column > 0).setValue(PipeBlock.EAST, column < 7);
                pipe(origin.offset(column, 0, 0), upper);
                pipe(origin.offset(column, 0, 2), lower);
                BlockPos pumpPos = origin.offset(column, 0, 1);
                pipe(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH));
                BlockEntity pumpEntity = level.getBlockEntity(pumpPos);
                if (pumpEntity == null) {
                    throw new NullPointerException("Parallel pump fixture block entity is missing at " + pumpPos + '.');
                }

                if (!(pumpEntity instanceof AirtightPumpBlockEntity pump)) {
                    throw new IllegalStateException("Expected an airtight pump block entity at " + pumpPos + '.');
                }

                pumps.add(pump);
            }
            BlockState straight = CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.Z);
            pipe(origin.north(), straight);
            pipe(origin.north(2), CCBBlocks.AIRTIGHT_MANOMETER_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.Z));
            pipe(origin.north(3), CCBBlocks.AIRTIGHT_FLOWMETER_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.Z));
            pipe(origin.south(3), straight);
            BlockPos tankPos = origin.south(4);
            place(tankPos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.getDefaultState());
            BlockEntity tankEntity = level.getBlockEntity(tankPos);
            if (tankEntity == null) {
                throw new NullPointerException("Parallel pump fixture tank block entity is missing at " + tankPos + '.');
            }

            if (!(tankEntity instanceof CreativeAirtightTankBlockEntity tank)) {
                throw new IllegalStateException("Expected a creative airtight tank block entity at " + tankPos + '.');
            }

            CreativeGasReservoir inventory = tank.getTankInventory();
            inventory.setFixedPressurePa(GasPressure.pascals(16));
            helper.assertTrue(inventory.getGasInTank(0).isEmpty(), "Creative tank must start without configured gas");
            place(origin.offset(0, -1, 1), AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Axis.Z));
            BlockPos motorPos = origin.offset(0, -1, 2);
            place(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH));
            BlockEntity motorEntity = level.getBlockEntity(motorPos);
            if (motorEntity == null) {
                throw new NullPointerException("Parallel pump fixture motor block entity is missing at " + motorPos + '.');
            }

            if (!(motorEntity instanceof CreativeMotorBlockEntity motor)) {
                throw new IllegalStateException("Expected a creative motor block entity at " + motorPos + '.');
            }

            motor.generatedSpeed.setValue(speeds[speedIndex]);
            phase = 0;
            ticks = 0;
            directionChangeNanos = 0;
            capture = GasSolverProfiler.capture();
        }

        private void tick() {
            if (finished) {
                return;
            }

            GasNetworkSimulator.simulateTick(level, origin);
            if (phase > 0 || ticks >= STARTUP_TICKS - 1) {
                for (AirtightPumpBlockEntity pump : pumps) {
                    float speed = pump.getSpeed();
                    helper.assertTrue(Math.abs(speed) == speeds[speedIndex], label() + " unexpected speed at " + pump.getBlockPos() + ": " + speed);
                }
            }
            ticks++;
            int phaseTicks = phase == 0 ? STARTUP_TICKS : sampleTicks;
            if (ticks < phaseTicks) {
                return;
            }

            Snapshot topology = GasNetworkTopology.get(level, origin);
            helper.assertValueEqual(topology.pipePositions().size(), 28, "parallel fixture transport block count");
            helper.assertValueEqual(topology.endpointFaces().size(), 1, "parallel fixture tank access count");
            List<BlockFace> atmosphericFaces = topology.atmosphericFaces();
            helper.assertValueEqual(atmosphericFaces.size(), 1, "parallel fixture atmospheric opening count");
            BlockFace atmosphericFace = atmosphericFaces.getFirst();
            helper.assertTrue(atmosphericFace.getPos().equals(origin.north(3)) && atmosphericFace.getFace() == Direction.NORTH, "Only the flowmeter outlet may access atmosphere");
            report();
            if (++phase >= 2 + 2 * flippedColumns.length) {
                nextSpeed();
                return;
            }

            ticks = 0;
            directionChangeNanos = 0;
            capture = GasSolverProfiler.capture();
            if (phase < 2) {
                return;
            }

            int column = flippedColumns[(phase - 2) / 2];
            BlockPos pumpPos = origin.offset(column, 0, 1);
            BlockState state = level.getBlockState(pumpPos);
            BlockState rotated = CCBBlocks.AIRTIGHT_PUMP_BLOCK.get().getRotatedBlockState(state, Direction.UP);
            long start = System.nanoTime();
            level.setBlock(pumpPos, rotated, Block.UPDATE_ALL);
            GasNetworkTopology.invalidate(level, pumpPos);
            directionChangeNanos = System.nanoTime() - start;
        }

        private void report() {
            String label = label();
            Capture completed = capture;
            if (completed == null) {
                throw new NullPointerException("Parallel pump benchmark capture is missing for '" + label + "'.");
            }

            completed.close();
            capture = null;
            List<NetworkProfile> samples = completed.samples().stream().filter(sample -> pipes.contains(sample.startPos())).toList();
            helper.assertValueEqual(samples.size(), ticks, label + " exactly one network simulation per tick");
            helper.assertTrue(samples.stream().allMatch(sample -> sample.graph().nonConvergedSolveCount() == 0), label + " pressure solve failed");
            if (phase > 0) {
                helper.assertTrue(samples.stream().anyMatch(sample -> sample.topologyCache().misses() == 0), label + " has no cached simulation sample");
                helper.assertTrue(samples.stream().filter(sample -> sample.topologyCache().misses() == 0).allMatch(sample -> sample.diagnostics().regularizedSolves() == 0), label + " cached equilibrium repeated pressure recovery");
            }
            if (!benchmark) {
                return;
            }

            GasSolverProfiler.logSummary(label, samples);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("label", label);
            report.put("requestedRpm", speeds[speedIndex]);
            report.put("actualSignedRpm", pumps.stream().map(AirtightPumpBlockEntity::getSpeed).toList());
            report.put("facings", pumps.stream().map(pump -> pump.getBlockState().getValue(BlockStateProperties.FACING).getName()).toList());
            report.put("directionChangeNanos", directionChangeNanos);
            report.put("java", System.getProperty("java.runtime.version"));
            report.put("jvmArgs", ManagementFactory.getRuntimeMXBean().getInputArguments());
            report.put("summary", GasSolverProfiler.summarize(samples));
            report.put("samples", samples);
            Path directory = Path.of("parallel-pump-benchmark");
            try {
                Files.createDirectories(directory);
                Files.writeString(directory.resolve(label + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
            }
            catch (IOException failure) {
                throw new IllegalStateException("Failed to export parallel pump benchmark '" + label + "'.", failure);
            }
            GasParallelPumpNumericalDiagnostics.report(level, origin, label);
        }

        private void pipe(BlockPos pos, BlockState state) {
            pipes.add(pos);
            place(pos, state);
        }

        private void place(BlockPos pos, BlockState state) {
            placed.add(pos);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }

        private void cleanup() {
            if (capture != null) {
                capture.close();
                capture = null;
            }
            for (BlockPos pos : placed) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            placed.clear();
            pipes.clear();
            pumps.clear();
            for (ChunkPos chunk : forced) {
                level.setChunkForced(chunk.x, chunk.z, false);
            }
            forced.clear();
        }
    }
}
