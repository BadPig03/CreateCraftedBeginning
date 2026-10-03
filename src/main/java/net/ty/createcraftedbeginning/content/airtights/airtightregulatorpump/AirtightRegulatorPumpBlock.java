package net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.DirectionalGasPipe;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.visual.GasPlacementHelper;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightRegulatorPumpBlock extends KineticBlock implements IBE<AirtightRegulatorPumpBlockEntity>, SimpleWaterloggedBlock, IWrenchable, IAxisPipe, DirectionalGasPipe, GasConnectable {
    private static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final SpeedLevel MINIMUM_REQUIRED_SPEED_LEVEL = SpeedLevel.MEDIUM;
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightRegulatorPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(DIRECTIONAL_FACING, DirectionalFacing.NULL).setValue(WATERLOGGED, false));
    }

    static Direction getOutputDirection(BlockState state) {
        return state.getValue(FACING);
    }

    static Direction getInputDirection(BlockState state) {
        return getOutputDirection(state).getOpposite();
    }

    static Axis getShaftAxis(BlockState state) {
        Axis gasAxis = getOutputDirection(state).getAxis();
        Axis panelAxis = getPanelDirection(state).getAxis();
        if (gasAxis != Axis.X && panelAxis != Axis.X) {
            return Axis.X;
        }

        if (gasAxis != Axis.Y && panelAxis != Axis.Y) {
            return Axis.Y;
        }

        return Axis.Z;
    }

    private static Direction getPanelDirection(BlockState state) {
        Axis gasAxis = getOutputDirection(state).getAxis();
        if (gasAxis != Axis.Y) {
            return Direction.UP;
        }

        return DirectionalFacing.getDirection(state.getValue(DIRECTIONAL_FACING)).getOpposite();
    }

    private static boolean isGasFace(BlockState state, Direction direction) {
        return direction.getAxis() == getOutputDirection(state).getAxis();
    }

    private static BlockState setDirectionalFacing(BlockState state, Direction direction) {
        return state.setValue(DIRECTIONAL_FACING, DirectionalFacing.getFacingDirection(direction));
    }

    @Override
    protected MapCodec<? extends KineticBlock> codec() {
        return simpleCodec(AirtightRegulatorPumpBlock::new);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean isSneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        EnumSet<Direction> connectedDirections = GasPlacementHelper.getConnectableDirections(level, pos);
        Axis gasAxis = isSneaking ? context.getNearestLookingDirection().getAxis() : GasPlacementHelper.chooseAxis(context, GasPlacementHelper.getConnectableAxes(connectedDirections));
        Direction outputDirection = GasPlacementHelper.chooseOutputDirection(context, gasAxis, connectedDirections, !isSneaking);
        DirectionalFacing panelFacing = DirectionalFacing.getFacingDirection(context.getHorizontalDirection());
        BlockState state = defaultBlockState().setValue(FACING, outputDirection).setValue(DIRECTIONAL_FACING, panelFacing);
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, DIRECTIONAL_FACING, WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getOpposite()));
        GasNetworkTopology.invalidate(level, pos);
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Axis getAxis(BlockState state) {
        return getOutputDirection(state).getAxis();
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return getShaftAxis(state);
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return MINIMUM_REQUIRED_SPEED_LEVEL;
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return isGasFace(currentState, localFace);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        Direction changedDirection = GasConnectionResolver.getChangedNeighborFace(level, pos, neighborPos);
        if (changedDirection == null || !isGasFace(state, changedDirection)) {
            return;
        }

        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (!state.getValue(WATERLOGGED)) {
            return super.getFluidState(state);
        }

        return Fluids.WATER.defaultFluidState();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        DirectionalFacing facing = state.getValue(DIRECTIONAL_FACING);
        if (facing == DirectionalFacing.NULL) {
            return rotated;
        }

        return setDirectionalFacing(rotated, rotation.rotate(DirectionalFacing.getDirection(facing)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        DirectionalFacing facing = state.getValue(DIRECTIONAL_FACING);
        if (facing == DirectionalFacing.NULL) {
            return mirrored;
        }

        return setDirectionalFacing(mirrored, mirror.mirror(DirectionalFacing.getDirection(facing)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        Axis gasAxis = getOutputDirection(state).getAxis();
        if (gasAxis != Axis.Y) {
            return CCBShapes.AIRTIGHT_REGULATOR_PUMP.get(gasAxis);
        }

        return CCBShapes.AIRTIGHT_REGULATOR_PUMP_VERTICAL.get(getPanelDirection(state));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GasNetworkTopology.invalidate(level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || state == oldState) {
            return;
        }

        GasNetworkTopology.invalidate(level, pos);
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
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getShaftAxis(state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, placer);
    }

    @Override
    public Class<AirtightRegulatorPumpBlockEntity> getBlockEntityClass() {
        return AirtightRegulatorPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightRegulatorPumpBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_REGULATOR_PUMP.get();
    }
}
