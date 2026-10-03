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
import net.minecraft.world.entity.player.Player;
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
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasEncasedPipeDynamicOpeningGameTest {
    private static final long SOURCE_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final long SINK_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final int INITIAL_FLOW_DEADLINE_TICKS = 30;
    private static final int TRANSITION_DEADLINE_TICKS = 5;

    private static final BlockPos SOURCE_POS = new BlockPos(1, 1, 3);
    private static final BlockPos ENCASED_PIPE_POS = new BlockPos(2, 1, 3);
    private static final BlockPos DOWNSTREAM_PIPE_POS = new BlockPos(3, 1, 3);
    private static final BlockPos SINK_POS = new BlockPos(4, 1, 3);

    private GasEncasedPipeDynamicOpeningGameTest() {
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void encasedPipeRuntimeOpeningToggleInvalidatesAndRestoresTopology(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        placeCreativeBoundary(helper, SINK_POS, SINK_PRESSURE_PA);
        helper.setBlock(ENCASED_PIPE_POS, CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState().setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST), true).setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST), true));
        helper.setBlock(DOWNSTREAM_PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        Player wrenchPlayer = helper.makeMockPlayer(GameType.CREATIVE);
        Snapshot[] snapshots = new Snapshot[3];
        long[] initialFlowRate = new long[1];
        int[] phase = new int[1];
        int[] testTicks = new int[1];
        int[] transitionTicks = new int[1];

        helper.onEachTick(() -> {
            testTicks[0]++;

            if (phase[0] == 0) {
                GasTransportBehaviour encasedTransport = transport(helper, ENCASED_PIPE_POS);
                GasTransportBehaviour downstreamTransport = transport(helper, DOWNSTREAM_PIPE_POS);
                FlowState encasedWest = encasedTransport.getFlowState(Direction.WEST);
                FlowState encasedEast = encasedTransport.getFlowState(Direction.EAST);
                FlowState downstreamWest = downstreamTransport.getFlowState(Direction.WEST);
                FlowState downstreamEast = downstreamTransport.getFlowState(Direction.EAST);
                if (!hasPositiveFlow(encasedWest) || !hasPositiveFlow(encasedEast) || !hasPositiveFlow(downstreamWest) || !hasPositiveFlow(downstreamEast)) {
                    if (testTicks[0] > INITIAL_FLOW_DEADLINE_TICKS) {
                        helper.fail("Airtight encased pipe network never established its initial flow before the dynamic opening test");
                    }
                    return;
                }

                assertForwardFlow(helper, encasedWest, encasedEast, "airtight encased pipe initial");
                assertForwardFlow(helper, downstreamWest, downstreamEast, "downstream airtight pipe initial");
                helper.assertValueEqual(encasedEast.flowRate(), downstreamWest.flowRate(), "initial flow across the encased/downstream pipe connection");
                helper.assertValueEqual(encasedTransport.getThroughputFlowRate(), encasedWest.flowRate(), "initial airtight encased pipe throughput");
                helper.assertValueEqual(downstreamTransport.getThroughputFlowRate(), downstreamWest.flowRate(), "initial downstream airtight pipe throughput");
                initialFlowRate[0] = encasedWest.flowRate();

                BlockPos absoluteEncasedPos = helper.absolutePos(ENCASED_PIPE_POS);
                BlockPos absoluteDownstreamPos = helper.absolutePos(DOWNSTREAM_PIPE_POS);
                snapshots[0] = GasNetworkTopology.get(level, absoluteEncasedPos);
                helper.assertTrue(snapshots[0].pipePositions().size() == 2, "Initial topology did not contain exactly the encased pipe and downstream pipe");
                helper.assertTrue(snapshots[0].pipePositions().contains(absoluteEncasedPos), "Initial topology did not contain the airtight encased pipe");
                helper.assertTrue(snapshots[0].pipePositions().contains(absoluteDownstreamPos), "Initial topology did not contain the downstream airtight pipe");
                helper.assertTrue(GasNetworkTopology.get(level, absoluteDownstreamPos) == snapshots[0], "Initially connected pipes did not share the same cached topology snapshot");

                toggleEastFaceWithWrench(helper, wrenchPlayer, false);
                snapshots[1] = GasNetworkTopology.get(level, absoluteEncasedPos);
                helper.assertTrue(snapshots[1] != snapshots[0], "Closing the airtight encased pipe east face did not invalidate its cached topology snapshot");
                helper.assertTrue(snapshots[1].pipePositions().size() == 1, "Closed airtight encased pipe topology did not split away from the downstream pipe");
                helper.assertTrue(snapshots[1].pipePositions().contains(absoluteEncasedPos), "Closed topology lost the airtight encased pipe itself");
                helper.assertTrue(!snapshots[1].pipePositions().contains(absoluteDownstreamPos), "Closed east face left the downstream pipe in the airtight encased pipe topology");

                Snapshot downstreamClosedSnapshot = GasNetworkTopology.get(level, absoluteDownstreamPos);
                helper.assertTrue(downstreamClosedSnapshot != snapshots[1], "Closing the east face did not split the downstream pipe into a separate topology snapshot");
                helper.assertTrue(downstreamClosedSnapshot.pipePositions().size() == 1, "Downstream topology after closing the east face contained unexpected pipes");
                helper.assertTrue(downstreamClosedSnapshot.pipePositions().contains(absoluteDownstreamPos), "Downstream topology after closing the east face lost the downstream pipe");
                helper.assertTrue(!downstreamClosedSnapshot.pipePositions().contains(absoluteEncasedPos), "Downstream topology still contained the airtight encased pipe after the east face closed");

                phase[0] = 1;
                transitionTicks[0] = 0;
                return;
            }

            transitionTicks[0]++;
            if (phase[0] == 1) {
                assertEastFaceState(helper, false);
                GasTransportBehaviour encasedTransport = transport(helper, ENCASED_PIPE_POS);
                GasTransportBehaviour downstreamTransport = transport(helper, DOWNSTREAM_PIPE_POS);
                if (encasedTransport.getFlowState(Direction.WEST) != null || encasedTransport.getFlowState(Direction.EAST) != null || encasedTransport.getThroughputFlowRate() != 0 || downstreamTransport.getFlowState(Direction.WEST) != null || downstreamTransport.getFlowState(Direction.EAST) != null || downstreamTransport.getThroughputFlowRate() != 0) {
                    if (transitionTicks[0] > TRANSITION_DEADLINE_TICKS) {
                        helper.fail("Gas flow did not stop promptly after the airtight encased pipe east face was closed");
                    }
                    return;
                }

                helper.assertTrue(encasedTransport.getConnection(Direction.EAST) == null, "Closed east face retained an active GasPipeConnection");
                helper.assertTrue(encasedTransport.getFlowState(Direction.NORTH) == null, "Closed network produced ghost north flow on the airtight encased pipe");
                helper.assertTrue(encasedTransport.getFlowState(Direction.SOUTH) == null, "Closed network produced ghost south flow on the airtight encased pipe");
                helper.assertTrue(encasedTransport.getFlowState(Direction.UP) == null, "Closed network produced ghost up flow on the airtight encased pipe");
                helper.assertTrue(encasedTransport.getFlowState(Direction.DOWN) == null, "Closed network produced ghost down flow on the airtight encased pipe");

                toggleEastFaceWithWrench(helper, wrenchPlayer, true);
                BlockPos absoluteEncasedPos = helper.absolutePos(ENCASED_PIPE_POS);
                BlockPos absoluteDownstreamPos = helper.absolutePos(DOWNSTREAM_PIPE_POS);
                snapshots[2] = GasNetworkTopology.get(level, absoluteEncasedPos);
                helper.assertTrue(snapshots[2] != snapshots[1], "Reopening the airtight encased pipe east face did not invalidate the split topology snapshot");
                helper.assertTrue(snapshots[2].pipePositions().size() == 2, "Reopened airtight encased pipe topology did not merge with the downstream pipe");
                helper.assertTrue(snapshots[2].pipePositions().contains(absoluteEncasedPos), "Reopened topology did not contain the airtight encased pipe");
                helper.assertTrue(snapshots[2].pipePositions().contains(absoluteDownstreamPos), "Reopened topology did not contain the downstream airtight pipe");
                helper.assertTrue(GasNetworkTopology.get(level, absoluteDownstreamPos) == snapshots[2], "Reopened pipes did not share the same merged cached topology snapshot");

                phase[0] = 2;
                transitionTicks[0] = 0;
                return;
            }

            if (phase[0] != 2) {
                return;
            }

            assertEastFaceState(helper, true);
            GasTransportBehaviour encasedTransport = transport(helper, ENCASED_PIPE_POS);
            GasTransportBehaviour downstreamTransport = transport(helper, DOWNSTREAM_PIPE_POS);
            FlowState encasedWest = encasedTransport.getFlowState(Direction.WEST);
            FlowState encasedEast = encasedTransport.getFlowState(Direction.EAST);
            FlowState downstreamWest = downstreamTransport.getFlowState(Direction.WEST);
            FlowState downstreamEast = downstreamTransport.getFlowState(Direction.EAST);
            if (!hasPositiveFlow(encasedWest) || !hasPositiveFlow(encasedEast) || !hasPositiveFlow(downstreamWest) || !hasPositiveFlow(downstreamEast)) {
                if (transitionTicks[0] > TRANSITION_DEADLINE_TICKS) {
                    helper.fail("Gas flow did not recover promptly after the airtight encased pipe east face was reopened");
                }
                return;
            }

            assertForwardFlow(helper, encasedWest, encasedEast, "airtight encased pipe restored");
            assertForwardFlow(helper, downstreamWest, downstreamEast, "downstream airtight pipe restored");

            helper.assertValueEqual(encasedEast.flowRate(), downstreamWest.flowRate(), "restored flow across the encased/downstream pipe connection");
            helper.assertValueEqual(encasedTransport.getThroughputFlowRate(), encasedWest.flowRate(), "restored airtight encased pipe throughput");
            helper.assertValueEqual(downstreamTransport.getThroughputFlowRate(), downstreamWest.flowRate(), "restored downstream airtight pipe throughput");
            helper.assertValueEqual(encasedWest.flowRate(), initialFlowRate[0], "airtight encased pipe flow after topology restoration");

            Snapshot restoredSnapshot = GasNetworkTopology.get(level, helper.absolutePos(ENCASED_PIPE_POS));
            helper.assertTrue(restoredSnapshot.pipePositions().size() == 2, "Restored topology did not contain exactly both connected pipes");
            helper.assertTrue(restoredSnapshot.pipePositions().contains(helper.absolutePos(ENCASED_PIPE_POS)), "Restored topology lost the airtight encased pipe");
            helper.assertTrue(restoredSnapshot.pipePositions().contains(helper.absolutePos(DOWNSTREAM_PIPE_POS)), "Restored topology lost the downstream airtight pipe");
            helper.assertTrue(GasNetworkTopology.get(level, helper.absolutePos(DOWNSTREAM_PIPE_POS)) == restoredSnapshot, "Restored pipes did not share one cached topology snapshot");
            helper.succeed();
        });
    }

    private static boolean hasPositiveFlow(@Nullable FlowState flowState) {
        return flowState != null && flowState.flowRate() > 0;
    }

    private static void assertForwardFlow(GameTestHelper helper, FlowState westFlow, FlowState eastFlow, String description) {
        helper.assertTrue(westFlow.direction() == FlowDirection.INBOUND, description + " west face was not inbound");
        helper.assertTrue(eastFlow.direction() == FlowDirection.OUTBOUND, description + " east face was not outbound");
        helper.assertTrue(westFlow.gas().is(CCBGases.NATURAL_AIR.get()), description + " west face gas was not Natural Air");
        helper.assertTrue(eastFlow.gas().is(CCBGases.NATURAL_AIR.get()), description + " east face gas was not Natural Air");
        helper.assertValueEqual(eastFlow.flowRate(), westFlow.flowRate(), description + " face flow rate");
    }

    private static void toggleEastFaceWithWrench(GameTestHelper helper, Player player, boolean expectedOpen) {
        BlockPos absolutePos = helper.absolutePos(ENCASED_PIPE_POS);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get()), "Airtight encased pipe was missing before its east face was toggled");
        helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)) != expectedOpen, "Airtight encased pipe east face was already in the requested state before wrenching");

        Vec3 hitLocation = Vec3.atCenterOf(absolutePos).add(0.5, 0, 0);
        BlockHitResult hitResult = new BlockHitResult(hitLocation, Direction.EAST, absolutePos, false);
        InteractionResult result = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().onWrenched(state, new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
        helper.assertTrue(result == InteractionResult.SUCCESS, "Wrenching the airtight encased pipe east face did not succeed");
        assertEastFaceState(helper, expectedOpen);
    }

    private static void assertEastFaceState(GameTestHelper helper, boolean expectedOpen) {
        BlockState state = helper.getLevel().getBlockState(helper.absolutePos(ENCASED_PIPE_POS));
        helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get()), "Airtight encased pipe disappeared during the dynamic opening test");
        helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)) == expectedOpen, "Airtight encased pipe east face did not match its expected runtime opening state");
        helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)), "Airtight encased pipe west face unexpectedly closed during the dynamic opening test");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.NORTH)), "Airtight encased pipe north face unexpectedly opened during the dynamic opening test");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.SOUTH)), "Airtight encased pipe south face unexpectedly opened during the dynamic opening test");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP)), "Airtight encased pipe up face unexpectedly opened during the dynamic opening test");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.DOWN)), "Airtight encased pipe down face unexpectedly opened during the dynamic opening test");
    }

    private static void placeCreativeBoundary(GameTestHelper helper, BlockPos pos, long pressurePa) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Creative airtight tank block entity missing at " + pos + '.');
        }

        tank.getTankInventory().setFixedPressurePa(pressurePa);
        tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        helper.assertValueEqual(tank.getTankInventory().getPressurePa(), pressurePa, "creative boundary pressure at " + pos);
    }

    private static GasTransportBehaviour transport(GameTestHelper helper, BlockPos pos) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(pos), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized at " + pos);
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized at " + pos + '.');
        }

        return transport;
    }
}
