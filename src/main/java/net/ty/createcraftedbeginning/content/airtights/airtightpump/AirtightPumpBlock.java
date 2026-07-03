package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPumpBlock extends DirectionalKineticBlock implements IBE<AirtightPumpBlockEntity>, SimpleWaterloggedBlock, ICogWheel, IAirtightComponent {
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public static boolean canConnectTo(Level level, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
        if (GasCapabilities.hasGasCapability(level, neighbourPos, direction.getOpposite())) {
            return true;
        }

        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, neighbourPos, GasTransportBehaviour.TYPE);
        return transport != null && transport.canHaveFlowToward(neighbour, direction.getOpposite());
    }

    public static boolean isPump(BlockState state) {
        return state.getBlock() instanceof AirtightPumpBlock;
    }

    public static boolean isOpenAt(BlockState state, Direction direction) {
        return direction.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return originalState.setValue(FACING, originalState.getValue(FACING).getOpposite());
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean isShiftKeyDown = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        state = ProperWaterloggedBlock.withWater(level, state, pos);

        Direction nearestLookingDirection = context.getNearestLookingDirection();
        Direction targetDirection = isShiftKeyDown ? nearestLookingDirection : nearestLookingDirection.getOpposite();
        Direction bestConnectedDirection = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Iterate.directions) {
            BlockPos adjPos = pos.relative(direction);
            BlockState adjState = level.getBlockState(adjPos);
            if (!canConnectTo(level, adjPos, adjState, direction)) {
                continue;
            }

            double distance = Vec3.atLowerCornerOf(direction.getNormal()).distanceTo(Vec3.atLowerCornerOf(targetDirection.getNormal()));
            if (distance > bestDistance) {
                continue;
            }

            bestDistance = distance;
            bestConnectedDirection = direction;
        }

        return bestConnectedDirection != null && bestConnectedDirection.getAxis() != targetDirection.getAxis() && !isShiftKeyDown ? state.setValue(FACING, bestConnectedDirection) : state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide) {
            return;
        }

        if (state != oldState) {
            level.scheduleTick(pos, this, 1, TickPriority.HIGH);
        }
        if (!isPump(state) || !isPump(oldState) || state.getValue(FACING) != oldState.getValue(FACING).getOpposite()) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof AirtightPumpBlockEntity pump)) {
            return;
        }

        pump.markPressureUpdate();
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
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_PUMP.get(state.getValue(FACING));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GasPropagator.propagatePipe(level, pos, state);
    }

    @Override
    public Class<AirtightPumpBlockEntity> getBlockEntityClass() {
        return AirtightPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightPumpBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_PUMP.get();
    }

    @Override
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return currentState.getValue(FACING).getAxis() == oppositeDirection.getAxis();
    }
}
