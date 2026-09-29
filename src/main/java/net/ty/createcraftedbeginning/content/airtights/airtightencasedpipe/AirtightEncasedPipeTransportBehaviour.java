package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightEncasedPipeTransportBehaviour extends GasTransportBehaviour {
    AirtightEncasedPipeTransportBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public boolean canConnectOnFace(BlockState state, Direction direction) {
        if (!isConnectionFaceEnabled(state, direction)) {
            return false;
        }

        Level level = getWorld();
        if (level == null) {
            return false;
        }

        BlockPos adjacentPos = blockEntity.getBlockPos().relative(direction);
        if (!level.isLoaded(adjacentPos)) {
            return false;
        }

        BlockState adjacentState = level.getBlockState(adjacentPos);
        return isValidConnectionTarget(level, adjacentPos, adjacentState, direction);
    }

    @Override
    public boolean isConnectionFaceEnabled(BlockState state, Direction direction) {
        return state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
    }
}
