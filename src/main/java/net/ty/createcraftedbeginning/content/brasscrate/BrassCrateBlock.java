package net.ty.createcraftedbeginning.content.brasscrate;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;

public class BrassCrateBlock extends HorizontalDirectionalBlock implements IBE<BrassCrateBlockEntity>, IWrenchable {
    public static final int SLOT_LIMIT = 1;
    static final int MAX_SLOT = 64;

    public static final MapCodec<BrassCrateBlock> CODEC = simpleCodec(BrassCrateBlock::new);

    public BrassCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<BrassCrateBlockEntity> getBlockEntityClass() {
        return BrassCrateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BrassCrateBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.BRASS_CRATE.get();
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BrassCrateBlockEntity crate)) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        for (int i = 0; i < MAX_SLOT; i++) {
            ItemStack stack = crate.inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos) {
        return Helpers.calculateRedstoneSignal(this, pLevel, pPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState toPlace = super.getStateForPlacement(context);
        return toPlace == null ? Blocks.AIR.defaultBlockState() : toPlace.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return CCBShapes.CRATE;
	}

    @Override
	protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}
}
