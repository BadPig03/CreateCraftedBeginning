package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirCompressorPlacement {
    private AirCompressorPlacement() {
    }

    public static BlockState getStateForPlacement(BlockPlaceContext context, BlockState compressorState) {
        Direction defaultFacing = context.getHorizontalDirection().getOpposite();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
        }

        Direction neighborDirection = context.getClickedFace().getOpposite();
        BlockState neighborState = context.getLevel().getBlockState(context.getClickedPos().relative(neighborDirection));
        return switch (neighborState.getBlock()) {
            case AirtightPumpBlock ignored -> getStateForPumpPlacement(compressorState, neighborState, defaultFacing);
            case AirtightPipeBlock ignored -> getStateForPipePlacement(compressorState, neighborState, neighborDirection, defaultFacing);
            case SmartAirtightPipeBlock ignored -> getStateForPipePlacement(compressorState, neighborState, neighborDirection, defaultFacing);
            case AirtightCheckValveBlock ignored -> getStateForCheckValvePlacement(compressorState, neighborState, defaultFacing);
            default -> compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
        };
    }

    private static BlockState getStateForPumpPlacement(BlockState compressorState, BlockState pumpState, Direction defaultFacing) {
        Direction pumpFacing = pumpState.getValue(AirtightPumpBlock.FACING);
        if (pumpFacing.getAxis() == Axis.Y) {
            return compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
        }
        return compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, pumpFacing.getClockWise());
    }

    private static BlockState getStateForPipePlacement(BlockState compressorState, BlockState pipeState, Direction neighborDirection, Direction defaultFacing) {
        boolean isNegativeNeighborDirection = neighborDirection.getAxisDirection() == AxisDirection.NEGATIVE;
        return switch (pipeState.getValue(BlockStateProperties.AXIS)) {
            case X -> compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, isNegativeNeighborDirection ? Direction.SOUTH : Direction.NORTH);
            case Y -> compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
            case Z -> compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, isNegativeNeighborDirection ? Direction.EAST : Direction.WEST);
        };
    }

    private static BlockState getStateForCheckValvePlacement(BlockState compressorState, BlockState valveState, Direction defaultFacing) {
        boolean isInverted = valveState.getValue(AirtightCheckValveBlock.INVERTED);
        return switch (valveState.getValue(AirtightCheckValveBlock.AXIS)) {
            case X -> compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, isInverted ? Direction.NORTH : Direction.SOUTH);
            case Y -> compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, defaultFacing);
            case Z -> compressorState.setValue(AirCompressorBlock.HORIZONTAL_FACING, isInverted ? Direction.WEST : Direction.EAST);
        };
    }
}
