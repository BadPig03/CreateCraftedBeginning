package net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve;

import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.IGasExtractor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightCheckValveBlockEntity extends SmartBlockEntity implements IGasExtractor {
    public AirtightCheckValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        CheckValvePipeTransportBehaviour transportBehaviour = new CheckValvePipeTransportBehaviour(this);
        behaviours.add(transportBehaviour);
    }

    @Override
    public boolean canExtract(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return true;
    }

    public class CheckValvePipeTransportBehaviour extends GasTransportBehaviour {
        public CheckValvePipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(@NotNull BlockState state, @NotNull Direction direction) {
            if (state.getValue(AirtightCheckValveBlock.AXIS) != direction.getAxis()) {
                return false;
            }

            BlockPos otherPos = worldPosition.relative(direction);
            BlockState otherState = getWorld().getBlockState(otherPos);
            return isValidAirtightComponents(otherPos, otherState, direction);
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
            if (isIncorrectAxis(state, direction)) {
                return AttachmentTypes.NONE;
            }

            BlockState otherState = level.getBlockState(pos.relative(direction));
            Block otherBlock = otherState.getBlock();
            Axis axis = state.getValue(AirtightCheckValveBlock.AXIS);
            return otherBlock instanceof IAxisPipe axisPipe && axisPipe.getAxis(otherState) == axis ? AttachmentTypes.NONE : AttachmentTypes.RIM.withoutConnector();
        }

        @Override
        public boolean canPullGasFrom(GasStack gas, @NotNull BlockState state, @NotNull Direction direction) {
            boolean isInverted = state.getValue(AirtightCheckValveBlock.INVERTED);
            AxisDirection axisDirection = direction.getAxisDirection();
            return axisDirection == AxisDirection.POSITIVE && !isInverted || axisDirection == AxisDirection.NEGATIVE && isInverted;
        }
    }
}
