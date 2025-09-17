package net.ty.createcraftedbeginning.content.airtights.checkvalve;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.api.gas.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.api.gas.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class CheckValveBlock extends AxisGasPipeBlock implements IBE<CheckValveBlockEntity>, IDirectionalPipe {
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;

    public CheckValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(INVERTED, false).setValue(DIRECTIONAL_FACING, DirectionalFacing.NULL));
    }

    @Override
    public InteractionResult onWrenched(@NotNull BlockState state, @NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState newState = state.setValue(INVERTED, !state.getValue(INVERTED));
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        IWrenchable.playRotateSound(level, pos);
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(INVERTED, DIRECTIONAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @NotNull BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction horizontalFacing = context.getHorizontalDirection();
        if (state.hasProperty(IDirectionalPipe.DIRECTIONAL_FACING)) {
            state = state.setValue(IDirectionalPipe.DIRECTIONAL_FACING, IDirectionalPipe.DirectionalFacing.getFacingDirection(horizontalFacing));
        }
        return state;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        return CCBShapes.CHECK_VALVE.get(state.getValue(AXIS));
    }

    @Override
    public Class<CheckValveBlockEntity> getBlockEntityClass() {
        return CheckValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CheckValveBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.CHECK_VALVE.get();
    }
}