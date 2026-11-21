package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AxisGasTransportBehaviour extends GasTransportBehaviour {
    protected AxisGasTransportBehaviour(SmartBlockEntity blockEntity) {
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
        return level.isLoaded(adjacentPos) && isValidConnectionTarget(level, adjacentPos, level.getBlockState(adjacentPos), direction);
    }

    @Override
    public boolean isConnectionFaceEnabled(BlockState state, Direction direction) {
        return state.getValue(BlockStateProperties.AXIS) == direction.getAxis();
    }
}
