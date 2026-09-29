package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPipeSixWayManifoldGameTest {
    private static final long HIGH_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final long MAX_FACE_FLOW_QUANTIZATION_ERROR_GU = 4;
    private static final long MAX_SYMMETRY_ERROR_GU = 1;

    private static final BlockPos MANIFOLD = new BlockPos(1, 1, 1);
    private static final BlockPos WEST_SOURCE = MANIFOLD.relative(Direction.WEST);
    private static final BlockPos NORTH_SOURCE = MANIFOLD.relative(Direction.NORTH);
    private static final BlockPos DOWN_SOURCE = MANIFOLD.relative(Direction.DOWN);

    private static final Direction[] INLET_FACES = {Direction.WEST, Direction.NORTH, Direction.DOWN};
    private static final Direction[] OUTLET_FACES = {Direction.EAST, Direction.SOUTH, Direction.UP};

    private GasPipeSixWayManifoldGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void sixWayManifoldSharesOneCenterNode(GameTestHelper helper) {
        placeSource(helper, WEST_SOURCE);
        placeSource(helper, NORTH_SOURCE);
        placeSource(helper, DOWN_SOURCE);
        helper.setBlock(MANIFOLD, sixWayEncasedPipeState());

        helper.succeedWhen(() -> {
            GasTransportBehaviour manifold = transport(helper);

            FlowState[] inletFlows = readFlows(helper, manifold, INLET_FACES, FlowDirection.INBOUND, "inlet");
            FlowState[] outletFlows = readFlows(helper, manifold, OUTLET_FACES, FlowDirection.OUTBOUND, "outlet");
            if (containsNull(inletFlows) || containsNull(outletFlows)) {
                return;
            }

            long inboundTotal = totalFlow(inletFlows);
            long outboundTotal = totalFlow(outletFlows);
            long balanceError = Math.abs(inboundTotal - outboundTotal);
            helper.assertTrue(balanceError <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Six-way manifold flow telemetry exceeded whole-GU quantization tolerance: inlets=" + inboundTotal + ", outlets=" + outboundTotal + ", error=" + balanceError + " GU/t");

            assertSymmetric(helper, inletFlows, "Equal-pressure manifold inlets");
            assertSymmetric(helper, outletFlows, "Equivalent atmospheric manifold outlets");

            long throughput = manifold.getThroughputFlowRate();
            helper.assertTrue(throughput > 0, "Six-way manifold throughput was not positive");
            helper.assertTrue(Math.abs(throughput - inboundTotal) <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Six-way manifold throughput did not match total inlet telemetry: throughput=" + throughput + ", inlets=" + inboundTotal + " GU/t");
            helper.assertTrue(Math.abs(throughput - outboundTotal) <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Six-way manifold throughput did not match total outlet telemetry: throughput=" + throughput + ", outlets=" + outboundTotal + " GU/t");
        });
    }

    private static FlowState[] readFlows(GameTestHelper helper, GasTransportBehaviour manifold, Direction[] faces, FlowDirection expectedDirection, String role) {
        FlowState[] flows = new FlowState[faces.length];
        for (int index = 0; index < faces.length; index++) {
            Direction face = faces[index];
            FlowState flow = manifold.getFlowState(face);
            flows[index] = flow;
            helper.assertTrue(flow != null, "Six-way manifold " + role + " face " + face + " had no flow");
            if (flow == null) {
                throw new NullPointerException("Six-way manifold " + role + " face " + face + " had no flow" + '.');
            }

            helper.assertTrue(flow.direction() == expectedDirection, "Six-way manifold " + role + " face " + face + " had the wrong flow direction");
            helper.assertTrue(flow.flowRate() > 0, "Six-way manifold " + role + " face " + face + " flow was not positive");
            helper.assertTrue(flow.gas().is(CCBGases.NATURAL_AIR.get()), "Six-way manifold " + role + " face " + face + " gas was not Natural Air");
        }
        return flows;
    }

    private static boolean containsNull(FlowState[] flows) {
        for (FlowState flow : flows) {
            if (flow == null) {
                return true;
            }
        }
        return false;
    }

    private static long totalFlow(FlowState[] flows) {
        long total = 0;
        for (FlowState flow : flows) {
            total += flow.flowRate();
        }
        return total;
    }

    private static void assertSymmetric(GameTestHelper helper, FlowState[] flows, String description) {
        long minFlow = Long.MAX_VALUE;
        long maxFlow = Long.MIN_VALUE;
        for (FlowState flow : flows) {
            minFlow = Math.min(minFlow, flow.flowRate());
            maxFlow = Math.max(maxFlow, flow.flowRate());
        }

        helper.assertTrue(maxFlow - minFlow <= MAX_SYMMETRY_ERROR_GU, description + " did not split evenly: min=" + minFlow + ", max=" + maxFlow + " GU/t");
    }

    private static void placeSource(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity sourceTank)) {
            return;
        }

        sourceTank.getTankInventory().setFixedPressurePa(HIGH_PRESSURE_PA);
        sourceTank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), HIGH_PRESSURE_PA, "source tank pressure at " + pos);
    }

    private static BlockState sixWayEncasedPipeState() {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState();
        for (Direction face : Iterate.directions) {
            state = state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(face), true);
        }
        return state;
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(MANIFOLD), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized at " + MANIFOLD);
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized at " + MANIFOLD + '.');
        }

        return transport;
    }
}
