package net.ty.createcraftedbeginning.content.airtightpipe;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.content.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.data.CCBTags;

import java.util.List;

public class AirtightPipeBlockEntity extends StraightPipeBlockEntity {
    public AirtightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.removeIf(b -> b instanceof FluidTransportBehaviour);
        behaviours.add(new CompressAirPipeTransportBehaviour(this));
    }

    public static class CompressAirPipeTransportBehaviour extends FluidTransportBehaviour {
        public CompressAirPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return state.hasProperty(AxisPipeBlock.AXIS) && state.getValue(AxisPipeBlock.AXIS) == direction.getAxis();
        }

        @Override
        public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
            if (!fluid.is(CCBTags.commonFluidTag("compressed_air"))) {
                return false;
            }

            Direction.Axis axis = IAxisPipe.getAxisOf(state);
            if (axis == null) {
                return true;
            }

            BlockState otherState = getWorld().getBlockState(getPos().relative(direction));
            if (otherState.getBlock() instanceof AirtightIntakePortBlock) {
                return isProperIntakePort(axis, direction, otherState);
            }

            return true;
        }

        private boolean isProperIntakePort(Direction.Axis pipeAxis, Direction connectionDirection, BlockState portState) {
            Direction portFacing = portState.getValue(AirtightIntakePortBlock.FACING).getOpposite();
            return switch (pipeAxis) {
                case X -> (connectionDirection == Direction.WEST && portFacing == Direction.EAST) || (connectionDirection == Direction.EAST && portFacing == Direction.WEST);
                case Z -> (connectionDirection == Direction.NORTH && portFacing == Direction.SOUTH) || (connectionDirection == Direction.SOUTH && portFacing == Direction.NORTH);
                case Y -> (connectionDirection == Direction.DOWN && portFacing == Direction.UP) || (connectionDirection == Direction.UP && portFacing == Direction.DOWN);
            };
        }

        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
            BlockState otherState = world.getBlockState(pos.relative(direction));

            Direction.Axis axis = IAxisPipe.getAxisOf(state);
            Direction.Axis otherAxis = IAxisPipe.getAxisOf(otherState);

            Block otherBlock = otherState.getBlock();

            if (otherBlock instanceof AirtightIntakePortBlock) {
                if (axis != direction.getAxis()) {
                    return AttachmentTypes.NONE;
                }
                return isProperIntakePort(axis, direction, otherState) ? attachment.withoutConnector() : AttachmentTypes.PARTIAL_RIM;
            }

            return switch (otherBlock) {
                case AirtightPipeBlock ignored when attachment == AttachmentTypes.RIM && axis == otherAxis && axis != null -> AttachmentTypes.NONE;
                case PumpBlock ignored when attachment == AttachmentTypes.RIM -> AttachmentTypes.NONE;
                case FluidValveBlock ignored when attachment == AttachmentTypes.RIM && FluidValveBlock.getPipeAxis(otherState) == direction.getAxis() -> AttachmentTypes.NONE;
                case SmartFluidPipeBlock ignored when attachment == AttachmentTypes.RIM && axis == otherAxis && axis != null -> AttachmentTypes.NONE;
                default -> attachment.withoutConnector();
            };
        }
    }
}
