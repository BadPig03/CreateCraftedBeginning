package net.ty.createcraftedbeginning.gametests.content.airtights.airtightpipe;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachments;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightPipeAttachmentGameTests {
    private AirtightPipeAttachmentGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void alignedPipeAttachmentsPreserveCasingAndMeterRules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos neighbor = pos.east();
        BlockState pipe = CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.X);
        level.setBlockAndUpdate(pos, pipe);
        level.setBlockAndUpdate(neighbor, pipe);
        assertAttachment(helper, pos, Direction.EAST, AirtightPipeAttachmentTypes.NONE);
        assertAttachment(helper, pos, Direction.NORTH, AirtightPipeAttachmentTypes.NONE);

        level.setBlockAndUpdate(neighbor, pipe.setValue(AirtightPipeBlock.CASED, true));
        assertAttachment(helper, pos, Direction.EAST, AirtightPipeAttachmentTypes.RIM);

        level.setBlockAndUpdate(neighbor, CCBBlocks.AIRTIGHT_FLOWMETER_BLOCK.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.X));
        assertAttachment(helper, pos, Direction.EAST, AirtightPipeAttachmentTypes.RIM);

        level.setBlockAndUpdate(neighbor, CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState());
        assertAttachment(helper, pos, Direction.EAST, AirtightPipeAttachmentTypes.DRAIN);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void regulatorAttachmentsFollowAllOutputDirections(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (Direction output : Iterate.directions) {
            for (Direction face : Iterate.directions) {
                level.setBlockAndUpdate(pos.relative(face), Blocks.AIR.defaultBlockState());
            }

            BlockState pump = CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK.getDefaultState().setValue(BlockStateProperties.FACING, output);
            level.setBlockAndUpdate(pos, pump);
            assertAttachment(helper, pos, output, AirtightPipeAttachmentTypes.OUTLET_RIM);
            assertAttachment(helper, pos, output.getOpposite(), AirtightPipeAttachmentTypes.INLET_RIM);
            for (Direction face : Iterate.directions) {
                if (face.getAxis() == output.getAxis()) {
                    continue;
                }

                assertAttachment(helper, pos, face, AirtightPipeAttachmentTypes.NONE);
            }

            level.setBlockAndUpdate(pos.relative(output), CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState());
            level.setBlockAndUpdate(pos.relative(output.getOpposite()), CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState());
            assertAttachment(helper, pos, output, AirtightPipeAttachmentTypes.OUTLET_DRAIN);
            assertAttachment(helper, pos, output.getOpposite(), AirtightPipeAttachmentTypes.INLET_DRAIN);
        }

        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void encasedAttachmentsRespectConnectionsAndPumpFacing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState pipe = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.getDefaultState().setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST), true);
        level.setBlockAndUpdate(pos, pipe);
        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "Encased pipe transport is missing");
        if (transport == null) {
            throw new NullPointerException("Encased pipe transport is missing.");
        }

        helper.assertTrue(AirtightPipeAttachments.resolve(level, pos, pipe, Direction.EAST, transport) == AirtightPipeAttachmentTypes.RIM, "Open encased outlet lost its rim");
        helper.assertTrue(AirtightPipeAttachments.resolve(level, pos, pipe, Direction.WEST, transport) == AirtightPipeAttachmentTypes.NONE, "Closed encased face gained an attachment");
        helper.assertTrue(AirtightPipeAttachments.resolve(level, pos, pipe, Direction.EAST, null) == AirtightPipeAttachmentTypes.NONE, "Encased fallback unexpectedly gained an attachment");

        BlockState pump = CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.WEST);
        level.setBlockAndUpdate(pos.east(), pump);
        helper.assertTrue(AirtightPipeAttachments.resolve(level, pos, pipe, Direction.EAST, transport) == AirtightPipeAttachmentTypes.NONE, "Pump outlet gained a duplicate rim");
        level.setBlockAndUpdate(pos.east(), pump.setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.assertTrue(AirtightPipeAttachments.resolve(level, pos, pipe, Direction.EAST, transport) == AirtightPipeAttachmentTypes.RIM, "Pump inlet lost its pipe rim");
        helper.succeed();
    }

    private static void assertAttachment(GameTestHelper helper, BlockPos pos, Direction face, AirtightPipeAttachmentTypes expected) {
        ServerLevel level = helper.getLevel();
        BlockState state = level.getBlockState(pos);
        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "Attachment test transport is missing");
        if (transport == null) {
            throw new NullPointerException("Attachment test transport is missing.");
        }

        helper.assertTrue(AirtightPipeAttachments.resolve(level, pos, state, face, transport) == expected, "Live attachment mismatch on " + face + ": expected " + expected);
        helper.assertTrue(AirtightPipeAttachments.resolve(level, pos, state, face, null) == expected, "Fallback attachment mismatch on " + face + ": expected " + expected);
    }
}
