package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeBlock extends AxisGasPipeBlock implements IBE<SmartAirtightPipeBlockEntity>, IDirectionalPipe, IAirtightComponent {
    public SmartAirtightPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DIRECTIONAL_FACING, DirectionalFacing.NULL));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(DIRECTIONAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction horizontalFacing = context.getHorizontalDirection();
        return super.getStateForPlacement(context).setValue(DIRECTIONAL_FACING, DirectionalFacing.getFacingDirection(horizontalFacing));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        Axis axis = state.getValue(AXIS);
        if (axis != Axis.Y) {
            return CCBShapes.SMART_AIRTIGHT_PIPE.get(axis);
        }

        DirectionalFacing facing = state.getValue(DIRECTIONAL_FACING);
        return CCBShapes.SMART_AIRTIGHT_PIPE_VERTICAL.get(DirectionalFacing.getDirection(facing).getOpposite());
    }

    @Override
    public Class<SmartAirtightPipeBlockEntity> getBlockEntityClass() {
        return SmartAirtightPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmartAirtightPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.SMART_AIRTIGHT_PIPE.get();
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return currentState.getValue(AXIS) == localFace.getAxis();
    }
}
