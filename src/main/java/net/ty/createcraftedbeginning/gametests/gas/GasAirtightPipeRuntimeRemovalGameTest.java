package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
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
public final class GasAirtightPipeRuntimeRemovalGameTest {
    private static final long SOURCE_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final long SINK_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final int INITIAL_FLOW_DEADLINE_TICKS = 30;
    private static final int TRANSITION_DEADLINE_TICKS = 5;

    private static final BlockPos SOURCE_POS = new BlockPos(1, 1, 3);
    private static final BlockPos UPSTREAM_PIPE_POS = new BlockPos(2, 1, 3);
    private static final BlockPos MIDDLE_PIPE_POS = new BlockPos(3, 1, 3);
    private static final BlockPos DOWNSTREAM_PIPE_POS = new BlockPos(4, 1, 3);
    private static final BlockPos SINK_POS = new BlockPos(5, 1, 3);

    private GasAirtightPipeRuntimeRemovalGameTest() {
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void removingAndReplacingOrdinaryAirtightPipeSplitsAndRestoresTopology(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        placePipe(helper, UPSTREAM_PIPE_POS);
        placePipe(helper, MIDDLE_PIPE_POS);
        placePipe(helper, DOWNSTREAM_PIPE_POS);
        placeCreativeBoundary(helper, SINK_POS, SINK_PRESSURE_PA);

        Snapshot[] snapshots = new Snapshot[4];
        long[] initialFlowRate = new long[1];
        int[] phase = new int[1];
        int[] testTicks = new int[1];
        int[] transitionTicks = new int[1];

        helper.onEachTick(() -> {
            testTicks[0]++;

            if (phase[0] == 0) {
                GasTransportBehaviour upstream = transport(helper, UPSTREAM_PIPE_POS);
                GasTransportBehaviour middle = transport(helper, MIDDLE_PIPE_POS);
                GasTransportBehaviour downstream = transport(helper, DOWNSTREAM_PIPE_POS);
                if (!hasPositiveFlowOnAxis(upstream) || !hasPositiveFlowOnAxis(middle) || !hasPositiveFlowOnAxis(downstream)) {
                    if (testTicks[0] > INITIAL_FLOW_DEADLINE_TICKS) {
                        helper.fail("Three-pipe airtight network never established its initial flow before the runtime removal test");
                    }
                    return;
                }

                long upstreamFlow = assertForwardFlow(helper, upstream, "upstream airtight pipe initial");
                long middleFlow = assertForwardFlow(helper, middle, "middle airtight pipe initial");
                long downstreamFlow = assertForwardFlow(helper, downstream, "downstream airtight pipe initial");
                helper.assertValueEqual(middleFlow, upstreamFlow, "initial upstream/middle pipe flow rate");
                helper.assertValueEqual(downstreamFlow, upstreamFlow, "initial upstream/downstream pipe flow rate");
                initialFlowRate[0] = upstreamFlow;

                BlockPos absoluteUpstream = helper.absolutePos(UPSTREAM_PIPE_POS);
                BlockPos absoluteMiddle = helper.absolutePos(MIDDLE_PIPE_POS);
                BlockPos absoluteDownstream = helper.absolutePos(DOWNSTREAM_PIPE_POS);
                snapshots[0] = GasNetworkTopology.get(level, absoluteUpstream);
                assertConnectedThreePipeTopology(helper, snapshots[0], absoluteUpstream, absoluteMiddle, absoluteDownstream, "initial");
                helper.assertTrue(GasNetworkTopology.get(level, absoluteMiddle) == snapshots[0], "Initial middle pipe did not share the three-pipe topology snapshot");
                helper.assertTrue(GasNetworkTopology.get(level, absoluteDownstream) == snapshots[0], "Initial downstream pipe did not share the three-pipe topology snapshot");

                boolean destroyed = level.destroyBlock(absoluteMiddle, false);
                helper.assertTrue(destroyed, "Runtime removal did not destroy the middle airtight pipe");
                helper.assertTrue(level.getBlockState(absoluteMiddle).isAir(), "Middle airtight pipe position was not air after runtime removal");
                helper.assertTrue(BlockEntityBehaviour.get(level, absoluteMiddle, GasTransportBehaviour.TYPE) == null, "Removed middle airtight pipe retained a GasTransportBehaviour");

                Snapshot upstreamRemovedSnapshot = GasNetworkTopology.get(level, absoluteUpstream);
                Snapshot downstreamRemovedSnapshot = GasNetworkTopology.get(level, absoluteDownstream);
                helper.assertTrue(upstreamRemovedSnapshot != snapshots[0], "Removing the middle airtight pipe did not invalidate the upstream cached topology snapshot");
                helper.assertTrue(downstreamRemovedSnapshot != snapshots[0], "Removing the middle airtight pipe did not invalidate the downstream cached topology snapshot");
                helper.assertTrue(upstreamRemovedSnapshot != downstreamRemovedSnapshot, "Removing the middle airtight pipe did not split the network into two topology snapshots");
                assertSplitTopology(helper, upstreamRemovedSnapshot, absoluteUpstream, absoluteMiddle, absoluteDownstream, "upstream");
                assertSplitTopology(helper, downstreamRemovedSnapshot, absoluteDownstream, absoluteMiddle, absoluteUpstream, "downstream");

                phase[0] = 1;
                transitionTicks[0] = 0;
                return;
            }

            transitionTicks[0]++;
            if (phase[0] == 1) {
                GasTransportBehaviour upstream = transport(helper, UPSTREAM_PIPE_POS);
                GasTransportBehaviour downstream = transport(helper, DOWNSTREAM_PIPE_POS);
                FlowState upstreamWest = upstream.getFlowState(Direction.WEST);
                FlowState upstreamEast = upstream.getFlowState(Direction.EAST);
                FlowState downstreamWest = downstream.getFlowState(Direction.WEST);
                FlowState downstreamEast = downstream.getFlowState(Direction.EAST);

                boolean sourceSideVenting = isPositiveFlow(upstreamWest, FlowDirection.INBOUND) && isPositiveFlow(upstreamEast, FlowDirection.OUTBOUND);
                boolean downstreamStopped = downstreamWest == null && downstreamEast == null && downstream.getThroughputFlowRate() == 0;
                if (!sourceSideVenting || !downstreamStopped) {
                    if (transitionTicks[0] > TRANSITION_DEADLINE_TICKS) {
                        helper.fail("Gas flow did not transition promptly to the split-network state after the middle airtight pipe was removed");
                    }
                    return;
                }

                helper.assertValueEqual(upstreamEast.flowRate(), upstreamWest.flowRate(), "split source-side pipe face flow rate");
                helper.assertValueEqual(upstream.getThroughputFlowRate(), upstreamWest.flowRate(), "split source-side pipe throughput");
                helper.assertTrue(upstreamWest.gas().is(CCBGases.NATURAL_AIR.get()), "Split source-side inlet gas was not Natural Air");
                helper.assertTrue(upstreamEast.gas().is(CCBGases.NATURAL_AIR.get()), "Split source-side atmospheric outlet gas was not Natural Air");

                BlockPos absoluteUpstream = helper.absolutePos(UPSTREAM_PIPE_POS);
                BlockPos absoluteMiddle = helper.absolutePos(MIDDLE_PIPE_POS);
                BlockPos absoluteDownstream = helper.absolutePos(DOWNSTREAM_PIPE_POS);
                snapshots[1] = GasNetworkTopology.get(level, absoluteUpstream);
                snapshots[2] = GasNetworkTopology.get(level, absoluteDownstream);
                helper.assertTrue(snapshots[1] != snapshots[2], "Split network unexpectedly shared one topology snapshot before the pipe was replaced");
                assertSplitTopology(helper, snapshots[1], absoluteUpstream, absoluteMiddle, absoluteDownstream, "upstream stable");
                assertSplitTopology(helper, snapshots[2], absoluteDownstream, absoluteMiddle, absoluteUpstream, "downstream stable");

                placePipe(helper, MIDDLE_PIPE_POS);
                helper.assertTrue(level.getBlockState(helper.absolutePos(MIDDLE_PIPE_POS)).is(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get()), "Middle airtight pipe was not restored after runtime replacement");

                phase[0] = 2;
                transitionTicks[0] = 0;
                return;
            }

            GasTransportBehaviour upstream = transport(helper, UPSTREAM_PIPE_POS);
            GasTransportBehaviour middle = transport(helper, MIDDLE_PIPE_POS);
            GasTransportBehaviour downstream = transport(helper, DOWNSTREAM_PIPE_POS);
            if (!hasPositiveFlowOnAxis(upstream) || !hasPositiveFlowOnAxis(middle) || !hasPositiveFlowOnAxis(downstream)) {
                if (transitionTicks[0] > TRANSITION_DEADLINE_TICKS) {
                    helper.fail("Gas flow did not recover promptly after the middle airtight pipe was replaced");
                }
                return;
            }

            long restoredUpstreamFlow = assertForwardFlow(helper, upstream, "upstream airtight pipe restored");
            long restoredMiddleFlow = assertForwardFlow(helper, middle, "middle airtight pipe restored");
            long restoredDownstreamFlow = assertForwardFlow(helper, downstream, "downstream airtight pipe restored");
            helper.assertValueEqual(restoredMiddleFlow, restoredUpstreamFlow, "restored upstream/middle pipe flow rate");
            helper.assertValueEqual(restoredDownstreamFlow, restoredUpstreamFlow, "restored upstream/downstream pipe flow rate");
            helper.assertValueEqual(restoredUpstreamFlow, initialFlowRate[0], "restored three-pipe flow rate versus initial flow rate");

            BlockPos absoluteUpstream = helper.absolutePos(UPSTREAM_PIPE_POS);
            BlockPos absoluteMiddle = helper.absolutePos(MIDDLE_PIPE_POS);
            BlockPos absoluteDownstream = helper.absolutePos(DOWNSTREAM_PIPE_POS);
            snapshots[3] = GasNetworkTopology.get(level, absoluteUpstream);
            helper.assertTrue(snapshots[3] != snapshots[1], "Replacing the middle airtight pipe did not invalidate the upstream split topology snapshot");
            helper.assertTrue(snapshots[3] != snapshots[2], "Replacing the middle airtight pipe did not invalidate the downstream split topology snapshot");
            assertConnectedThreePipeTopology(helper, snapshots[3], absoluteUpstream, absoluteMiddle, absoluteDownstream, "restored");
            helper.assertTrue(GasNetworkTopology.get(level, absoluteMiddle) == snapshots[3], "Restored middle pipe did not share the merged topology snapshot");
            helper.assertTrue(GasNetworkTopology.get(level, absoluteDownstream) == snapshots[3], "Restored downstream pipe did not share the merged topology snapshot");
            helper.succeed();
        });
    }

    private static void assertConnectedThreePipeTopology(GameTestHelper helper, Snapshot snapshot, BlockPos upstream, BlockPos middle, BlockPos downstream, String stage) {
        helper.assertTrue(snapshot.pipePositions().size() == 3, stage + " topology did not contain exactly three airtight pipes");
        helper.assertTrue(snapshot.pipePositions().contains(upstream), stage + " topology did not contain the upstream airtight pipe");
        helper.assertTrue(snapshot.pipePositions().contains(middle), stage + " topology did not contain the middle airtight pipe");
        helper.assertTrue(snapshot.pipePositions().contains(downstream), stage + " topology did not contain the downstream airtight pipe");
        helper.assertTrue(snapshot.endpointFaces().size() == 2, stage + " topology did not contain exactly the source and sink endpoint faces");
        helper.assertTrue(snapshot.atmosphericFaces().isEmpty(), stage + " connected three-pipe topology unexpectedly contained an atmospheric face");
    }

    private static void assertSplitTopology(GameTestHelper helper, Snapshot snapshot, BlockPos expectedPipe, BlockPos removedPipe, BlockPos oppositePipe, String side) {
        helper.assertTrue(snapshot.pipePositions().size() == 1, "Runtime removal left unexpected pipes in the " + side + " split topology");
        helper.assertTrue(snapshot.pipePositions().contains(expectedPipe), "Runtime removal lost the expected pipe from the " + side + " split topology");
        helper.assertTrue(!snapshot.pipePositions().contains(removedPipe), "Removed middle pipe remained in the " + side + " split topology");
        helper.assertTrue(!snapshot.pipePositions().contains(oppositePipe), "Runtime removal left the opposite-side pipe in the " + side + " split topology");
        helper.assertTrue(snapshot.endpointFaces().size() == 1, side + " split topology did not contain exactly one endpoint face");
        helper.assertTrue(snapshot.atmosphericFaces().size() == 1, side + " split topology did not expose exactly one atmospheric break face");
    }

    private static boolean hasPositiveFlowOnAxis(GasTransportBehaviour transport) {
        return isPositiveFlow(transport.getFlowState(Direction.WEST), FlowDirection.INBOUND) && isPositiveFlow(transport.getFlowState(Direction.EAST), FlowDirection.OUTBOUND);
    }

    private static boolean isPositiveFlow(@Nullable FlowState flowState, FlowDirection expectedDirection) {
        return flowState != null && flowState.direction() == expectedDirection && flowState.flowRate() > 0;
    }

    private static long assertForwardFlow(GameTestHelper helper, GasTransportBehaviour transport, String label) {
        FlowState west = transport.getFlowState(Direction.WEST);
        FlowState east = transport.getFlowState(Direction.EAST);
        helper.assertTrue(west != null, label + " did not have west-face flow");
        if (west == null) {
            throw new NullPointerException("Required test object is missing: " + label + " did not have west-face flow" + '.');
        }

        helper.assertTrue(east != null, label + " did not have east-face flow");
        if (east == null) {
            throw new NullPointerException("Required test object is missing: " + label + " did not have east-face flow" + '.');
        }

        helper.assertTrue(west.direction() == FlowDirection.INBOUND, label + " west-face flow was not inbound");
        helper.assertTrue(east.direction() == FlowDirection.OUTBOUND, label + " east-face flow was not outbound");
        helper.assertTrue(west.flowRate() > 0, label + " flow rate was not positive");
        helper.assertValueEqual(east.flowRate(), west.flowRate(), label + " face flow rate");
        helper.assertValueEqual(transport.getThroughputFlowRate(), west.flowRate(), label + " throughput");
        helper.assertTrue(west.gas().is(CCBGases.NATURAL_AIR.get()), label + " west-face gas was not Natural Air");
        helper.assertTrue(east.gas().is(CCBGases.NATURAL_AIR.get()), label + " east-face gas was not Natural Air");
        return west.flowRate();
    }

    private static void placePipe(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
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
