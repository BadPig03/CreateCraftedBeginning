package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BoilerSteamOutletBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<BoilerSteamOutletBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public BoilerSteamOutletBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(OPEN, true).setValue(POWERED, false));
    }

    public static Direction getFacing(BlockState state) {
        return getConnectedDirection(state);
    }

    public static BlockPos getAttachedTankPos(BlockState state, BlockPos pos) {
        return pos.relative(getFacing(state).getOpposite());
    }

    public static boolean isActive(BlockState state) {
        return state.getBlock() instanceof BoilerSteamOutletBlock && state.getValue(OPEN) && !state.getValue(POWERED);
    }

    static void refreshBoiler(BlockState state, Level level, BlockPos pos) {
        FluidTankBlock.updateBoilerState(state, level, getAttachedTankPos(state, pos));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, WATERLOGGED, OPEN, POWERED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return simpleCodec(BoilerSteamOutletBlock::new);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(getAttachedTankPos(state, pos)).getBlock() instanceof FluidTankBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Direction horizontalFacing = context.getHorizontalDirection();
        BlockState state = switch (clickedFace) {
            case UP -> defaultBlockState().setValue(FACE, AttachFace.FLOOR).setValue(FACING, horizontalFacing);
            case DOWN -> defaultBlockState().setValue(FACE, AttachFace.CEILING).setValue(FACING, horizontalFacing);
            default -> defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, clickedFace);
        };
        return ProperWaterloggedBlock.withWater(level, state, pos).setValue(POWERED, level.hasNeighborSignal(pos));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighbourPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighbourPos, isMoving);
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }

        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) {
            return;
        }

        BlockState updatedState = state.setValue(POWERED, powered);
        level.setBlock(pos, updatedState, UPDATE_ALL);
        if (level.isClientSide) {
            return;
        }

        FluidTankBlock.updateBoilerState(updatedState, level, getAttachedTankPos(updatedState, pos));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        refreshBoiler(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            refreshBoiler(state, level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState updatedState = state.setValue(OPEN, !state.getValue(OPEN));
        level.setBlock(pos, updatedState, UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, 0.5f);
        refreshBoiler(updatedState, level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.BOILER_STEAM_OUTLET.get(getFacing(state));
    }

    @Override
    public Class<BoilerSteamOutletBlockEntity> getBlockEntityClass() {
        return BoilerSteamOutletBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BoilerSteamOutletBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.BOILER_STEAM_OUTLET.get();
    }
}