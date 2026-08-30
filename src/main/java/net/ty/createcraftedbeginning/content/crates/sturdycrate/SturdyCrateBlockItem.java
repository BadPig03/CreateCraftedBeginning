package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
    public int getMaxStackSize(ItemStack stack) {
        if (!stack.has(CCBDataComponents.STURDY_CRATE_CONTENTS)) {
            return super.getMaxStackSize(stack);
        }
        return 1;
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity, DamageSource damageSource) {
        Level level = itemEntity.level();
        if (level.isClientSide) {
            return;
        }

        ItemStack crateStack = itemEntity.getItem();
        if (crateStack.isEmpty()) {
            return;
        }

        ItemEntity replacement = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), crateStack.copy(), itemEntity.getDeltaMovement().x, itemEntity.getDeltaMovement().y, itemEntity.getDeltaMovement().z);
        replacement.setTarget(itemEntity.getTarget());
        replacement.setDefaultPickUpDelay();
        level.addFreshEntity(replacement);
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }
}
