package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AxisGasTransportBehaviour extends GasTransportBehaviour {
    protected AxisGasTransportBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public boolean canHaveFlowToward(BlockState state, Direction direction) {
        if (!canHaveFlowTowardWithoutLevel(state, direction)) {
            return false;
        }

        Level level = getWorld();
        if (level == null) {
            return false;
        }

        BlockPos adjacentPos = blockEntity.getBlockPos().relative(direction);
        return level.isLoaded(adjacentPos) && isValidAirtightComponents(level, adjacentPos, level.getBlockState(adjacentPos), direction);
    }

    @Override
    public boolean canHaveFlowTowardWithoutLevel(BlockState state, Direction direction) {
        return state.getValue(BlockStateProperties.AXIS) == direction.getAxis();
    }

    @Override
    public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (isIncorrectAxis(state, direction)) {
            return AttachmentTypes.NONE;
        }

        BlockPos adjacentPos = pos.relative(direction);
        BlockState adjacentState = level.getBlockState(adjacentPos);
        Block adjacentBlock = adjacentState.getBlock();
        Axis pipeAxis = state.getValue(BlockStateProperties.AXIS);
        if (adjacentBlock instanceof IAxisPipe axisPipe && axisPipe.getAxis(adjacentState) == pipeAxis) {
            return AttachmentTypes.NONE;
        }

        if (adjacentBlock instanceof IAirtightPipeDrain drain && drain.shouldRenderDrain(level, adjacentPos, adjacentState, direction.getOpposite())) {
            return AttachmentTypes.DRAIN;
        }
        return AttachmentTypes.RIM;
    }
}
