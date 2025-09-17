package net.ty.createcraftedbeginning.content.airtights.checkvalve;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.api.gas.AxisGasPipeBlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CheckValveBlockEntity extends AxisGasPipeBlockEntity {
    public CheckValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new CheckValvePipeTransportBehaviour(this));
    }

    public class CheckValvePipeTransportBehaviour extends GasTransportBehaviour {
        public CheckValvePipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canPullGasFrom(GasStack gas, @NotNull BlockState state, @NotNull Direction direction) {
            boolean isInverted = state.getValue(CheckValveBlock.INVERTED);
            Direction.AxisDirection axisDirection = direction.getAxisDirection();
            return (axisDirection == Direction.AxisDirection.POSITIVE && !isInverted) || (axisDirection == Direction.AxisDirection.NEGATIVE && isInverted);
        }

        @Override
        public boolean canHaveFlowToward(@NotNull BlockState state, Direction direction) {
            return state.hasProperty(CheckValveBlock.AXIS) && state.getValue(CheckValveBlock.AXIS) == direction.getAxis();
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            if (!canHaveFlowToward(state, direction)) {
                return AttachmentTypes.NONE;
            }

            BlockState otherState = getWorld().getBlockState(getPos().relative(direction));
            Block otherBlock = otherState.getBlock();
            Direction.Axis axis = state.getValue(CheckValveBlock.AXIS);

            return switch (otherBlock) {
                case AirtightPipeBlock ignored when otherState.getValue(AirtightPipeBlock.AXIS) == axis -> AttachmentTypes.NONE;
                case CheckValveBlock ignored when otherState.getValue(CheckValveBlock.AXIS) == axis -> AttachmentTypes.NONE;
                case SmartAirtightPipeBlock ignored when otherState.getValue(SmartAirtightPipeBlock.AXIS) == axis -> AttachmentTypes.NONE;
                default -> AttachmentTypes.RIM.withoutConnector();
            };
        }
    }
}
