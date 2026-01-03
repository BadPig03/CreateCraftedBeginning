package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasTransportBehaviour;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightPipeBlockEntity extends SmartBlockEntity {
    public AirtightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        GasPipeTransportBehaviour transportBehaviour = new GasPipeTransportBehaviour(this);
        behaviours.add(transportBehaviour);
    }

    public class GasPipeTransportBehaviour extends GasTransportBehaviour {
        public GasPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(@NotNull BlockState state, @NotNull Direction direction) {
            if (state.getValue(AirtightPipeBlock.AXIS) != direction.getAxis()) {
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
            Axis axis = state.getValue(AirtightPipeBlock.AXIS);
            return otherBlock instanceof IAxisPipe axisPipe && axisPipe.getAxis(otherState) == axis ? AttachmentTypes.NONE : AttachmentTypes.RIM.withoutConnector();
        }
    }
}
