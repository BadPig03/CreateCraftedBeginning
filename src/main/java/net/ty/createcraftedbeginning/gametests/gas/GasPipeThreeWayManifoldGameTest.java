package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
public final class GasPipeThreeWayManifoldGameTest {
    private static final long HIGH_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final long MAX_FACE_FLOW_QUANTIZATION_ERROR_GU = 2;

    private static final BlockPos WEST_SOURCE = new BlockPos(0, 1, 1);
    private static final BlockPos NORTH_SOURCE = new BlockPos(1, 1, 0);
    private static final BlockPos MANIFOLD = new BlockPos(1, 1, 1);

    private GasPipeThreeWayManifoldGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void threeWayManifoldConservesFlow(GameTestHelper helper) {
        placeSource(helper, WEST_SOURCE);
        placeSource(helper, NORTH_SOURCE);
        helper.setBlock(MANIFOLD, encasedPipeState());

        helper.succeedWhen(() -> {
            GasTransportBehaviour manifold = transport(helper);
            FlowState westFlow = manifold.getFlowState(Direction.WEST);
            FlowState northFlow = manifold.getFlowState(Direction.NORTH);
            FlowState southFlow = manifold.getFlowState(Direction.SOUTH);

            helper.assertTrue(westFlow != null, "West source did not feed the three-way manifold");
            if (westFlow == null) {
                throw new NullPointerException("West source did not feed the three-way manifold.");
            }

            helper.assertTrue(northFlow != null, "North source did not feed the three-way manifold");
            if (northFlow == null) {
                throw new NullPointerException("North source did not feed the three-way manifold.");
            }

            helper.assertTrue(southFlow != null, "Three-way manifold did not discharge toward atmosphere");
            if (southFlow == null) {
                throw new NullPointerException("Three-way manifold did not discharge toward atmosphere.");
            }

            helper.assertTrue(westFlow.direction() == FlowDirection.INBOUND, "West manifold face was not inbound");
            helper.assertTrue(northFlow.direction() == FlowDirection.INBOUND, "North manifold face was not inbound");
            helper.assertTrue(southFlow.direction() == FlowDirection.OUTBOUND, "South manifold face was not outbound");
            helper.assertTrue(westFlow.flowRate() > 0, "West manifold inlet flow was not positive");
            helper.assertTrue(northFlow.flowRate() > 0, "North manifold inlet flow was not positive");
            helper.assertTrue(southFlow.flowRate() > 0, "South manifold outlet flow was not positive");
            helper.assertTrue(westFlow.gas().is(CCBGases.NATURAL_AIR.get()), "West manifold inlet gas was not Natural Air");
            helper.assertTrue(northFlow.gas().is(CCBGases.NATURAL_AIR.get()), "North manifold inlet gas was not Natural Air");
            helper.assertTrue(southFlow.gas().is(CCBGases.NATURAL_AIR.get()), "South manifold outlet gas was not Natural Air");

            long inboundTotal = westFlow.flowRate() + northFlow.flowRate();
            long outboundTotal = southFlow.flowRate();
            long balanceError = Math.abs(inboundTotal - outboundTotal);
            helper.assertTrue(balanceError <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Three-way manifold flow telemetry exceeded whole-GU quantization tolerance: inlets=" + inboundTotal + ", outlet=" + outboundTotal + ", error=" + balanceError + " GU/t");
            helper.assertTrue(Math.abs(westFlow.flowRate() - northFlow.flowRate()) <= 1, "Equal-pressure manifold inlets did not contribute evenly: west=" + westFlow.flowRate() + ", north=" + northFlow.flowRate() + " GU/t");

            long throughput = manifold.getThroughputFlowRate();
            helper.assertTrue(throughput > 0, "Three-way manifold throughput was not positive");
            helper.assertTrue(Math.abs(throughput - inboundTotal) <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Three-way manifold throughput did not match total inlet telemetry: throughput=" + throughput + ", inlets=" + inboundTotal + " GU/t");
            helper.assertTrue(Math.abs(throughput - outboundTotal) <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Three-way manifold throughput did not match outlet telemetry: throughput=" + throughput + ", outlet=" + outboundTotal + " GU/t");
        });
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

    private static BlockState encasedPipeState() {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState();
        for (Direction face : new Direction[]{Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
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
