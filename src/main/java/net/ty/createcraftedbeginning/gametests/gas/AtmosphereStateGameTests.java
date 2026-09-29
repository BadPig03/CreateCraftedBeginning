package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereState;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.endpoint.AtmosphericGasEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AtmosphereStateGameTests {
    private static final BlockPos AIR_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SOLID_POS = new BlockPos(2, 1, 1);
    private static final BlockPos BUBBLE_POS = new BlockPos(1, 1, 2);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos WEST_POS = PIPE_POS.relative(Direction.WEST);
    private static final BlockPos EAST_POS = PIPE_POS.relative(Direction.EAST);
    private static final int PLANNER_READY_DEADLINE_TICKS = 20;

    private AtmosphereStateGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void resolverCombinesCompositionAndPressure(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.dimension() == Level.OVERWORLD, "Atmosphere state core test did not run in the Overworld");
        helper.setBlock(AIR_POS, Blocks.AIR.defaultBlockState());
        helper.setBlock(SOLID_POS, Blocks.STONE.defaultBlockState());
        helper.setBlock(BUBBLE_POS.below(), Blocks.SOUL_SAND.defaultBlockState());
        helper.setBlock(BUBBLE_POS, Blocks.BUBBLE_COLUMN.defaultBlockState());

        AtmosphereState airState = AtmosphereStateResolver.resolve(level, helper.absolutePos(AIR_POS));
        AtmosphereState solidState = AtmosphereStateResolver.resolve(level, helper.absolutePos(SOLID_POS));
        AtmosphereState bubbleState = AtmosphereStateResolver.resolve(level, helper.absolutePos(BUBBLE_POS));
        long expectedPressurePa = expectedAmbientPressurePa(level);

        helper.assertTrue(airState.gas() == CCBGases.NATURAL_AIR.get(), "Overworld air did not resolve to Natural Air");
        helper.assertTrue(solidState.gas().isEmpty(), "Block without an atmosphere composition handler did not resolve to empty composition");
        helper.assertTrue(bubbleState.gas() == CCBGases.MOIST_AIR.get(), "Bubble column did not resolve to Moist Air");
        helper.assertValueEqual(airState.pressurePa(), expectedPressurePa, "air atmospheric pressure");
        helper.assertValueEqual(solidState.pressurePa(), expectedPressurePa, "empty-composition atmospheric pressure");
        helper.assertValueEqual(bubbleState.pressurePa(), expectedPressurePa, "bubble-column atmospheric pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void atmosphereStateKeepsCompositionAndPressureIndependent(GameTestHelper helper) {
        AtmosphereState emptyAtPressure = new AtmosphereState(Gas.EMPTY_GAS_HOLDER.value(), GasPressure.REFERENCE_PRESSURE_PA);
        AtmosphereState clampedVacuum = new AtmosphereState(CCBGases.NATURAL_AIR.get(), -1);

        helper.assertTrue(emptyAtPressure.gas().isEmpty(), "Atmosphere state did not preserve empty composition");
        helper.assertValueEqual(emptyAtPressure.pressurePa(), GasPressure.REFERENCE_PRESSURE_PA, "pressure retained by empty-composition atmosphere state");
        helper.assertTrue(clampedVacuum.gas() == CCBGases.NATURAL_AIR.get(), "Atmosphere state changed gas while clamping pressure");
        helper.assertValueEqual(clampedVacuum.pressurePa(), GasPressure.VACUUM_PA, "atmosphere pressure clamped below vacuum");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void resolverDoesNotLoadChunkForComposition(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos farPos = helper.absolutePos(new BlockPos(4096, 1, 4096));
        helper.assertTrue(!level.isLoaded(farPos), "Far atmosphere test position was unexpectedly loaded before resolution");

        AtmosphereState state = AtmosphereStateResolver.resolve(level, farPos);

        helper.assertTrue(state.gas().isEmpty(), "Unloaded position exposed an atmosphere composition");
        helper.assertValueEqual(state.pressurePa(), expectedAmbientPressurePa(level), "unloaded-position atmospheric pressure");
        helper.assertTrue(!level.isLoaded(farPos), "Atmosphere resolution loaded a chunk while reading composition");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void atmosphericEndpointResolvesItsOutputPosition(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        helper.setBlock(EAST_POS.below(), Blocks.SOUL_SAND.defaultBlockState());
        helper.setBlock(EAST_POS, Blocks.BUBBLE_COLUMN.defaultBlockState());

        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        BlockPos absoluteEastPos = helper.absolutePos(EAST_POS);
        BlockEntity pipe = level.getBlockEntity(absolutePipePos);
        helper.assertTrue(pipe != null, "Airtight pipe block entity was not initialized for atmosphere endpoint test");
        if (pipe == null) {
            throw new NullPointerException("Airtight pipe block entity was not initialized for atmosphere endpoint test.");
        }

        AtmosphericGasEndpoint endpoint = new AtmosphericGasEndpoint(new BlockFace(absolutePipePos, Direction.EAST));
        AtmosphereState unboundState = endpoint.getAtmosphereState();
        helper.assertTrue(unboundState.gas().isEmpty(), "Unbound atmospheric endpoint exposed a gas composition");
        helper.assertValueEqual(unboundState.pressurePa(), GasPressure.VACUUM_PA, "unbound atmospheric endpoint pressure");

        endpoint.bind(level, pipe);
        AtmosphereState endpointState = endpoint.getAtmosphereState();
        AtmosphereState resolvedState = AtmosphereStateResolver.resolve(level, absoluteEastPos);

        helper.assertTrue(endpointState.gas() == CCBGases.MOIST_AIR.get(), "Atmospheric endpoint did not resolve composition at its output position");
        helper.assertTrue(endpointState.gas() == resolvedState.gas(), "Atmospheric endpoint composition differed from the resolver");
        helper.assertValueEqual(endpointState.pressurePa(), resolvedState.pressurePa(), "atmospheric endpoint pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 60)
    public static void plannerKeepsAtmosphereSnapshotStableWithinPlanningPass(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(WEST_POS, Blocks.AIR.defaultBlockState());
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        helper.setBlock(EAST_POS, Blocks.AIR.defaultBlockState());

        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        BlockFace eastFace = new BlockFace(absolutePipePos, Direction.EAST);
        BlockFace westFace = new BlockFace(absolutePipePos, Direction.WEST);
        GasEndpointPlanner[] capturedPlanner = new GasEndpointPlanner[1];
        Gas[] capturedGas = new Gas[1];
        long[] capturedPressurePa = new long[1];
        int[] phase = new int[1];
        int[] phaseTicks = new int[1];

        helper.onEachTick(() -> {
            phaseTicks[0]++;
            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, absolutePipePos, GasTransportBehaviour.TYPE);
            if (transport == null || transport.getConnection(Direction.EAST) == null || transport.getConnection(Direction.WEST) == null) {
                failAfterDeadline(helper, phaseTicks[0], "Airtight pipe did not establish both atmospheric connections for snapshot test");
                return;
            }

            GasNetworkTopology.invalidate(level, absolutePipePos);
            Snapshot topology = GasNetworkTopology.get(level, absolutePipePos);
            if (!topology.atmosphericFaces().contains(eastFace) || !topology.atmosphericFaces().contains(westFace)) {
                failAfterDeadline(helper, phaseTicks[0], "Solver topology did not expose both atmospheric faces for snapshot test");
                return;
            }

            if (phase[0] == 0) {
                AtmosphereState initialState = AtmosphereStateResolver.resolve(level, eastFace.getConnectedPos());
                helper.assertTrue(!initialState.gas().isEmpty(), "Initial atmospheric composition was empty");
                helper.assertTrue(initialState.gas() != CCBGases.MOIST_AIR.get(), "Initial atmospheric composition was already Moist Air");

                GasEndpointPlanner planner = GasEndpointPlanner.prepare(level, topology);
                helper.assertTrue(containsGas(planner.gasGroups(), initialState.gas()), "Planner did not capture the initial atmospheric gas group");
                helper.assertTrue(!containsGas(planner.gasGroups(), CCBGases.MOIST_AIR.get()), "Planner captured Moist Air before the atmosphere changed");

                capturedPlanner[0] = planner;
                capturedGas[0] = initialState.gas();
                capturedPressurePa[0] = initialState.pressurePa();
                helper.setBlock(EAST_POS.below(), Blocks.SOUL_SAND.defaultBlockState());
                helper.setBlock(EAST_POS, Blocks.BUBBLE_COLUMN.defaultBlockState());
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            AtmosphereState changedState = AtmosphereStateResolver.resolve(level, eastFace.getConnectedPos());
            if (changedState.gas() != CCBGases.MOIST_AIR.get()) {
                failAfterDeadline(helper, phaseTicks[0], "East atmosphere did not transition to Moist Air");
                return;
            }

            GasEndpointPlanner planner = capturedPlanner[0];
            Gas gas = capturedGas[0];
            helper.assertTrue(planner != null, "Captured atmospheric planner state was not retained");
            if (planner == null) {
                throw new NullPointerException("Required planner is missing.");
            }

            if (gas == null) {
                throw new NullPointerException("Captured atmospheric planner state was not retained.");
            }

            helper.assertTrue(containsGas(planner.gasGroups(), gas), "Captured planner lost its original atmospheric gas group after the world changed");
            helper.assertTrue(!containsGas(planner.gasGroups(), CCBGases.MOIST_AIR.get()), "Captured planner re-resolved atmospheric composition after the world changed");

            GasNetworkPressureEndpoint capturedEast = endpointForFace(planner.planPressureEndpoints(level, new GasStack(gas, 1)), eastFace);
            helper.assertTrue(capturedEast != null, "Captured planner did not retain the east atmospheric pressure endpoint");
            if (capturedEast == null) {
                throw new NullPointerException("Captured planner did not retain the east atmospheric pressure endpoint.");
            }

            helper.assertValueEqual(capturedEast.transferLimits().drainLimit(), Long.MAX_VALUE, "captured east atmospheric drain limit");
            helper.assertTrue(capturedEast.pressureState().pressurePa() == capturedPressurePa[0], "Captured east atmospheric endpoint changed pressure after the world changed");

            GasEndpointPlanner freshPlanner = GasEndpointPlanner.prepare(level, topology);
            helper.assertTrue(containsGas(freshPlanner.gasGroups(), gas), "Fresh planner lost the unchanged west atmospheric gas group");
            helper.assertTrue(containsGas(freshPlanner.gasGroups(), CCBGases.MOIST_AIR.get()), "Fresh planner did not capture the changed east atmosphere");

            GasNetworkPressureEndpoint freshOriginalEast = endpointForFace(freshPlanner.planPressureEndpoints(level, new GasStack(gas, 1)), eastFace);
            helper.assertTrue(freshOriginalEast != null, "Fresh planner did not retain an east fill endpoint for the original gas");
            if (freshOriginalEast == null) {
                throw new NullPointerException("Fresh planner did not retain an east fill endpoint for the original gas.");
            }

            helper.assertValueEqual(freshOriginalEast.transferLimits().drainLimit(), 0L, "fresh east drain limit for the previous atmosphere gas");

            GasNetworkPressureEndpoint freshMoistEast = endpointForFace(freshPlanner.planPressureEndpoints(level, new GasStack(CCBGases.MOIST_AIR.get(), 1)), eastFace);
            helper.assertTrue(freshMoistEast != null, "Fresh planner did not expose the changed Moist Air atmosphere");
            if (freshMoistEast == null) {
                throw new NullPointerException("Fresh planner did not expose the changed Moist Air atmosphere.");
            }

            helper.assertValueEqual(freshMoistEast.transferLimits().drainLimit(), Long.MAX_VALUE, "fresh east Moist Air drain limit");
            helper.assertTrue(freshMoistEast.pressureState().pressurePa() == changedState.pressurePa(), "Fresh east atmospheric endpoint did not use the changed atmosphere pressure");
            helper.succeed();
        });
    }

    private static long expectedAmbientPressurePa(Level level) {
        if (Level.NETHER == level.dimension()) {
            return GasPressure.pascals(CCBConfig.server().gas.atmosphere.netherPressure.getF());
        }

        if (Level.END == level.dimension()) {
            return GasPressure.pascals(CCBConfig.server().gas.atmosphere.endPressure.getF());
        }

        return GasPressure.pascals(CCBConfig.server().gas.atmosphere.overworldPressure.getF());
    }

    private static boolean containsGas(List<GasStack> groups, Gas gas) {
        for (GasStack group : groups) {
            if (group.is(gas)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable GasNetworkPressureEndpoint endpointForFace(List<GasNetworkPressureEndpoint> endpoints, BlockFace face) {
        for (GasNetworkPressureEndpoint endpoint : endpoints) {
            if (endpoint.access().drainFaces().contains(face) || endpoint.access().fillFaces().contains(face)) {
                return endpoint;
            }
        }
        return null;
    }

    private static void failAfterDeadline(GameTestHelper helper, int elapsedTicks, String message) {
        if (!(elapsedTicks > PLANNER_READY_DEADLINE_TICKS)) {
            return;
        }

        helper.fail(message);
    }
}
