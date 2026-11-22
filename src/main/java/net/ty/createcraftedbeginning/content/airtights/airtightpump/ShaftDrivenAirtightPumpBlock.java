package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.AxisGasPipe;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.visual.GasPlacementHelper;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShaftDrivenAirtightPumpBlock extends DirectionalAxisKineticBlock implements IBE<AirtightPumpBlockEntity>, SimpleWaterloggedBlock, AxisGasPipe, GasConnectable {
    private static final SpeedLevel MINIMUM_REQUIRED_SPEED_LEVEL = SpeedLevel.MEDIUM;
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public ShaftDrivenAirtightPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS_ALONG_FIRST_COORDINATE, true).setValue(WATERLOGGED, false));
    }

    private static BlockState withShaftAxis(BlockState state, Axis shaftAxis) {
        Axis gasAxis = state.getValue(FACING).getAxis();
        boolean alongFirst = shaftAxis == (gasAxis == Axis.X ? Axis.Y : Axis.X);
        return state.setValue(AXIS_ALONG_FIRST_COORDINATE, alongFirst);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        Direction output = originalState.getValue(FACING);
        if (targetedFace.getAxis() == output.getAxis()) {
            return originalState.cycle(AXIS_ALONG_FIRST_COORDINATE);
        }

        return originalState.setValue(FACING, output.getOpposite());
    }

    @Override
    public Axis getAxis(BlockState state) {
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction output = GasPlacementHelper.choosePumpFacing(context, false);
        Player player = context.getPlayer();
        Direction preferredShaft = null;
        if (player == null || !player.isShiftKeyDown()) {
            preferredShaft = GasPlacementHelper.getPreferredShaft(level, pos);
        }

        Axis shaftAxis = preferredShaft == null ? null : preferredShaft.getAxis();
        if (shaftAxis == output.getAxis() && GasPlacementHelper.getConnectableDirections(level, pos).isEmpty()) {
            for (Direction direction : context.getNearestLookingDirections()) {
                if (direction.getAxis() == shaftAxis) {
                    continue;
                }

                output = direction.getOpposite();
                break;
            }
        }

        Axis gasAxis = output.getAxis();
        if (shaftAxis == null || shaftAxis == gasAxis) {
            shaftAxis = gasAxis == Axis.X ? Axis.Z : Axis.X;
            if (gasAxis == Axis.Y) {
                shaftAxis = context.getHorizontalDirection().getClockWise().getAxis();
            }
        }

        BlockState state = withShaftAxis(defaultBlockState().setValue(FACING, output), shaftAxis);
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Direction shaft = Direction.fromAxisAndDirection(getRotationAxis(state), AxisDirection.POSITIVE);
        return withShaftAxis(state.setValue(FACING, rotation.rotate(state.getValue(FACING))), rotation.rotate(shaft).getAxis());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        if (transform.mirror != null) {
            state = mirror(state, transform.mirror);
        }

        Direction shaft = Direction.fromAxisAndDirection(getRotationAxis(state), AxisDirection.POSITIVE);
        return withShaftAxis(state.setValue(FACING, transform.rotateFacing(state.getValue(FACING))), transform.rotateFacing(shaft).getAxis());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || state == oldState) {
            return;
        }

        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
            if (transport != null) {
                transport.finalizePendingTransfersBeforeBlockRemoval();
            }

            GasNetworkTopology.invalidate(level, pos);
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
        Direction changedDirection = GasConnectionResolver.getChangedNeighborFace(level, pos, neighborPos);
        if (changedDirection == null || changedDirection.getAxis() != state.getValue(FACING).getAxis()) {
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
        return CCBShapes.SHAFT_DRIVEN_AIRTIGHT_PUMP.get(getAxis(state));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GasNetworkTopology.invalidate(level, pos);
    }

    @Override
    public Class<AirtightPumpBlockEntity> getBlockEntityClass() {
        return AirtightPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightPumpBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.SHAFT_DRIVEN_AIRTIGHT_PUMP.get();
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return currentState.getValue(FACING).getAxis() == localFace.getAxis();
    }

    @Override
    public boolean propagatesGasNeighborUpdates() {
        return false;
    }

    @Override
    public boolean blocksAtmosphere(BlockPos pos, BlockState state, Direction face) {
        return canConnectOnFace(pos, state, face);
    }

}
