package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateBlock;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SturdyCrateBlock extends CrateBlock<SturdyCrateBlockEntity> {
    public SturdyCrateBlock(Properties properties) {
        super(properties);
    }

    public static int getMaxCount() {
        return CCBConfig.server().crates.maxSturdyCapacity.get();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (!(level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate)) {
            return;
        }

        crate.loadFromItem(stack);
    }

    @Override
    protected void onCrateRemoved(Level level, BlockPos pos, SturdyCrateBlockEntity crate, boolean isMoving) {
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack crateItem = new ItemStack(this);
        if (!(level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate) || !crate.hasStoredData() || !player.isShiftKeyDown()) {
            return crateItem;
        }

        crate.saveToItem(crateItem);
        return crateItem;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isCreative() && !level.isClientSide && level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate && crate.hasStoredData()) {
            ItemStack crateItem = new ItemStack(this);
            crate.saveToItem(crateItem);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, crateItem);
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public void appendHoverText(ItemStack crateStack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        SturdyCrateContents storedContents = crateStack.getOrDefault(CCBDataComponents.STURDY_CRATE_CONTENTS, SturdyCrateContents.empty());
        ItemStack storedItem = storedContents.content();
        int storedCount = storedContents.count();
        if (!storedItem.isEmpty()) {
            tooltips.add(CCBLang.translate("gui.sturdy_crate.item").add(CCBLang.itemName(storedItem)).style(ChatFormatting.GRAY).component());
        }

        tooltips.add(CCBLang.translate("gui.sturdy_crate.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(storedCount).style(ChatFormatting.GOLD)).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(getMaxCount()).style(ChatFormatting.DARK_GRAY)).component());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, Builder params) {
        if (!(params.getParameter(LootContextParams.BLOCK_ENTITY) instanceof SturdyCrateBlockEntity crate)) {
            return super.getDrops(state, params);
        }

        ItemStack crateItem = new ItemStack(this);
        crate.saveToItem(crateItem);
        return Collections.singletonList(crateItem);
    }
}
