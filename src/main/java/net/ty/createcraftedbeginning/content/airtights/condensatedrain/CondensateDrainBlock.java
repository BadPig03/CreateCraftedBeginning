package net.ty.createcraftedbeginning.content.airtights.condensatedrain;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.GasControllerData;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class CondensateDrainBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<CondensateDrainBlockEntity>, IWrenchable, SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final MapCodec<CondensateDrainBlock> CODEC = simpleCodec(CondensateDrainBlock::new);

    public CondensateDrainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public static @NotNull Direction getConnectedDirection(@NotNull BlockState state) {
        AttachFace face = state.getValue(FACE);
        if (face == AttachFace.CEILING) {
            return Direction.UP;
        } else if (face == AttachFace.FLOOR) {
            return Direction.DOWN;
        } else {
            return state.getValue(FACING);
        }
    }

    public static @NotNull Direction getFacing(BlockState state) {
        return getConnectedDirection(state);
    }

    public static boolean canAttach(@NotNull LevelReader level, @NotNull BlockPos blockPos, Direction direction) {
        BlockPos blockpos = blockPos.relative(direction);
        return level.getBlockState(blockpos).getBlock() instanceof AirtightTankBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Class<CondensateDrainBlockEntity> getBlockEntityClass() {
        return CondensateDrainBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CondensateDrainBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.CONDENSATE_DRAIN.get();
    }

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return canAttach(level, pos, getConnectedDirection(state));
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        Direction horizontalDirection = context.getHorizontalDirection();

        if (direction == Direction.UP) {
            state = defaultBlockState().setValue(FACE, AttachFace.FLOOR).setValue(FACING, horizontalDirection);
        } else if (direction == Direction.DOWN) {
            state = defaultBlockState().setValue(FACE, AttachFace.CEILING).setValue(FACING, horizontalDirection);
        } else {
            state = defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, direction.getOpposite());
        }

        state = ProperWaterloggedBlock.withWater(level, state, pos);

        if (canSurvive(state, level, pos)) {
            return state;
        }

        return null;
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbourState, @NotNull LevelAccessor world, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return super.updateShape(state, direction, neighbourState, world, pos, neighbourPos);
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        AirtightTankBlock.updateTankState(level, pos.relative(getFacing(state)));
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            AirtightTankBlock.updateTankState(level, pos.relative(getFacing(state)));
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CondensateDrainBlockEntity drain)) {
            super.onRemove(state, level, pos, newState, isMoving);
            AirtightTankBlock.updateTankState(level, pos.relative(getFacing(state)));
            return;
        }

        SmartFluidTankBehaviour tankBehaviour = drain.getTankBehaviour();
        SmartFluidTank tank = tankBehaviour.getPrimaryHandler();

        if (tank.isEmpty()) {
            super.onRemove(state, level, pos, newState, isMoving);
            AirtightTankBlock.updateTankState(level, pos.relative(getFacing(state)));
            return;
        }

        GasControllerData gasController = drain.getGasController();
        if (gasController == null) {
            super.onRemove(state, level, pos, newState, isMoving);
            AirtightTankBlock.updateTankState(level, pos.relative(getFacing(state)));
            return;
        }

        int currentLevel = gasController.getCondensationLevel();
        int penalty = Mth.clamp(currentLevel / 3, 1, 3);
        gasController.setCondensationLevel(Mth.clamp(currentLevel - penalty, 0, GasControllerData.MAX_LEVEL));
        gasController.setCondensateSuccessCount(0);

        super.onRemove(state, level, pos, newState, isMoving);
        AirtightTankBlock.updateTankState(level, pos.relative(getFacing(state)));
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext collisionContext) {
        return CCBShapes.CONDENSATE_DRAIN.get(getFacing(state).getOpposite());
    }
}