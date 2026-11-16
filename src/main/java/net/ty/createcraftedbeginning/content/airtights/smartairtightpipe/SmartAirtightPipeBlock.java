package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import net.ty.createcraftedbeginning.gas.visual.GasPipeOrientation;
import net.ty.createcraftedbeginning.gas.visual.GasPlacementHelper;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeBlock extends AxisGasPipeBlock implements IBE<SmartAirtightPipeBlockEntity>, GasConnectable, TransformableBlock {
    public SmartAirtightPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(GasPipeOrientation.PANEL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(GasPipeOrientation.PANEL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Axis axis = state.getValue(AXIS);
        Direction nearest = context.getNearestLookingDirections()[0];
        Direction panel = nearest.getOpposite();
        EnumSet<Axis> candidates = GasPlacementHelper.getConnectableAxes(GasPlacementHelper.getConnectableDirections(context.getLevel(), context.getClickedPos()));
        if (axis == Axis.Y) {
            if (candidates.size() == 1) {
                panel = nearest.getAxis().isVertical() ? context.getHorizontalDirection().getOpposite() : nearest;
            }
        }
        else if (panel.getAxis().isHorizontal()) {
            panel = Direction.UP;
        }
        return state.setValue(GasPipeOrientation.PANEL_FACING, panel);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        if (targetedFace.getAxis() != Axis.Y) {
            return state;
        }

        return rotate(state, Rotation.CLOCKWISE_90);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation).setValue(GasPipeOrientation.PANEL_FACING, rotation.rotate(GasPipeOrientation.panel(state, state.getValue(AXIS))));
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
        Axis axis = state.getValue(AXIS);
        return GasPipeOrientation.shape(axis, GasPipeOrientation.panel(state, axis));
    }

    @Override
    public Class<SmartAirtightPipeBlockEntity> getBlockEntityClass() {
        return SmartAirtightPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmartAirtightPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.SMART_AIRTIGHT_PIPE.get();
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return currentState.getValue(AXIS) == localFace.getAxis();
    }
}
