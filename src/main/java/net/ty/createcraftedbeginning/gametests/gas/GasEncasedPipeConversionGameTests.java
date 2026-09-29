package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasEncasedPipeConversionGameTests {
    private static final long SOURCE_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final int FLOW_DEADLINE_TICKS = 30;
    private static final int CASING_RECOVERY_DEADLINE_TICKS = 5;

    private static final BlockPos SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);

    private GasEncasedPipeConversionGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void replacingOrdinaryPipeWithEncasedPipeInheritsAxisConnections(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(PIPE_POS, uncasedPipeState());

        BlockState originalState = level.getBlockState(helper.absolutePos(PIPE_POS));
        helper.assertTrue(originalState.is(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get()), "Ordinary airtight pipe was not present before replacement");
        helper.assertTrue(originalState.getValue(RotatedPillarBlock.AXIS) == Axis.X, "Ordinary airtight pipe did not start on the X axis");
        helper.assertTrue(!originalState.getValue(AirtightPipeBlock.CASED), "Ordinary airtight pipe unexpectedly started cased");

        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack encasedPipeStack = new ItemStack(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, encasedPipeStack);

        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePipePos).add(0, 0.5, 0), Direction.UP, absolutePipePos, false);
        InteractionResult replacementResult = encasedPipeStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
        helper.assertTrue(replacementResult.consumesAction(), "Using an airtight encased pipe item did not replace the ordinary airtight pipe");

        BlockState replacedState = level.getBlockState(absolutePipePos);
        assertInheritedXAxisOpenings(helper, replacedState);

        placeCreativeSource(helper);

        helper.succeedWhen(() -> {
            BlockState runtimeState = level.getBlockState(absolutePipePos);
            assertInheritedXAxisOpenings(helper, runtimeState);

            GasTransportBehaviour transport = transport(helper);
            helper.assertTrue(transport.canConnectOnFace(runtimeState, Direction.WEST), "Inherited west opening could not connect to the high-pressure source");
            helper.assertTrue(transport.canConnectOnFace(runtimeState, Direction.EAST), "Inherited east opening could not connect to atmosphere");
            helper.assertTrue(!transport.isConnectionFaceEnabled(runtimeState, Direction.NORTH), "Replacement unexpectedly enabled the north face");
            helper.assertTrue(!transport.isConnectionFaceEnabled(runtimeState, Direction.SOUTH), "Replacement unexpectedly enabled the south face");
            helper.assertTrue(!transport.isConnectionFaceEnabled(runtimeState, Direction.UP), "Replacement unexpectedly enabled the up face");
            helper.assertTrue(!transport.isConnectionFaceEnabled(runtimeState, Direction.DOWN), "Replacement unexpectedly enabled the down face");

            FlowState westFlow = transport.getFlowState(Direction.WEST);
            FlowState eastFlow = transport.getFlowState(Direction.EAST);
            assertPositiveForwardFlow(helper, westFlow, eastFlow, "replaced airtight encased pipe");
            if (westFlow != null) {
                helper.assertValueEqual(transport.getThroughputFlowRate(), westFlow.flowRate(), "replaced airtight encased pipe throughput");
            }
            helper.assertTrue(transport.getFlowState(Direction.NORTH) == null, "Replacement produced ghost north flow");
            helper.assertTrue(transport.getFlowState(Direction.SOUTH) == null, "Replacement produced ghost south flow");
            helper.assertTrue(transport.getFlowState(Direction.UP) == null, "Replacement produced ghost up flow");
            helper.assertTrue(transport.getFlowState(Direction.DOWN) == null, "Replacement produced ghost down flow");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void casingOrdinaryPipePreservesTransportTopology(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeCreativeSource(helper);
        helper.setBlock(PIPE_POS, uncasedPipeState());

        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        long[] initialFlowRate = new long[1];
        int[] phase = new int[1];
        int[] elapsedTicks = new int[1];
        int[] recoveryTicks = new int[1];

        helper.onEachTick(() -> {
            elapsedTicks[0]++;
            BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
            BlockState state = level.getBlockState(absolutePipePos);

            if (phase[0] == 0) {
                helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get()), "Ordinary airtight pipe disappeared before casing");
                helper.assertTrue(state.getValue(RotatedPillarBlock.AXIS) == Axis.X, "Ordinary airtight pipe changed axis before casing");
                helper.assertTrue(!state.getValue(AirtightPipeBlock.CASED), "Ordinary airtight pipe became cased before the test interaction");

                GasTransportBehaviour transport = transport(helper);
                FlowState westFlow = transport.getFlowState(Direction.WEST);
                FlowState eastFlow = transport.getFlowState(Direction.EAST);
                if (!hasPositiveFlow(westFlow) || !hasPositiveFlow(eastFlow)) {
                    if (elapsedTicks[0] > FLOW_DEADLINE_TICKS) {
                        helper.fail("Ordinary airtight pipe never established its initial flow before casing");
                    }
                    return;
                }

                assertPositiveForwardFlow(helper, westFlow, eastFlow, "uncased airtight pipe");
                helper.assertValueEqual(transport.getThroughputFlowRate(), westFlow.flowRate(), "uncased airtight pipe throughput");
                assertStraightPipeConnections(helper, transport, state, "uncased airtight pipe");
                assertSinglePipeTopology(helper, GasNetworkTopology.get(level, absolutePipePos), "uncased airtight pipe");
                initialFlowRate[0] = westFlow.flowRate();

                applyAirtightSheetCasing(helper, player, state);

                BlockState casedState = level.getBlockState(absolutePipePos);
                helper.assertTrue(casedState.is(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get()), "Applying an airtight sheet replaced the ordinary airtight pipe block type");
                helper.assertTrue(casedState.getValue(AirtightPipeBlock.CASED), "Applying an airtight sheet did not set CASED=true");
                helper.assertTrue(casedState.getValue(RotatedPillarBlock.AXIS) == Axis.X, "Applying an airtight sheet changed the pipe axis");

                GasTransportBehaviour casedTransport = transport(helper);
                assertStraightPipeConnections(helper, casedTransport, casedState, "newly cased airtight pipe");
                assertSinglePipeTopology(helper, GasNetworkTopology.get(level, absolutePipePos), "newly cased airtight pipe");

                phase[0] = 1;
                recoveryTicks[0] = 0;
                return;
            }

            recoveryTicks[0]++;
            helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get()), "Cased airtight pipe changed block type while waiting for flow recovery");
            helper.assertTrue(state.getValue(AirtightPipeBlock.CASED), "Cased airtight pipe lost its casing while waiting for flow recovery");
            helper.assertTrue(state.getValue(RotatedPillarBlock.AXIS) == Axis.X, "Cased airtight pipe changed axis while waiting for flow recovery");

            GasTransportBehaviour transport = transport(helper);
            FlowState westFlow = transport.getFlowState(Direction.WEST);
            FlowState eastFlow = transport.getFlowState(Direction.EAST);
            if (!hasPositiveFlow(westFlow) || !hasPositiveFlow(eastFlow)) {
                if (recoveryTicks[0] > CASING_RECOVERY_DEADLINE_TICKS) {
                    helper.fail("Gas flow did not recover promptly after the ordinary airtight pipe was cased");
                }
                return;
            }

            assertPositiveForwardFlow(helper, westFlow, eastFlow, "cased airtight pipe");
            helper.assertValueEqual(westFlow.flowRate(), initialFlowRate[0], "flow rate after ordinary airtight pipe casing");
            helper.assertValueEqual(transport.getThroughputFlowRate(), initialFlowRate[0], "throughput after ordinary airtight pipe casing");
            assertStraightPipeConnections(helper, transport, state, "cased airtight pipe");
            assertSinglePipeTopology(helper, GasNetworkTopology.get(level, absolutePipePos), "cased airtight pipe");
            helper.succeed();
        });
    }

    private static void applyAirtightSheetCasing(GameTestHelper helper, Player player, BlockState state) {
        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        ItemStack sheetStack = new ItemStack(CCBItems.AIRTIGHT_SHEET.asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, sheetStack);

        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePipePos).add(0, 0.5, 0), Direction.UP, absolutePipePos, false);
        ItemInteractionResult result = state.useItemOn(sheetStack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hitResult);
        helper.assertTrue(result.consumesAction(), "Using an airtight sheet on the ordinary airtight pipe did not consume the block interaction");
    }

    private static void assertInheritedXAxisOpenings(GameTestHelper helper, BlockState state) {
        helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get()), "Ordinary airtight pipe was not replaced by an airtight encased pipe");
        helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)), "Replacement did not inherit the west opening from the original X-axis pipe");
        helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)), "Replacement did not inherit the east opening from the original X-axis pipe");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.NORTH)), "Replacement unexpectedly opened the north face");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.SOUTH)), "Replacement unexpectedly opened the south face");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP)), "Replacement unexpectedly opened the up face");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.DOWN)), "Replacement unexpectedly opened the down face");
    }

    private static void assertStraightPipeConnections(GameTestHelper helper, GasTransportBehaviour transport, BlockState state, String description) {
        helper.assertTrue(transport.isConnectionFaceEnabled(state, Direction.WEST), description + " west face was not enabled");
        helper.assertTrue(transport.isConnectionFaceEnabled(state, Direction.EAST), description + " east face was not enabled");
        helper.assertTrue(!transport.isConnectionFaceEnabled(state, Direction.NORTH), description + " north face unexpectedly became enabled");
        helper.assertTrue(!transport.isConnectionFaceEnabled(state, Direction.SOUTH), description + " south face unexpectedly became enabled");
        helper.assertTrue(!transport.isConnectionFaceEnabled(state, Direction.UP), description + " up face unexpectedly became enabled");
        helper.assertTrue(!transport.isConnectionFaceEnabled(state, Direction.DOWN), description + " down face unexpectedly became enabled");

        helper.assertTrue(transport.getConnection(Direction.WEST) != null, description + " lost its west gas connection");
        helper.assertTrue(transport.getConnection(Direction.EAST) != null, description + " lost its east gas connection");
        helper.assertTrue(transport.getConnection(Direction.NORTH) == null, description + " gained a ghost north gas connection");
        helper.assertTrue(transport.getConnection(Direction.SOUTH) == null, description + " gained a ghost south gas connection");
        helper.assertTrue(transport.getConnection(Direction.UP) == null, description + " gained a ghost up gas connection");
        helper.assertTrue(transport.getConnection(Direction.DOWN) == null, description + " gained a ghost down gas connection");
    }

    private static void assertSinglePipeTopology(GameTestHelper helper, Snapshot snapshot, String description) {
        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        helper.assertTrue(snapshot.pipePositions().size() == 1, description + " topology did not contain exactly one transport pipe");
        helper.assertTrue(snapshot.pipePositions().contains(absolutePipePos), description + " topology did not contain the tested pipe");
        helper.assertTrue(snapshot.endpointFaces().size() == 1, description + " topology did not contain exactly the source endpoint face");
        helper.assertTrue(snapshot.atmosphericFaces().size() == 1, description + " topology did not contain exactly the east atmospheric face");
    }

    private static void assertPositiveForwardFlow(GameTestHelper helper, @Nullable FlowState westFlow, @Nullable FlowState eastFlow, String description) {
        helper.assertTrue(westFlow != null, description + " did not have west-face flow");
        if (westFlow == null) {
            throw new NullPointerException("Required test object is missing: " + description + " did not have west-face flow" + '.');
        }

        helper.assertTrue(eastFlow != null, description + " did not have east-face flow");
        if (eastFlow == null) {
            throw new NullPointerException("Required test object is missing: " + description + " did not have east-face flow" + '.');
        }

        helper.assertTrue(westFlow.direction() == FlowDirection.INBOUND, description + " west face was not inbound");
        helper.assertTrue(eastFlow.direction() == FlowDirection.OUTBOUND, description + " east face was not outbound");
        helper.assertTrue(westFlow.flowRate() > 0, description + " flow rate was not positive");
        helper.assertValueEqual(eastFlow.flowRate(), westFlow.flowRate(), description + " face flow rate");
        helper.assertTrue(westFlow.gas().is(CCBGases.NATURAL_AIR.get()), description + " west-face gas was not Natural Air");
        helper.assertTrue(eastFlow.gas().is(CCBGases.NATURAL_AIR.get()), description + " east-face gas was not Natural Air");
    }

    private static boolean hasPositiveFlow(@Nullable FlowState flowState) {
        return flowState != null && flowState.flowRate() > 0;
    }

    private static BlockState uncasedPipeState() {
        return CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X).setValue(AirtightPipeBlock.CASED, false);
    }

    private static void placeCreativeSource(GameTestHelper helper) {
        helper.setBlock(SOURCE_POS, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(SOURCE_POS));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight source tank block entity was not initialized at " + SOURCE_POS);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Creative airtight source tank block entity missing at " + SOURCE_POS + '.');
        }

        tank.getTankInventory().setFixedPressurePa(SOURCE_PRESSURE_PA);
        tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        helper.assertValueEqual(tank.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "creative source pressure at " + SOURCE_POS);
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized at " + PIPE_POS);
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized at " + PIPE_POS + '.');
        }

        return transport;
    }
}
