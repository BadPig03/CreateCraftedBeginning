package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirCompressorPlacement {
    private AirCompressorPlacement() {
    }

    static BlockState getStateForPlacement(BlockPlaceContext context, BlockState state) {
        Direction defaultFacing = context.getHorizontalDirection().getOpposite();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return state.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction neighborDirection = context.getClickedFace().getOpposite();
        BlockState neighborState = level.getBlockState(pos.relative(neighborDirection));
        return switch (neighborState.getBlock()) {
            case AirtightPumpBlock ignored -> getStateForPumpPlacement(state, neighborState, defaultFacing);
            case AirtightPipeBlock ignored -> getStateForPipePlacement(state, neighborState, neighborDirection, defaultFacing);
            case SmartAirtightPipeBlock ignored -> getStateForPipePlacement(state, neighborState, neighborDirection, defaultFacing);
            case AirtightCheckValveBlock ignored -> getStateForCheckValvePlacement(state, neighborState, defaultFacing);
            default -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
        };
    }

    private static BlockState getStateForPumpPlacement(BlockState state, BlockState pumpState, Direction defaultFacing) {
        Direction facing = pumpState.getValue(AirtightPumpBlock.FACING);
        if (facing.getAxis() == Axis.Y) {
            return state.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
        }
        return state.setValue(AirCompressorBlock.HORIZONTAL_FACING, facing.getClockWise());
    }

    private static BlockState getStateForPipePlacement(BlockState state, BlockState pipeState, Direction neighborDirection, Direction defaultFacing) {
        boolean isNegativeDirection = neighborDirection.getAxisDirection() == AxisDirection.NEGATIVE;
        return switch (pipeState.getValue(AirCompressorBlock.AXIS)) {
            case X -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, isNegativeDirection ? Direction.SOUTH : Direction.NORTH);
            case Y -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
            case Z -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, isNegativeDirection ? Direction.EAST : Direction.WEST);
        };
    }

    private static BlockState getStateForCheckValvePlacement(BlockState state, BlockState valveState, Direction defaultFacing) {
        boolean inverted = valveState.getValue(AirtightCheckValveBlock.INVERTED);
        return switch (valveState.getValue(AirtightCheckValveBlock.AXIS)) {
            case X -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, inverted ? Direction.NORTH : Direction.SOUTH);
            case Y -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
            case Z -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, inverted ? Direction.WEST : Direction.EAST);
        };
    }
}
