package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump.AirtightRegulatorPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkSimulator;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.Capture;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.NetworkProfile;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasNetworkStressBenchmarkGameTest {
    private GasNetworkStressBenchmarkGameTest() {}

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 5000, required = false, batch = "gasSolverStressBenchmark")
    public static void regulatorEndpointAndQuantizedBenchmarks(GameTestHelper helper) {
        if (!GasNetworkBenchmarkSupport.ENABLED) {
            helper.succeed();
            return;
        }

        Run run = new Run(helper);
        run.next();
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
        private static final String[] LABELS = {"series-3-regulators", "series-8-regulators", "manifold-16-sinks", "manifold-64-sinks", "quantized-3-tanks-reset-5gu"};
        private final GameTestHelper helper;
        private final ServerLevel level;
        private final BlockPos origin;
        private final List<BlockPos> placed = new ArrayList<>();
        private final Set<BlockPos> pipes = new HashSet<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<AirtightRegulatorPumpBlockEntity> regulators = new ArrayList<>();
        private final List<AirtightTankBlockEntity> finiteTanks = new ArrayList<>();
        private CreativeAirtightTankBlockEntity source;
        private Capture capture;
        private int scenario = -1;
        private int ticks;
        private BlockPos firstPipe;
        private boolean finished;

        private Run(GameTestHelper helper) {
            this.helper = helper;
            level = helper.getLevel();
            origin = helper.absolutePos(new BlockPos(0, 1, 0)).offset(0, 0, 4096);
        }

        private void next() {
            cleanup();
            if (++scenario == LABELS.length) {
                finished = true;
                helper.succeed();
                return;
            }

            ChunkPos min = new ChunkPos(origin.offset(-2, 0, -2));
            ChunkPos max = new ChunkPos(origin.offset(195, 0, 4));
            for (int x = min.x; x <= max.x; x++) {
                for (int z = min.z; z <= max.z; z++) {
                    if (level.setChunkForced(x, z, true)) {
                        forced.add(new ChunkPos(x, z));
                    }
                    level.getChunk(x, z);
                }
            }
            if (scenario < 2) {
                buildRegulators(scenario == 0 ? 3 : 8);
            }
            else if (scenario < 4) {
                buildManyEndpoints(scenario == 2 ? 16 : 64);
            }
            else {
                buildQuantized();
            }
            ticks = 0;
            capture = GasSolverProfiler.capture();
        }

        private void tick() {
            if (finished) {
                return;
            }

            GasNetworkSimulator.simulateTick(level, firstPipe);
            ticks++;
            if (ticks == GasNetworkBenchmarkSupport.WARMUP_TICKS) {
                capture.close();
                helper.assertValueEqual(GasNetworkTopology.get(level, firstPipe).pipePositions().size(), pipes.size(), LABELS[scenario] + " connected pipe count");
                capture = GasSolverProfiler.capture();
            }
            if (ticks > GasNetworkBenchmarkSupport.WARMUP_TICKS) {
                for (AirtightRegulatorPumpBlockEntity regulator : regulators) {
                    GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, regulator.getBlockPos());
                    if (behaviour == null) {
                        continue;
                    }

                    long throughput = behaviour.getThroughputFlowRate();
                    helper.assertTrue(throughput > 0 && throughput <= regulator.getFlowRateLimit(), "Regulator benchmark stalled or exceeded its budget");
                }
                if (!finiteTanks.isEmpty()) {
                    long total = finiteTanks.stream().mapToLong(tank -> tank.getTankInventory().getStoredAmount()).sum();
                    helper.assertValueEqual(total, finiteTanks.getFirst().getTankInventory().getVolume() * 3 + 5, "Quantized benchmark gas conservation");
                }
            }
            if (ticks >= GasNetworkBenchmarkSupport.WARMUP_TICKS + GasNetworkBenchmarkSupport.SAMPLE_TICKS) {
                capture.close();
                List<NetworkProfile> samples = capture.samples().stream().filter(profile -> pipes.contains(profile.startPos())).toList();
                capture = null;
                helper.assertValueEqual(samples.size(), GasNetworkBenchmarkSupport.SAMPLE_TICKS, LABELS[scenario] + " one network sample per tick");
                helper.assertTrue(samples.stream().allMatch(profile -> profile.graph().nonConvergedSolveCount() == 0), "Benchmark contained failed solves");
                if (scenario == 4) {
                    helper.assertTrue(samples.stream().anyMatch(profile -> profile.graph().hypotheticalSolveCount() > 0), "Quantized fixture did not exercise hypothetical solves");
                }
                GasNetworkBenchmarkSupport.report(LABELS[scenario], samples);
                next();
                return;
            }

            if (source != null && scenario >= 2) {
                source.getTankInventory().setFixedPressurePa(GasPressure.pascals(6) + GasPressure.pascals(6) * (ticks % 40 <= 20 ? ticks % 40 : 40 - ticks % 40) / 20);
            }
            resetQuantizedTanks();
        }

        private void buildRegulators(int count) {
            source = boundary(origin, GasPressure.REFERENCE_PRESSURE_PA);
            boundary(origin.offset(2 * count, 0, 0), GasPressure.pascals(3));
            for (int i = 0; i < count; i++) {
                BlockPos pos = origin.offset(1 + 2 * i, 0, 0);
                pipe(pos, CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
                place(pos.south(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH));
                AirtightRegulatorPumpBlockEntity regulator = (AirtightRegulatorPumpBlockEntity) level.getBlockEntity(pos);
                if (regulator == null) {
                    throw new NullPointerException("Regulator pump block entity is missing at " + pos + '.');
                }

                regulator.setOutletSetPressurePa(GasPressure.pascals(4));
                regulators.add(regulator);
                BlockPos motorPos = pos.south();
                CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) level.getBlockEntity(motorPos);
                if (motor == null) {
                    throw new NullPointerException("Creative motor block entity is missing at " + motorPos + '.');
                }

                motor.generatedSpeed.setValue(i == 0 ? 64 : 128);
                if (!(i + 1 < count)) {
                    continue;
                }

                pipe(pos.east(), axis(Axis.X));
            }
        }

        private void buildManyEndpoints(int count) {
            source = boundary(origin, GasPressure.pascals(12));
            for (int i = 1; i <= count * 3; i++) {
                BlockPos pos = origin.offset(i, 0, 0);
                if (i % 3 != 0) {
                    pipe(pos, axis(Axis.X));
                    continue;
                }

                pipe(pos, i == count * 3 ? encased(Direction.WEST, Direction.SOUTH) : encased(Direction.WEST, Direction.EAST, Direction.SOUTH));
                pipe(pos.south(), axis(Axis.Z));
                boundary(pos.south(2), GasPressure.REFERENCE_PRESSURE_PA);
            }
        }

        private void buildQuantized() {
            BlockPos center = origin.offset(1, 0, 1);
            for (Direction face : List.of(Direction.WEST, Direction.EAST, Direction.SOUTH)) {
                BlockPos pos = center.relative(face);
                place(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState());
                AirtightTankBlockEntity tank = (AirtightTankBlockEntity) level.getBlockEntity(pos);
                if (tank == null) {
                    throw new NullPointerException("Airtight tank block entity is missing at " + pos + '.');
                }

                finiteTanks.add(tank);
            }
            pipe(center, encased(Direction.WEST, Direction.EAST, Direction.SOUTH));
            resetQuantizedTanks();
        }

        private void resetQuantizedTanks() {
            for (int i = 0; i < finiteTanks.size(); i++) {
                GasTank tank = finiteTanks.get(i).getTankInventory();
                tank.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), tank.getVolume() + (i == 0 ? 5 : 0))).requireAccepted();
            }
        }

        private CreativeAirtightTankBlockEntity boundary(BlockPos pos, long pressure) {
            place(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.getDefaultState());
            CreativeAirtightTankBlockEntity tank = (CreativeAirtightTankBlockEntity) level.getBlockEntity(pos);
            if (tank == null) {
                throw new NullPointerException("Creative airtight tank block entity is missing at " + pos + '.');
            }

            tank.getTankInventory().setFixedPressurePa(pressure);
            tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
            return tank;
        }

        private static BlockState axis(Axis axis) {
            return CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, axis);
        }

        private static BlockState encased(Direction... faces) {
            BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.getDefaultState();
            for (Direction face : faces) {
                state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(face), true);
            }
            return state;
        }

        private void pipe(BlockPos pos, BlockState state) {
            if (pipes.isEmpty()) {
                firstPipe = pos;
            }
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
            regulators.clear();
            finiteTanks.clear();
            source = null;
            for (ChunkPos chunk : forced) {
                level.setChunkForced(chunk.x, chunk.z, false);
            }
            forced.clear();
        }
    }
}
