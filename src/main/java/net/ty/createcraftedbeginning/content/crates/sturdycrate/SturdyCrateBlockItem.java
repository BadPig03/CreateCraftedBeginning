package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.content.crates.CrateContainersUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

public class SturdyCrateBlockItem extends BlockItem {
    public SturdyCrateBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        SturdyCrateContents contents = stack.getOrDefault(CCBDataComponents.STURDY_CRATE_CONTENTS, SturdyCrateContents.empty());
        return contents.equals(SturdyCrateContents.empty()) ? super.getMaxStackSize(stack) : 1;
    }

    @Override
    public void onDestroyed(@NotNull ItemEntity itemEntity) {
        Level level = itemEntity.level();
        if (level.isClientSide) {
            return;
        }

        SturdyCrateContents contents = itemEntity.getItem().getOrDefault(CCBDataComponents.STURDY_CRATE_CONTENTS, SturdyCrateContents.empty());
        CrateContainersUtils.dropContents(level, itemEntity.position(), contents);
    }
}
