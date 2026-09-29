package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPipeSeriesResistanceGameTest {
    private static final BlockPos ONE_PIPE = new BlockPos(5, 1, 1);
    private static final BlockPos TWO_PIPE = new BlockPos(5, 1, 3);
    private static final BlockPos FOUR_PIPE = new BlockPos(5, 1, 5);

    private GasPipeSeriesResistanceGameTest() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 120)
    public static void longerSeriesPipeRunsReduceFlow(GameTestHelper helper) {
        placeRun(helper, 1, 1);
        placeRun(helper, 3, 2);
        placeRun(helper, 5, 4);

        helper.succeedWhen(() -> {
            long onePipeFlow = throughput(helper, ONE_PIPE);
            long twoPipeFlow = throughput(helper, TWO_PIPE);
            long fourPipeFlow = throughput(helper, FOUR_PIPE);

            helper.assertTrue(onePipeFlow > 0, "Expected the one-pipe run to draw gas from the atmosphere");
            helper.assertTrue(twoPipeFlow > 0, "Expected the two-pipe run to draw gas from the atmosphere");
            helper.assertTrue(fourPipeFlow > 0, "Expected the four-pipe run to draw gas from the atmosphere");
            helper.assertTrue(onePipeFlow > twoPipeFlow, "Expected one pipe to flow faster than two pipes, got " + onePipeFlow + " <= " + twoPipeFlow + " GU/t");
            helper.assertTrue(twoPipeFlow > fourPipeFlow, "Expected two pipes to flow faster than four pipes, got " + twoPipeFlow + " <= " + fourPipeFlow + " GU/t");
        });
    }

    private static void placeRun(GameTestHelper helper, int z, int pipeCount) {
        helper.setBlock(new BlockPos(6, 1, z), CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());

        for (int offset = 0; offset < pipeCount; offset++) {
            BlockPos pipePos = new BlockPos(5 - offset, 1, z);
            helper.setBlock(pipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        }
    }

    private static long throughput(GameTestHelper helper, BlockPos pipePos) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(pipePos), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "Airtight pipe transport behaviour was not initialized at " + pipePos);
        if (transport == null) {
            throw new NullPointerException("Airtight pipe transport behaviour was not initialized at " + pipePos + '.');
        }

        return transport.getThroughputFlowRate();
    }
}
