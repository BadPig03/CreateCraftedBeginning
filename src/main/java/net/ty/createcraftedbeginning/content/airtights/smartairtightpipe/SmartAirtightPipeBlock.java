package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.api.gas.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.api.gas.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class SmartAirtightPipeBlock extends AxisGasPipeBlock implements IBE<SmartAirtightPipeBlockEntity>, IDirectionalPipe {
    public SmartAirtightPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DIRECTIONAL_FACING, DirectionalFacing.NULL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(DIRECTIONAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @NotNull BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction horizontalFacing = context.getHorizontalDirection();
        if (state.hasProperty(IDirectionalPipe.DIRECTIONAL_FACING)) {
            state = state.setValue(IDirectionalPipe.DIRECTIONAL_FACING, IDirectionalPipe.DirectionalFacing.getFacingDirection(horizontalFacing));
        }
        return state;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        Direction.Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
        if (axis == Direction.Axis.Y) {
            IDirectionalPipe.DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
            return CCBShapes.SMART_AIRTIGHT_PIPE_VERTICAL.get(DirectionalFacing.getDirection(facing).getOpposite());
        }
        return CCBShapes.SMART_AIRTIGHT_PIPE.get(state.getValue(AXIS));
    }

    @Override
    public Class<SmartAirtightPipeBlockEntity> getBlockEntityClass() {
        return SmartAirtightPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmartAirtightPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.SMART_AIRTIGHT_PIPE.get();
    }
}
