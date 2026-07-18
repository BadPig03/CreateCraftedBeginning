package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.content.crates.CrateContainersUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SturdyCrateBlockItem extends BlockItem {
    public SturdyCrateBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        Level level = itemEntity.level();
        if (level.isClientSide) {
            return;
        }

        SturdyCrateContents contents = itemEntity.getItem().get(CCBDataComponents.STURDY_CRATE_CONTENTS);
        if (contents == null) {
            return;
        }

        CrateContainersUtils.dropContents(level, itemEntity.position(), contents);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.has(CCBDataComponents.STURDY_CRATE_CONTENTS) ? 1 : super.getMaxStackSize(stack);
    }
}
