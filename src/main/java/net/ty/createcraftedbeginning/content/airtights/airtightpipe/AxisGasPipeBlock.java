package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.data.CCBShapes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AxisGasPipeBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock, IWrenchable, IAxisPipe {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AxisGasPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public static boolean isOpenAt(BlockState state, Direction direction) {
        return direction.getAxis() == state.getValue(AXIS);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.FAIL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState state, @Nullable LivingEntity entity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, state, entity, itemStack);
        CCBAdvancementBehaviour.setPlacedBy(level, blockPos, entity);
    }

    @Override
    public Axis getAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = super.getStateForPlacement(context);
        Direction facing = context.getNearestLookingDirection();
        Axis preferredAxis = facing.getAxis();
        boolean isSneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if (isSneaking) {
            state = state.setValue(AXIS, preferredAxis);
            return ProperWaterloggedBlock.withWater(level, state, pos);
        }

        Set<Axis> availableAxes = new HashSet<>();
        for (Direction side : Iterate.directions) {
            BlockPos otherPos = pos.relative(side);
            BlockEntity otherBE = level.getBlockEntity(otherPos);
            if (otherBE == null) {
                continue;
            }

            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, otherPos, GasTransportBehaviour.TYPE);
            IGasHandler capability = level.getCapability(GasHandler.BLOCK, otherPos, side);
            if (transport == null && capability == null) {
                continue;
            }

            availableAxes.add(side.getAxis());
        }

        if (availableAxes.isEmpty()) {
            state = state.setValue(AXIS, preferredAxis);
        }
        else if (!availableAxes.contains(preferredAxis)) {
            Axis finalAxis = preferredAxis;
            if (availableAxes.contains(Axis.X) && preferredAxis != Axis.X) {
                finalAxis = Axis.X;
            }
            else if (availableAxes.contains(Axis.Z) && preferredAxis != Axis.Z) {
                finalAxis = Axis.Z;
            }
            else if (availableAxes.contains(Axis.Y)) {
                finalAxis = Axis.Y;
            }

            state = state.setValue(AXIS, finalAxis);
        }
        else {
            state = state.setValue(AXIS, preferredAxis);
        }

        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        Direction direction = GasPropagator.getChangedNeighbourSide(level, pos, neighborPos);
        if (direction == null || !isOpenAt(state, direction)) {
            return;
        }

        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || state == oldState) {
            return;
        }

        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        boolean changed = !state.is(newState.getBlock());
        if (changed && !level.isClientSide) {
            GasPropagator.propagatePipe(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_PIPE.get(state.getValue(AXIS));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GasPropagator.propagatePipe(level, pos, state);
    }
}
