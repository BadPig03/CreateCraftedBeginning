package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.equipment.wrench.IWrenchableWithBracket;
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
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AxisGasPipeBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock, IWrenchableWithBracket, IAxisPipe {
    static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected AxisGasPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public static boolean isOpenAt(BlockState state, Direction direction) {
        return direction.getAxis() == state.getValue(AXIS);
    }

    private static Set<Axis> getAvailableAxes(Level level, BlockPos pos) {
        Set<Axis> availableAxes = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            Direction oppositeDirection = direction.getOpposite();
            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, adjacentPos, GasTransportBehaviour.TYPE);
            boolean hasTransportConnection = transport != null && transport.canHaveFlowToward(adjacentState, oppositeDirection);
            if (!hasTransportConnection && !GasCapabilities.hasGasCapability(level, adjacentPos, oppositeDirection)) {
                continue;
            }

            availableAxes.add(direction.getAxis());
        }
        return availableAxes;
    }

    private static void markConnectionsDirty(Level level, BlockPos pos) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        if (transport == null) {
            return;
        }

        transport.markConnectionsDirty();
    }

    private static Axis getPlacementAxis(Set<Axis> availableAxes, Axis preferredAxis) {
        if (availableAxes.isEmpty() || availableAxes.contains(preferredAxis)) {
            return preferredAxis;
        }

        if (availableAxes.contains(Axis.X)) {
            return Axis.X;
        }

        if (availableAxes.contains(Axis.Z)) {
            return Axis.Z;
        }
        return Axis.Y;
    }

    @Override
    public Optional<ItemStack> removeBracket(BlockGetter level, BlockPos pos, boolean inOnReplacedContext) {
        BracketedBlockEntityBehaviour bracketBehaviour = BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
        if (bracketBehaviour == null) {
            return Optional.empty();
        }

        BlockState bracketState = bracketBehaviour.removeBracket(inOnReplacedContext);
        if (bracketState == null) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(bracketState.getBlock()));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (tryRemoveBracket(context)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, placer);
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
        Axis preferredAxis = context.getNearestLookingDirection().getAxis();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            state = state.setValue(AXIS, preferredAxis);
            return ProperWaterloggedBlock.withWater(level, state, pos);
        }

        Set<Axis> availableAxes = getAvailableAxes(level, pos);
        state = state.setValue(AXIS, getPlacementAxis(availableAxes, preferredAxis));
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!state.getValue(WATERLOGGED)) {
            return state;
        }

        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        Direction changedSide = GasPropagator.getChangedNeighbourSide(level, pos, neighborPos);
        if (changedSide == null || !isOpenAt(state, changedSide)) {
            return;
        }

        markConnectionsDirty(level, pos);
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || state == oldState) {
            return;
        }

        markConnectionsDirty(level, pos);
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        boolean blockTypeChanged = !state.is(newState.getBlock());
        if (blockTypeChanged && !level.isClientSide) {
            GasPropagator.propagatePipe(level, pos);
        }
        if (state != newState && !isMoving) {
            removeBracket(level, pos, true).ifPresent(bracketStack -> popResource(level, pos, bracketStack));
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_PIPE.get(state.getValue(AXIS));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GasPropagator.propagateChangedPipe(level, pos);
    }
}
