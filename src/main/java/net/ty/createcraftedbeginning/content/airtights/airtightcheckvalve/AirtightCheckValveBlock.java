package net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCheckValveBlock extends AxisGasPipeBlock implements IBE<AirtightCheckValveBlockEntity>, IDirectionalPipe, IAirtightComponent {
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;

    public AirtightCheckValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(INVERTED, false).setValue(DIRECTIONAL_FACING, DirectionalFacing.NULL));
    }

    static boolean isInputSide(BlockState state, Direction direction) {
        if (state.getValue(AXIS) != direction.getAxis()) {
            return false;
        }

        boolean isPositiveDirection = direction.getAxisDirection() == AxisDirection.POSITIVE;
        return isPositiveDirection != state.getValue(INVERTED);
    }

    static boolean isOutputSide(BlockState state, Direction direction) {
        return state.getValue(AXIS) == direction.getAxis() && !isInputSide(state, direction);
    }

    private static Direction getOutputDirection(BlockState state) {
        AxisDirection axisDirection = state.getValue(INVERTED) ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
        return Direction.fromAxisAndDirection(state.getValue(AXIS), axisDirection);
    }

    private static BlockState setOutputDirection(BlockState state, Direction output) {
        return state.setValue(AXIS, output.getAxis()).setValue(INVERTED, output.getAxisDirection() == AxisDirection.POSITIVE);
    }

    private static BlockState setDirectionalFacing(BlockState state, Direction direction) {
        return state.setValue(DIRECTIONAL_FACING, DirectionalFacing.getFacingDirection(direction));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction output = rotation.rotate(getOutputDirection(state));
        BlockState rotatedState = setOutputDirection(super.rotate(state, rotation), output);
        DirectionalFacing facing = state.getValue(DIRECTIONAL_FACING);
        if (facing == DirectionalFacing.NULL) {
            return rotatedState;
        }
        return setDirectionalFacing(rotatedState, rotation.rotate(DirectionalFacing.getDirection(facing)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction output = mirror.mirror(getOutputDirection(state));
        BlockState mirroredState = setOutputDirection(super.mirror(state, mirror), output);
        DirectionalFacing facing = state.getValue(DIRECTIONAL_FACING);
        if (facing == DirectionalFacing.NULL) {
            return mirroredState;
        }
        return setDirectionalFacing(mirroredState, mirror.mirror(DirectionalFacing.getDirection(facing)));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockPos pos = context.getClickedPos();
        level.setBlockAndUpdate(pos, state.setValue(INVERTED, !state.getValue(INVERTED)));
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(INVERTED, DIRECTIONAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Axis axis = state.getValue(AXIS);
        Direction output = context.getNearestLookingDirection();
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() != axis) {
                continue;
            }

            output = direction;
            break;
        }

        boolean isInverted = output.getAxisDirection() == AxisDirection.POSITIVE;
        DirectionalFacing facing = DirectionalFacing.getFacingDirection(context.getHorizontalDirection());
        return state.setValue(INVERTED, isInverted).setValue(DIRECTIONAL_FACING, facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return CCBShapes.CHECK_VALVE.get(state.getValue(AXIS));
    }

    @Override
    public Class<AirtightCheckValveBlockEntity> getBlockEntityClass() {
        return AirtightCheckValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightCheckValveBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_CHECK_VALVE.get();
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return currentState.getValue(AXIS) == localFace.getAxis();
    }
}