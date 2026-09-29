package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
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
public final class GasEncasedPipeOpeningIsolationGameTest {
    private static final long SOURCE_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final long CLOSED_BRANCH_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA / 2;

    private static final BlockPos SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos CLOSED_BRANCH_POS = new BlockPos(1, 1, 2);

    private GasEncasedPipeOpeningIsolationGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void encasedPipeOnlyConnectsThroughOpenFaces(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        placeCreativeBoundary(helper, CLOSED_BRANCH_POS, CLOSED_BRANCH_PRESSURE_PA);
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState().setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST), true).setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST), true));

        helper.succeedWhen(() -> {
            BlockState state = level.getBlockState(helper.absolutePos(PIPE_POS));
            helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get()), "Airtight encased pipe was not present at the test position");
            helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)), "Airtight encased pipe west face was not open");
            helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)), "Airtight encased pipe east face was not open");
            helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.SOUTH)), "Airtight encased pipe south face unexpectedly opened toward the low-pressure neighbour");
            helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.NORTH)), "Airtight encased pipe north face unexpectedly opened");
            helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP)), "Airtight encased pipe up face unexpectedly opened");
            helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.DOWN)), "Airtight encased pipe down face unexpectedly opened");

            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(transport != null, "Airtight encased pipe transport behaviour was not initialized");
            if (transport == null) {
                throw new NullPointerException("Airtight encased pipe transport behaviour was not initialized.");
            }

            helper.assertTrue(transport.canConnectOnFace(state, Direction.WEST), "Open west face did not connect to the high-pressure source");
            helper.assertTrue(transport.canConnectOnFace(state, Direction.EAST), "Open east face did not connect toward atmosphere");
            helper.assertTrue(!transport.isConnectionFaceEnabled(state, Direction.SOUTH), "Closed south face was reported as enabled");
            helper.assertTrue(!transport.canConnectOnFace(state, Direction.SOUTH), "Closed south face connected to a valid low-pressure gas endpoint");

            FlowState westFlow = transport.getFlowState(Direction.WEST);
            FlowState eastFlow = transport.getFlowState(Direction.EAST);
            helper.assertTrue(westFlow != null, "High-pressure source did not feed the open west face of the airtight encased pipe");
            if (westFlow == null) {
                throw new NullPointerException("High-pressure source did not feed the open west face of the airtight encased pipe.");
            }

            helper.assertTrue(eastFlow != null, "Airtight encased pipe did not discharge through its open east face");
            if (eastFlow == null) {
                throw new NullPointerException("Airtight encased pipe did not discharge through its open east face.");
            }

            helper.assertTrue(westFlow.direction() == FlowDirection.INBOUND, "Airtight encased pipe west face was not inbound");
            helper.assertTrue(eastFlow.direction() == FlowDirection.OUTBOUND, "Airtight encased pipe east face was not outbound");
            helper.assertTrue(westFlow.flowRate() > 0, "Airtight encased pipe open-face flow was not positive");
            helper.assertValueEqual(eastFlow.flowRate(), westFlow.flowRate(), "airtight encased pipe open-face flow rate");
            helper.assertValueEqual(transport.getThroughputFlowRate(), westFlow.flowRate(), "airtight encased pipe throughput");
            helper.assertTrue(westFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Airtight encased pipe inlet gas was not Natural Air");
            helper.assertTrue(eastFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Airtight encased pipe outlet gas was not Natural Air");

            helper.assertTrue(transport.getFlowState(Direction.SOUTH) == null, "Closed south face leaked gas toward the low-pressure neighbour");
            helper.assertTrue(transport.getFlowState(Direction.NORTH) == null, "Closed north face produced ghost flow");
            helper.assertTrue(transport.getFlowState(Direction.UP) == null, "Closed up face produced ghost flow");
            helper.assertTrue(transport.getFlowState(Direction.DOWN) == null, "Closed down face produced ghost flow");
        });
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

}
