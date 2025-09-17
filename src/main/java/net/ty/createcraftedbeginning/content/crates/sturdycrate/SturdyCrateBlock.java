package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SturdyCrateBlock extends HorizontalDirectionalBlock implements IBE<SturdyCrateBlockEntity>, IWrenchable {
    public static final int MAX_SLOT = 16;
    public static final int SLOT_LIMIT = 64;

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
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SturdyCrateBlockEntity crate)) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        ItemStackHandler inv = crate.getInv();
        for (int i = 0; i < MAX_SLOT; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (!stack.getItem().canFitInsideContainerItems()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        BlockEntity blockentity = params.getParameter(LootContextParams.BLOCK_ENTITY);
        if (blockentity instanceof SturdyCrateBlockEntity crate) {
            ItemStack stack = new ItemStack(this);
            crate.saveToItem(stack);
            return Collections.singletonList(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos) {
        return Helpers.calculateRedstoneSignal(this, pLevel, pPos);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CCBShapes.CRATE;
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(level, pos, placer);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SturdyCrateBlockEntity crate) {
            crate.loadFromItem(stack);
        }
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
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
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        ItemContainerContents container = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int maxSlots = container.getSlots();
        if (maxSlots == 0) {
            return;
        }

        int totalCount = 0;
        ItemStack firstItem = ItemStack.EMPTY;

        for (int i = 0; i < maxSlots; i++) {
            ItemStack inSlot = container.getStackInSlot(i);
            if (inSlot.isEmpty()) {
                continue;
            }

            if (firstItem.isEmpty()) {
                firstItem = inSlot.copyWithCount(1);
            }
            totalCount += inSlot.getCount();
        }

        if (firstItem.isEmpty()) {
            return;
        }

        tooltipComponents.add(CCBLang.translateDirect("tooltips.sturdy_crate.item").withStyle(ChatFormatting.GRAY).append(Component.literal(firstItem.getHoverName().getString()).withStyle(ChatFormatting.GOLD)));
        tooltipComponents.add(CCBLang.builder().add(CCBLang.translateDirect("tooltips.sturdy_crate.count").withStyle(ChatFormatting.GRAY)).add(CCBLang.number(totalCount).component().withStyle(ChatFormatting.AQUA)).add(CCBLang.text(" / ").component().withStyle(ChatFormatting.GRAY)).add(CCBLang.number(MAX_SLOT * SLOT_LIMIT).component().withStyle(ChatFormatting.DARK_AQUA)).component());
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
