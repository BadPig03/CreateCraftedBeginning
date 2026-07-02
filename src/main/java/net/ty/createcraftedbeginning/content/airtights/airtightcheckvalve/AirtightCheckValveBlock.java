package net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.data.CCBShapes;
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

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockPos pos = context.getClickedPos();
        BlockState newState = state.setValue(INVERTED, !state.getValue(INVERTED));
        level.setBlockAndUpdate(pos, newState);
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
        GasPropagator.propagateChangedPipe(level, pos, newState);
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
        Direction horizontalFacing = context.getHorizontalDirection();
        return state.setValue(DIRECTIONAL_FACING, DirectionalFacing.getFacingDirection(horizontalFacing));
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
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return currentState.getValue(AXIS) == oppositeDirection.getAxis();
    }
}