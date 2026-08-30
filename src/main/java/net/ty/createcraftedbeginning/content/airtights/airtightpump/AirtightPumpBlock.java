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
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPumpBlock extends DirectionalKineticBlock implements IBE<AirtightPumpBlockEntity>, SimpleWaterloggedBlock, ICogWheel, IAirtightComponent {
    static final SpeedLevel MINIMUM_REQUIRED_SPEED_LEVEL = SpeedLevel.MEDIUM;
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    private static boolean canConnectTo(Level level, BlockPos neighbourPos, BlockState neighbourState, Direction direction) {
        Direction oppositeDirection = direction.getOpposite();
        if (GasCapabilities.hasGasCapability(level, neighbourPos, oppositeDirection)) {
            return true;
        }

        GasTransportBehaviour transportBehaviour = BlockEntityBehaviour.get(level, neighbourPos, GasTransportBehaviour.TYPE);
        return transportBehaviour != null && transportBehaviour.canHaveFlowToward(neighbourState, oppositeDirection);
    }

    private static boolean isPump(BlockState state) {
        return state.getBlock() instanceof AirtightPumpBlock;
    }

    private static boolean isOpenAt(BlockState state, Direction direction) {
        return direction.getAxis() == state.getValue(FACING).getAxis();
    }

    private static @Nullable Direction findBestConnection(Level level, BlockPos pos, Direction targetDirection) {
        Direction bestDirection = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3 targetDirectionVector = Vec3.atLowerCornerOf(targetDirection.getNormal());
        for (Direction direction : Iterate.directions) {
            BlockPos neighbourPos = pos.relative(direction);
            if (!canConnectTo(level, neighbourPos, level.getBlockState(neighbourPos), direction)) {
                continue;
            }

            double distance = Vec3.atLowerCornerOf(direction.getNormal()).distanceTo(targetDirectionVector);
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
        return MINIMUM_REQUIRED_SPEED_LEVEL;
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
        if (!isPump(state) || !isPump(oldState) || state.getValue(FACING) != oldState.getValue(FACING).getOpposite() || !(level.getBlockEntity(pos) instanceof AirtightPumpBlockEntity pump)) {
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, placer);
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
        Direction changedDirection = GasPropagator.getChangedNeighbourSide(level, pos, neighborPos);
        if (changedDirection == null || !isOpenAt(state, changedDirection)) {
            return;
        }

        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (!state.getValue(WATERLOGGED)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return Fluids.WATER.getSource(false);
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
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return currentState.getValue(FACING).getAxis() == localFace.getAxis();
    }
}
