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
        Direction opposite = direction.getOpposite();
        if (GasCapabilities.hasGasCapability(level, neighbourPos, opposite)) {
            return true;
        }

        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, neighbourPos, GasTransportBehaviour.TYPE);
        return transport != null && transport.canHaveFlowToward(neighbour, opposite);
    }

    public static boolean isPump(BlockState state) {
        return state.getBlock() instanceof AirtightPumpBlock;
    }

    public static boolean isOpenAt(BlockState state, Direction direction) {
        return direction.getAxis() == state.getValue(FACING).getAxis();
    }

    private static @Nullable Direction findBestConnection(Level level, BlockPos pos, Direction targetDirection) {
        Direction bestDirection = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3 targetVector = Vec3.atLowerCornerOf(targetDirection.getNormal());
        for (Direction direction : Iterate.directions) {
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbour = level.getBlockState(neighbourPos);
            if (!canConnectTo(level, neighbourPos, neighbour, direction)) {
                continue;
            }

            Vec3 directionVector = Vec3.atLowerCornerOf(direction.getNormal());
            double distance = directionVector.distanceTo(targetVector);
            if (distance > bestDistance) {
                continue;
            }

            bestDistance = distance;
            bestDirection = direction;
        }

        return bestDirection;
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
        boolean isSneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        Direction lookDirection = context.getNearestLookingDirection();
        Direction targetDirection = isSneaking ? lookDirection : lookDirection.getOpposite();
        Direction connectedDirection = findBestConnection(level, pos, targetDirection);

        state = ProperWaterloggedBlock.withWater(level, state, pos);
        if (isSneaking || connectedDirection == null || connectedDirection.getAxis() == targetDirection.getAxis()) {
            return state;
        }
        return state.setValue(FACING, connectedDirection);
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
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            GasPropagator.propagatePipe(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
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
        Direction direction = GasPropagator.getChangedNeighbourSide(level, pos, neighborPos);
        if (direction == null || !isOpenAt(state, direction)) {
            return;
        }

        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
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
        GasPropagator.propagateChangedPipe(level, pos);
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
