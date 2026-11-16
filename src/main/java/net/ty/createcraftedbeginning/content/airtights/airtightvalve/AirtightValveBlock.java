package net.ty.createcraftedbeginning.content.airtights.airtightvalve;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.visual.GasPipeOrientation;
import net.ty.createcraftedbeginning.gas.visual.GasPlacementHelper;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightValveBlock extends KineticBlock implements IBE<AirtightValveBlockEntity>, SimpleWaterloggedBlock, IAxisPipe, GasConnectable, TransformableBlock {
    public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Axis.Z).setValue(GasPipeOrientation.PANEL_FACING, Direction.UP).setValue(OPEN, false).setValue(WATERLOGGED, false));
    }

    static Axis getShaftAxis(BlockState state) {
        Axis axis = state.getValue(AXIS);
        return GasPipeOrientation.shaft(axis, GasPipeOrientation.panel(state, axis));
    }

    private static boolean isGasFace(BlockState state, Direction direction) {
        return direction.getAxis() == state.getValue(AXIS);
    }

    @Override
    protected MapCodec<? extends KineticBlock> codec() {
        return simpleCodec(AirtightValveBlock::new);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean isSneaking = player != null && player.isShiftKeyDown();
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (isSneaking) {
            facing = facing.getOpposite();
        }

        EnumSet<Direction> connections = GasPlacementHelper.getConnectableDirections(level, pos);
        Axis faceAxis = facing.getAxis();
        Axis gasAxis;
        if (faceAxis.isHorizontal()) {
            Direction perpendicular = faceAxis == Axis.X ? Direction.SOUTH : Direction.EAST;
            boolean verticalShaft = false;
            for (Direction side : Iterate.directionsInAxis(Axis.Y)) {
                BlockPos neighborPos = pos.relative(side);
                BlockState neighbor = level.getBlockState(neighborPos);
                if (neighbor.getBlock() instanceof IRotate rotating && rotating.hasShaftTowards(level, neighborPos, neighbor, side.getOpposite())) {
                    verticalShaft = true;
                }
            }
            gasAxis = verticalShaft || connections.contains(perpendicular) || connections.contains(perpendicular.getOpposite()) ? perpendicular.getAxis() : Axis.Y;
        }
        else {
            Axis shaftAxis = context.getHorizontalDirection().getAxis() == Axis.X ? Axis.X : Axis.Z;
            Direction preferred = null;
            for (Direction side : Iterate.horizontalDirections) {
                BlockPos neighborPos = pos.relative(side);
                BlockState neighbor = level.getBlockState(neighborPos);
                boolean shaft = neighbor.getBlock() instanceof IRotate rotating && rotating.hasShaftTowards(level, neighborPos, neighbor, side.getOpposite());
                if (!shaft && !connections.contains(side.getClockWise())) {
                    continue;
                }

                if (preferred != null && preferred.getAxis() != side.getAxis()) {
                    preferred = null;
                    break;
                }

                preferred = side;
            }
            if (preferred != null) {
                shaftAxis = preferred.getAxis();
            }
            gasAxis = shaftAxis == Axis.X ? Axis.Z : Axis.X;
        }
        BlockState state = defaultBlockState().setValue(AXIS, gasAxis).setValue(GasPipeOrientation.PANEL_FACING, facing);
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(AXIS, GasPipeOrientation.PANEL_FACING, OPEN, WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Axis getAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return getShaftAxis(state);
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
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        Axis axis = state.getValue(AXIS);
        Axis rotationAxis = targetedFace.getAxis();
        Direction pipe = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE).getClockWise(rotationAxis);
        Direction panel = GasPipeOrientation.panel(state, axis).getClockWise(rotationAxis);
        return state.setValue(AXIS, pipe.getAxis()).setValue(GasPipeOrientation.PANEL_FACING, panel);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction pipe = rotation.rotate(Direction.fromAxisAndDirection(state.getValue(AXIS), AxisDirection.POSITIVE));
        Direction panel = rotation.rotate(GasPipeOrientation.panel(state, state.getValue(AXIS)));
        return state.setValue(AXIS, pipe.getAxis()).setValue(GasPipeOrientation.PANEL_FACING, panel);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(GasPipeOrientation.PANEL_FACING, mirror.mirror(GasPipeOrientation.panel(state, state.getValue(AXIS))));
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        if (transform.mirror != null) {
            state = mirror(state, transform.mirror);
        }
        Axis axis = state.getValue(AXIS);
        Direction direction = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
        Direction panel = GasPipeOrientation.panel(state, axis);
        return state.setValue(AXIS, transform.rotateFacing(direction).getAxis()).setValue(GasPipeOrientation.PANEL_FACING, transform.rotateFacing(panel));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_VALVE.get(state.getValue(AXIS));
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
    public Class<AirtightValveBlockEntity> getBlockEntityClass() {
        return AirtightValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightValveBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_VALVE.get();
    }
}
