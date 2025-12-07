package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateContainersUtils;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SturdyCrateBlock extends HorizontalDirectionalBlock implements IBE<SturdyCrateBlockEntity>, IWrenchable {
    public SturdyCrateBlock(Properties properties) {
        super(properties);
    }

    public static int getMaxCount() {
        return CCBConfig.server().crates.maxSturdyCapacity.get();
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(SturdyCrateBlock::new);
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
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity entity, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
        if (!(level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate)) {
            return;
        }

        crate.loadFromItem(stack);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        ItemStack crateItemEntity = new ItemStack(this);
        if (!(level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate) || crate.isEmpty() || !player.isShiftKeyDown()) {
            return crateItemEntity;
        }

        crate.saveToItem(crateItemEntity);
        return crateItemEntity;
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BrassCrateBlockEntity crate ? CrateContainersUtils.calculateRedstoneSignal(crate.getHandler()) : 0;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (player.isCreative() && !level.isClientSide && level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate && !crate.isEmpty()) {
            ItemStack crateItemEntity = new ItemStack(this);
            crate.saveToItem(crateItemEntity);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, crateItemEntity);
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull Builder params) {
        if (!(params.getParameter(LootContextParams.BLOCK_ENTITY) instanceof SturdyCrateBlockEntity crate)) {
            return super.getDrops(state, params);
        }

        ItemStack crateItemEntity = new ItemStack(this);
        crate.saveToItem(crateItemEntity);
        return Collections.singletonList(crateItemEntity);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CCBShapes.CRATE;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack crate, @NotNull TooltipContext context, @NotNull List<Component> tooltips, @NotNull TooltipFlag flag) {
        SturdyCrateContents contents = crate.getOrDefault(CCBDataComponents.STURDY_CRATE_CONTENTS, SturdyCrateContents.empty());
        ItemStack content = contents.content().copyWithCount(1);
        int count = contents.count();
        if (!content.isEmpty()) {
            tooltips.add(CCBLang.translate("gui.tooltips.sturdy_crate.item").add(CCBLang.text(Component.translatable(content.getDescriptionId()).getString())).style(ChatFormatting.GRAY).component());
        }
        tooltips.add(CCBLang.translate("gui.tooltips.sturdy_crate.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(count).style(ChatFormatting.GOLD)).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(getMaxCount()).style(ChatFormatting.DARK_GRAY)).component());
    }
}
