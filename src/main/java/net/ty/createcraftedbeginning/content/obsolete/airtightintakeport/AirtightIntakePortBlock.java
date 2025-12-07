package net.ty.createcraftedbeginning.content.obsolete.airtightintakeport;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.data.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class AirtightIntakePortBlock extends Block implements IBE<AirtightIntakePortBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightIntakePortBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @SuppressWarnings("unused")
    public static boolean isValidDirection(@NotNull Axis pipeAxis, Direction connectionDirection, @NotNull BlockState portState) {
        Direction portFacing = portState.getValue(FACING).getOpposite();
        return switch (pipeAxis) {
            case X -> connectionDirection == Direction.WEST && portFacing == Direction.EAST || connectionDirection == Direction.EAST && portFacing == Direction.WEST;
            case Z -> connectionDirection == Direction.NORTH && portFacing == Direction.SOUTH || connectionDirection == Direction.SOUTH && portFacing == Direction.NORTH;
            case Y -> connectionDirection == Direction.DOWN && portFacing == Direction.UP || connectionDirection == Direction.UP && portFacing == Direction.DOWN;
        };
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        state = ProperWaterloggedBlock.withWater(context.getLevel(), state, context.getClickedPos());
        return state.setValue(FACING, context.getClickedFace());
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        CCBAdvancementBehaviour.setPlacedBy(world, pos, placer);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbourState, @NotNull LevelAccessor world, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return state;
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext collisionContext) {
        return CCBShapes.AIRTIGHT_INTAKE_PORT.get(state.getValue(FACING));
    }

    @Override
    public Class<AirtightIntakePortBlockEntity> getBlockEntityClass() {
        return AirtightIntakePortBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightIntakePortBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_INTAKE_PORT.get();
    }
}
