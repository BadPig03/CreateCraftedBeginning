package net.ty.createcraftedbeginning.content.breezechamber;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class EmptyBreezeChamberBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final MapCodec<EmptyBreezeChamberBlock> CODEC = simpleCodec(EmptyBreezeChamberBlock::new);

    public EmptyBreezeChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CCBShapes.CHAMBER_BLOCK_SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        if (context == CollisionContext.empty()) {
            return CCBShapes.CHAMBER_BLOCK_SPECIAL_COLLISION_SHAPE;
        }
        return getShape(blockState, level, blockPos, context);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState toPlace = super.getStateForPlacement(context);
        toPlace = ProperWaterloggedBlock.withWater(context.getLevel(), toPlace, context.getClickedPos());

        if (toPlace != null) {
            return toPlace.setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED, FACING);
    }
}
