package net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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
import net.minecraft.world.entity.player.Player;
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
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.visual.GasPipeOrientation;
import net.ty.createcraftedbeginning.gas.visual.GasPlacementHelper;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightRegulatorPumpBlock extends KineticBlock implements IBE<AirtightRegulatorPumpBlockEntity>, SimpleWaterloggedBlock, IWrenchable, IAxisPipe, GasConnectable, TransformableBlock {
    private static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final SpeedLevel MINIMUM_REQUIRED_SPEED_LEVEL = SpeedLevel.MEDIUM;
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightRegulatorPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GasPipeOrientation.PANEL_FACING, Direction.UP).setValue(WATERLOGGED, false));
    }

    static Direction getOutputDirection(BlockState state) {
        return state.getValue(FACING);
    }

    static Direction getInputDirection(BlockState state) {
        return getOutputDirection(state).getOpposite();
    }

    static Axis getShaftAxis(BlockState state) {
        Axis axis = getOutputDirection(state).getAxis();
        return GasPipeOrientation.shaft(axis, GasPipeOrientation.panel(state, axis));
    }

    private static boolean isGasFace(BlockState state, Direction direction) {
        return direction.getAxis() == getOutputDirection(state).getAxis();
    }

    @Override
    protected MapCodec<? extends KineticBlock> codec() {
        return simpleCodec(AirtightRegulatorPumpBlock::new);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction outputDirection = GasPlacementHelper.choosePumpFacing(context, false);
        Direction preferredShaft = null;
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isShiftKeyDown();
        if (!sneaking) {
            preferredShaft = GasPlacementHelper.getPreferredShaft(level, pos);
        }
        Axis shaftAxis = preferredShaft == null ? null : preferredShaft.getAxis();
        if (shaftAxis == outputDirection.getAxis() && GasPlacementHelper.getConnectableDirections(level, pos).isEmpty()) {
            for (Direction direction : context.getNearestLookingDirections()) {
                if (direction.getAxis() == shaftAxis) {
                    continue;
                }

                outputDirection = direction.getOpposite();
                break;
            }
        }
        Axis gasAxis = outputDirection.getAxis();
        Direction panel = gasAxis == Axis.Y ? context.getHorizontalDirection().getOpposite() : Direction.UP;
        if (shaftAxis != null && shaftAxis != gasAxis) {
            for (Direction direction : context.getNearestLookingDirections()) {
                if (direction.getAxis() == shaftAxis || direction.getAxis() == gasAxis) {
                    continue;
                }

                panel = direction.getOpposite();
                break;
            }
        }
        BlockState state = defaultBlockState().setValue(FACING, outputDirection).setValue(GasPipeOrientation.PANEL_FACING, panel);
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, GasPipeOrientation.PANEL_FACING, WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        Direction output = getOutputDirection(originalState);
        Axis gasAxis = output.getAxis();
        if (targetedFace.getAxis() == gasAxis) {
            Direction panel = GasPipeOrientation.panel(originalState, gasAxis);
            return originalState.setValue(GasPipeOrientation.PANEL_FACING, panel.getClockWise(gasAxis));
        }

        return originalState.setValue(FACING, output.getOpposite());
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        KineticBlockEntity.switchToBlockState(level, pos, getRotatedBlockState(state, context.getClickedFace()));
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
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING))).setValue(GasPipeOrientation.PANEL_FACING, rotation.rotate(GasPipeOrientation.panel(state, getOutputDirection(state).getAxis())));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(GasPipeOrientation.PANEL_FACING, mirror.mirror(GasPipeOrientation.panel(state, getOutputDirection(state).getAxis())));
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        if (transform.mirror != null) {
            state = mirror(state, transform.mirror);
        }
        Axis axis = getOutputDirection(state).getAxis();
        Direction direction = state.getValue(FACING);
        Direction panel = GasPipeOrientation.panel(state, axis);
        return state.setValue(FACING, transform.rotateFacing(direction)).setValue(GasPipeOrientation.PANEL_FACING, transform.rotateFacing(panel));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        Axis axis = getOutputDirection(state).getAxis();
        return GasPipeOrientation.shape(axis, GasPipeOrientation.panel(state, axis));
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
