package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineBlock extends KineticBlock implements IBE<AirtightEngineBlockEntity>, IWrenchable, SimpleWaterloggedBlock, ICogWheel {
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty CLOCKWISE = BooleanProperty.create("clockwise");

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH).setValue(AXIS, Axis.Y).setValue(CLOCKWISE, true));
    }

    public static Direction getFacing(BlockState state) {
        return switch (state.getValue(FACE)) {
            case CEILING -> Direction.UP;
            case FLOOR -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    public static boolean isStateValid(BlockState state) {
        AttachFace face = state.getValue(FACE);
        Axis axis = state.getValue(AXIS);
        if (face == AttachFace.WALL) {
            Direction facing = state.getValue(FACING);
            return facing.getAxis() == axis;
        }
        else {
            return axis.isVertical();
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        level.setBlockAndUpdate(pos, state.setValue(CLOCKWISE, !state.getValue(CLOCKWISE)));
        withBlockEntityDo(level, pos, AirtightEngineBlockEntity::updateRotation);
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, AXIS, CLOCKWISE, WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Class<AirtightEngineBlockEntity> getBlockEntityClass() {
        return AirtightEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightEngineBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_ENGINE.get();
    }

    @Override
    protected MapCodec<? extends KineticBlock> codec() {
        return simpleCodec(AirtightEngineBlock::new);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        Direction horizontalDirection = context.getHorizontalDirection();
        BlockState state;
        if (direction == Direction.UP) {
            state = defaultBlockState().setValue(FACE, AttachFace.FLOOR).setValue(FACING, horizontalDirection).setValue(AXIS, Axis.Y);
        }
        else if (direction == Direction.DOWN) {
            state = defaultBlockState().setValue(FACE, AttachFace.CEILING).setValue(FACING, horizontalDirection).setValue(AXIS, Axis.Y);
        }
        else {
            state = defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, direction.getOpposite()).setValue(AXIS, direction.getAxis());
        }
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = getFacing(state);
        return level.getBlockState(pos.relative(facing)).getBlock() instanceof AirtightTankBlock && CogWheelBlock.isValidCogwheelPosition(true, level, pos, facing.getAxis()) && isStateValid(state);
    }

    @Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        if (canSurvive(state, level, pos)) {
            return;
        }

        level.destroyBlock(pos, true);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos blockPos, CollisionContext collisionContext) {
        return CCBShapes.AIRTIGHT_ENGINE.get(getFacing(state).getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        AirtightTankBlock.updateTankState(level, pos.relative(getFacing(state)));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        AirtightTankBlock.updateTankState(level, pos.relative(getFacing(state)));
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    public float getParticleTargetRadius() {
        return 1.125f;
    }

    @Override
    public float getParticleInitialRadius() {
        return 1.0f;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return getFacing(state).getAxis();
    }

    @Override
    public boolean isLargeCog() {
        return true;
    }

    @Override
    public boolean isSmallCog() {
		return false;
	}
}
