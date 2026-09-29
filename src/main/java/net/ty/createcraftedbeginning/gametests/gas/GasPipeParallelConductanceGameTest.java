package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
public final class GasPipeParallelConductanceGameTest {
    private static final long HIGH_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final long MAX_FACE_FLOW_QUANTIZATION_ERROR_GU = 2;

    private static final BlockPos SINGLE_SOURCE = new BlockPos(1, 1, 1);
    private static final BlockPos SINGLE_JUNCTION = new BlockPos(2, 1, 1);
    private static final BlockPos SINGLE_BRANCH = new BlockPos(3, 1, 1);

    private static final BlockPos PARALLEL_SOURCE = new BlockPos(1, 1, 4);
    private static final BlockPos PARALLEL_JUNCTION = new BlockPos(2, 1, 4);
    private static final BlockPos PARALLEL_NORTH_BRANCH = new BlockPos(2, 1, 3);
    private static final BlockPos PARALLEL_SOUTH_BRANCH = new BlockPos(2, 1, 5);

    private GasPipeParallelConductanceGameTest() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 120)
    public static void parallelPipeBranchesIncreaseTotalFlow(GameTestHelper helper) {
        placeSingleBranchNetwork(helper);
        placeParallelBranchNetwork(helper);

        helper.succeedWhen(() -> {
            GasTransportBehaviour singleJunction = transport(helper, SINGLE_JUNCTION);
            GasTransportBehaviour parallelJunction = transport(helper, PARALLEL_JUNCTION);
            long singleFlow = singleJunction.getThroughputFlowRate();
            long parallelFlow = parallelJunction.getThroughputFlowRate();
            helper.assertTrue(singleFlow > 0, "Expected the single branch to carry gas toward atmosphere");
            helper.assertTrue(parallelFlow > singleFlow, "Expected two parallel branches to carry more total flow than one branch, got " + parallelFlow + " <= " + singleFlow + " GU/t");

            FlowState northFlow = parallelJunction.getFlowState(Direction.NORTH);
            FlowState southFlow = parallelJunction.getFlowState(Direction.SOUTH);
            helper.assertTrue(northFlow != null, "North parallel branch did not receive flow");
            if (northFlow == null) {
                throw new NullPointerException("North parallel branch did not receive flow.");
            }

            helper.assertTrue(southFlow != null, "South parallel branch did not receive flow");
            if (southFlow == null) {
                throw new NullPointerException("South parallel branch did not receive flow.");
            }

            helper.assertTrue(northFlow.direction() == FlowDirection.OUTBOUND, "North parallel branch was not flowing out of the junction");
            helper.assertTrue(southFlow.direction() == FlowDirection.OUTBOUND, "South parallel branch was not flowing out of the junction");
            helper.assertTrue(northFlow.flowRate() > 0, "North parallel branch flow was not positive");
            helper.assertTrue(southFlow.flowRate() > 0, "South parallel branch flow was not positive");
            helper.assertTrue(northFlow.gas().is(CCBGases.NATURAL_AIR.get()), "North parallel branch gas was not Natural Air");
            helper.assertTrue(southFlow.gas().is(CCBGases.NATURAL_AIR.get()), "South parallel branch gas was not Natural Air");

            long branchTotal = northFlow.flowRate() + southFlow.flowRate();
            long balanceError = Math.abs(parallelFlow - branchTotal);
            helper.assertTrue(balanceError <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Parallel junction flow telemetry exceeded whole-GU quantization tolerance: junction=" + parallelFlow + ", outlets=" + branchTotal + ", error=" + balanceError + " GU/t");
            helper.assertTrue(Math.abs(northFlow.flowRate() - southFlow.flowRate()) <= 1, "Symmetric parallel branches did not split flow evenly: north=" + northFlow.flowRate() + ", south=" + southFlow.flowRate() + " GU/t");
        });
    }

    private static void placeSingleBranchNetwork(GameTestHelper helper) {
        placeSource(helper, SINGLE_SOURCE);
        helper.setBlock(SINGLE_JUNCTION, encasedPipeState(Direction.WEST, Direction.EAST));
        helper.setBlock(SINGLE_BRANCH, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
    }

    private static void placeParallelBranchNetwork(GameTestHelper helper) {
        placeSource(helper, PARALLEL_SOURCE);
        helper.setBlock(PARALLEL_JUNCTION, encasedPipeState(Direction.WEST, Direction.NORTH, Direction.SOUTH));
        helper.setBlock(PARALLEL_NORTH_BRANCH, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.Z));
        helper.setBlock(PARALLEL_SOUTH_BRANCH, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.Z));
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

    private static BlockState encasedPipeState(Direction... openFaces) {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState();
        for (Direction face : openFaces) {
            state = state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(face), true);
        }
        return state;
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
