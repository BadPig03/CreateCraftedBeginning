package net.ty.createcraftedbeginning.content.sturdycrate;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SturdyCrateBlock extends HorizontalDirectionalBlock implements IBE<SturdyCrateBlockEntity>, IWrenchable {
    public static final int SLOT_LIMIT = 1;
    static final int MAX_SLOT = 128;

    public static final MapCodec<SturdyCrateBlock> CODEC = simpleCodec(SturdyCrateBlock::new);

    public SturdyCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SturdyCrateBlockEntity> getBlockEntityClass() {
        return SturdyCrateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SturdyCrateBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.STURDY_CRATE.get();
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!level.isClientSide && player.isCreative() && blockEntity instanceof SturdyCrateBlockEntity crate && !crate.isEmpty()) {
            ItemStack itemStack = new ItemStack(this);
            crate.saveToItem(itemStack);
            ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemStack);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.Builder params) {
        BlockEntity blockentity = params.getParameter(LootContextParams.BLOCK_ENTITY);
        if (blockentity instanceof SturdyCrateBlockEntity crate) {
            ItemStack stack = new ItemStack(this);
            crate.saveToItem(stack);
            return Collections.singletonList(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SturdyCrateBlockEntity crate) {
            crate.loadFromItem(stack);
        }
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SturdyCrateBlockEntity crate)) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        for (int i = 0; i < MAX_SLOT; i++) {
            ItemStack stack = crate.inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (!stack.getItem().canFitInsideContainerItems()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                crate.inv.setStackInSlot(i, ItemStack.EMPTY);
            }
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
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
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
