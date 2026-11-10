package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalReceiverBlock extends HorizontalDirectionalBlock implements IBE<PhotothermalReceiverBlockEntity>, IWrenchable {
    private static final int HEATED_COMPARATOR_SIGNAL = 5;
    private static final int SUPERHEATED_COMPARATOR_SIGNAL = 15;

    public PhotothermalReceiverBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.NONE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(PhotothermalReceiverBlock::new);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return originalState.setValue(FACING, originalState.getValue(FACING).getClockWise());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        BlockPos heatedPos = pos.above();
        if (!level.isLoaded(heatedPos)) {
            return;
        }

        BlockEntity heatedBlockEntity = level.getBlockEntity(heatedPos);
        if (!(heatedBlockEntity instanceof BasinBlockEntity basin)) {
            return;
        }

        basin.notifyChangeOfContents();
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, BlazeBurnerBlock.HEAT_LEVEL);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return switch (state.getValue(BlazeBurnerBlock.HEAT_LEVEL)) {
            case KINDLED -> HEATED_COMPARATOR_SIGNAL;
            case SEETHING -> SUPERHEATED_COMPARATOR_SIGNAL;
            default -> 0;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.PHOTOTHERMAL_RECEIVER;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof LaserCollisionContext) {
            return PhotothermalReceiverPort.getLaserShape();
        }

        return getShape(state, level, pos, context);
    }

    @Override
    public Class<PhotothermalReceiverBlockEntity> getBlockEntityClass() {
        return PhotothermalReceiverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhotothermalReceiverBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.PHOTOTHERMAL_RECEIVER.get();
    }
}
