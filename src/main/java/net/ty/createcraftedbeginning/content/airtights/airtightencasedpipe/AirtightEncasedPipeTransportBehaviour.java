package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightEncasedPipeTransportBehaviour extends GasTransportBehaviour {
    AirtightEncasedPipeTransportBehaviour(SmartBlockEntity blockEntity) {
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

        BlockPos otherPos = blockEntity.getBlockPos().relative(direction);
        BlockState otherState = level.getBlockState(otherPos);
        return isValidAirtightComponents(level, otherPos, otherState, direction);
    }

    @Override
    public boolean canHaveFlowTowardWithoutLevel(BlockState state, Direction direction) {
        return state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
    }
}
