package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasFilteringBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasSmartAirtightPipeDynamicFilterGameTest {
    private static final long SOURCE_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final long SINK_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final int INITIAL_FLOW_DEADLINE_TICKS = 30;
    private static final int TRANSITION_DEADLINE_TICKS = 5;

    private static final BlockPos SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SINK_POS = new BlockPos(2, 1, 1);

    private GasSmartAirtightPipeDynamicFilterGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void changingSmartAirtightPipeFilterStopsAndRestoresFlow(GameTestHelper helper) {
        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        helper.setBlock(PIPE_POS, CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        placeCreativeBoundary(helper, SINK_POS, SINK_PRESSURE_PA);

        GasFilteringBehaviour filter = filter(helper);
        GasTransportBehaviour transport = transport(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        setFilterWithInteraction(helper, filter, player, CCBGases.NATURAL_AIR.get());
        assertFilter(helper, filter, CCBGases.NATURAL_AIR.get(), CCBGases.ULTRAWARM_AIR.get(), "initial Natural Air");
        assertTopology(helper, "initial");

        long[] initialFlowRate = new long[1];
        int[] phase = new int[1];
        int[] testTicks = new int[1];
        int[] transitionTicks = new int[1];

        helper.onEachTick(() -> {
            testTicks[0]++;

            if (phase[0] == 0) {
                if (!hasPositiveForwardFlow(transport)) {
                    if (testTicks[0] > INITIAL_FLOW_DEADLINE_TICKS) {
                        helper.fail("Smart airtight pipe never established its initial Natural Air flow before the filter was changed");
                    }
                    return;
                }

                initialFlowRate[0] = assertForwardFlow(helper, transport, "smart airtight pipe before filter change");
                assertNoOffAxisFlow(helper, transport, "smart airtight pipe before filter change");
                assertFilter(helper, filter, CCBGases.NATURAL_AIR.get(), CCBGases.ULTRAWARM_AIR.get(), "pre-change Natural Air");
                assertTopology(helper, "pre-change");

                setFilterWithInteraction(helper, filter, player, CCBGases.ULTRAWARM_AIR.get());
                assertFilter(helper, filter, CCBGases.ULTRAWARM_AIR.get(), CCBGases.NATURAL_AIR.get(), "runtime Ultrawarm Air");
                assertTopology(helper, "immediately after blocking filter change");

                phase[0] = 1;
                transitionTicks[0] = 0;
                return;
            }

            transitionTicks[0]++;
            if (phase[0] == 1) {
                assertFilter(helper, filter, CCBGases.ULTRAWARM_AIR.get(), CCBGases.NATURAL_AIR.get(), "blocked Ultrawarm Air");
                assertTopology(helper, "blocked");

                boolean stopped = transport.getFlowState(Direction.WEST) == null && transport.getFlowState(Direction.EAST) == null && transport.getThroughputFlowRate() == 0;
                if (!stopped) {
                    if (transitionTicks[0] > TRANSITION_DEADLINE_TICKS) {
                        helper.fail("Natural Air flow did not stop promptly after the smart airtight pipe filter was changed to Ultrawarm Air");
                    }
                    return;
                }

                assertNoOffAxisFlow(helper, transport, "blocked smart airtight pipe");

                setFilterWithInteraction(helper, filter, player, CCBGases.NATURAL_AIR.get());
                assertFilter(helper, filter, CCBGases.NATURAL_AIR.get(), CCBGases.ULTRAWARM_AIR.get(), "runtime restored Natural Air");
                assertTopology(helper, "immediately after matching filter restoration");

                phase[0] = 2;
                transitionTicks[0] = 0;
                return;
            }

            if (!hasPositiveForwardFlow(transport)) {
                if (transitionTicks[0] > TRANSITION_DEADLINE_TICKS) {
                    helper.fail("Natural Air flow did not recover promptly after the smart airtight pipe filter was changed back to Natural Air");
                }
                return;
            }

            long restoredFlowRate = assertForwardFlow(helper, transport, "smart airtight pipe after filter restoration");
            helper.assertValueEqual(restoredFlowRate, initialFlowRate[0], "restored smart airtight pipe flow rate versus initial flow rate");
            assertNoOffAxisFlow(helper, transport, "restored smart airtight pipe");
            assertFilter(helper, filter, CCBGases.NATURAL_AIR.get(), CCBGases.ULTRAWARM_AIR.get(), "restored Natural Air");
            assertTopology(helper, "restored");
            helper.succeed();
        });
    }

    private static void setFilterWithInteraction(GameTestHelper helper, GasFilteringBehaviour filter, Player player, Gas gas) {
        ItemStack filterCanister = canisterFilter(gas);
        player.setItemInHand(InteractionHand.MAIN_HAND, filterCanister);

        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePipePos).add(0, 0.4, 0), Direction.UP, absolutePipePos, false);
        filter.onShortInteract(player, InteractionHand.MAIN_HAND, Direction.UP, hitResult);

        helper.assertTrue(ItemStack.isSameItemSameComponents(filter.getFilter(), filterCanister), "Smart airtight pipe did not retain the runtime " + gas + " canister filter");
    }

    private static ItemStack canisterFilter(Gas gas) {
        ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        canister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(gas, 1));
        return canister;
    }

    private static void assertFilter(GameTestHelper helper, GasFilteringBehaviour filter, Gas allowedGas, Gas rejectedGas, String stage) {
        helper.assertTrue(filter.test(new GasStack(allowedGas, 1)), stage + " filter rejected its configured gas");
        helper.assertTrue(!filter.test(new GasStack(rejectedGas, 1)), stage + " filter accepted the gas that should be rejected");
    }

    private static boolean hasPositiveForwardFlow(GasTransportBehaviour transport) {
        return isPositiveFlow(transport.getFlowState(Direction.WEST), FlowDirection.INBOUND) && isPositiveFlow(transport.getFlowState(Direction.EAST), FlowDirection.OUTBOUND);
    }

    private static boolean isPositiveFlow(@Nullable FlowState flowState, FlowDirection expectedDirection) {
        return flowState != null && flowState.direction() == expectedDirection && flowState.flowRate() > 0;
    }

    private static long assertForwardFlow(GameTestHelper helper, GasTransportBehaviour transport, String stage) {
        FlowState west = transport.getFlowState(Direction.WEST);
        FlowState east = transport.getFlowState(Direction.EAST);
        helper.assertTrue(west != null, stage + " did not have west-face flow");
        if (west == null) {
            throw new NullPointerException("Required test object is missing: " + stage + " did not have west-face flow" + '.');
        }

        helper.assertTrue(east != null, stage + " did not have east-face flow");
        if (east == null) {
            throw new NullPointerException("Required test object is missing: " + stage + " did not have east-face flow" + '.');
        }

        helper.assertTrue(west.direction() == FlowDirection.INBOUND, stage + " west face was not inbound");
        helper.assertTrue(east.direction() == FlowDirection.OUTBOUND, stage + " east face was not outbound");
        helper.assertTrue(west.flowRate() > 0, stage + " flow rate was not positive");
        helper.assertValueEqual(east.flowRate(), west.flowRate(), stage + " face flow rate");
        helper.assertValueEqual(transport.getThroughputFlowRate(), west.flowRate(), stage + " throughput");
        helper.assertTrue(west.gas().is(CCBGases.NATURAL_AIR.get()), stage + " west-face gas was not Natural Air");
        helper.assertTrue(east.gas().is(CCBGases.NATURAL_AIR.get()), stage + " east-face gas was not Natural Air");
        return west.flowRate();
    }

    private static void assertNoOffAxisFlow(GameTestHelper helper, GasTransportBehaviour transport, String stage) {
        helper.assertTrue(transport.getFlowState(Direction.NORTH) == null, stage + " produced ghost north flow");
        helper.assertTrue(transport.getFlowState(Direction.SOUTH) == null, stage + " produced ghost south flow");
        helper.assertTrue(transport.getFlowState(Direction.UP) == null, stage + " produced ghost up flow");
        helper.assertTrue(transport.getFlowState(Direction.DOWN) == null, stage + " produced ghost down flow");
    }

    private static void assertTopology(GameTestHelper helper, String stage) {
        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        Snapshot snapshot = GasNetworkTopology.get(helper.getLevel(), absolutePipePos);
        helper.assertTrue(snapshot.pipePositions().size() == 1, stage + " smart airtight pipe topology did not contain exactly one transport pipe");
        helper.assertTrue(snapshot.pipePositions().contains(absolutePipePos), stage + " smart airtight pipe topology lost the tested pipe");
        helper.assertTrue(snapshot.endpointFaces().size() == 2, stage + " smart airtight pipe topology did not contain exactly the source and sink endpoints");
        helper.assertTrue(snapshot.atmosphericFaces().isEmpty(), stage + " smart airtight pipe topology unexpectedly contained an atmospheric face");
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

    private static GasFilteringBehaviour filter(GameTestHelper helper) {
        GasFilteringBehaviour filter = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PIPE_POS), GasFilteringBehaviour.TYPE);
        helper.assertTrue(filter != null, "Smart airtight pipe filtering behaviour was not initialized");
        if (filter == null) {
            throw new NullPointerException("Smart airtight pipe filtering behaviour was not initialized.");
        }

        return filter;
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "Smart airtight pipe transport behaviour was not initialized");
        if (transport == null) {
            throw new NullPointerException("Smart airtight pipe transport behaviour was not initialized.");
        }

        return transport;
    }

}
