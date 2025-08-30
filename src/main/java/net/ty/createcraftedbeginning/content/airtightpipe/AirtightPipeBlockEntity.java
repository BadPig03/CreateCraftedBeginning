package net.ty.createcraftedbeginning.content.airtightpipe;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlock;
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
        public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
            if (!fluid.is(CCBTags.commonFluidTag("compressed_air"))) {
                return false;
            }

            BlockState otherState = getWorld().getBlockState(getPos().relative(direction));
            Block otherBlock = otherState.getBlock();
            Direction.Axis axis = state.getValue(AirtightPipeBlock.AXIS);

            return switch (otherBlock) {
                case AirtightPipeBlock ignored when otherState.getValue(AirtightPipeBlock.AXIS) == axis -> true;
                case AirtightPumpBlock ignored when otherState.getValue(AirtightPumpBlock.FACING).getAxis() == axis -> true;
                case AirCompressorBlock ignored when otherState.getValue(AirCompressorBlock.HORIZONTAL_FACING).getClockWise().getAxis() == axis -> true;
                case GasInjectionChamberBlock ignored -> true;
                case AirtightTankBlock ignored -> true;
                case AirtightEncasedPipeBlock ignored -> true;
                case FluidTankBlock ignored -> true;
                default -> false;
            };
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return state.hasProperty(AirtightPipeBlock.AXIS) && state.getValue(AirtightPipeBlock.AXIS) == direction.getAxis();
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            if (!canHaveFlowToward(state, direction)) {
                return AttachmentTypes.NONE;
            }

            BlockState otherState = getWorld().getBlockState(getPos().relative(direction));
            Block otherBlock = otherState.getBlock();
            Direction.Axis axis = state.getValue(AirtightPipeBlock.AXIS);

            if (otherBlock instanceof AirtightPipeBlock && otherState.getValue(AirtightPipeBlock.AXIS) == axis) {
                return AttachmentTypes.NONE;
            }

            return AttachmentTypes.RIM.withoutConnector();
        }
    }
}
