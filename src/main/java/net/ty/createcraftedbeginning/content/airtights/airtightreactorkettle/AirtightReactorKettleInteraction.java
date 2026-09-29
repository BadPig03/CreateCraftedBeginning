package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.registry.CCBDamageSources;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightReactorKettleInteraction {
    private AirtightReactorKettleInteraction() {
    }

    static void insertItemEntity(AirtightReactorKettleStructuralBlockEntity structural, ItemEntity itemEntity) {
        AirtightReactorKettleBlockEntity kettle = structural.getMasterBlockEntity();
        if (kettle == null) {
            return;
        }

        ItemStack insertionRemainder = ItemHandlerHelper.insertItemStacked(kettle.getInventories().getFirst(), itemEntity.getItem().copy(), false);
        if (insertionRemainder.isEmpty()) {
            itemEntity.discard();
            return;
        }

        itemEntity.setItem(insertionRemainder);
    }

    static ItemInteractionResult getUseItemOnResult(AirtightReactorKettleStructuralBlockEntity structural, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        AirtightReactorKettleBlockEntity kettle = structural.getMasterBlockEntity();
        if (kettle == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty()) {
            extractStoredItems(kettle, level, player, pos);
            return ItemInteractionResult.SUCCESS;
        }

        if (level.isClientSide) {
            boolean canEmptyContainer = GenericItemEmptying.canItemBeEmptied(level, stack);
            boolean canFillContainer = GenericItemFilling.canItemBeFilled(level, stack);
            if (canEmptyContainer || canFillContainer) {
                return ItemInteractionResult.SUCCESS;
            }

            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, kettle) || GenericItemEmptying.canItemBeEmptied(level, stack)) {
            return ItemInteractionResult.SUCCESS;
        }

        if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, kettle) || GenericItemFilling.canItemBeFilled(level, stack)) {
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

    }

    static void updateRecipeFilter(AirtightReactorKettleStructuralBlockEntity structural, ItemStack stack) {
        Level level = structural.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        AirtightReactorKettleBlockEntity kettle = structural.getMasterBlockEntity();
        if (kettle == null) {
            return;
        }

        kettle.setRecipeFilter(stack);
    }

    static void hurtInsideLivingEntities(AirtightReactorKettleStructuralBlockEntity structural, LivingEntity livingEntity) {
        AirtightReactorKettleBlockEntity kettle = structural.getMasterBlockEntity();
        if (kettle == null) {
            return;
        }

        Level level = kettle.getLevel();
        if (level == null) {
            return;
        }

        float mixerDamage = kettle.getDamage();
        if (mixerDamage == 0) {
            return;
        }

        livingEntity.hurt(CCBDamageSources.reactorKettleMixer(level), mixerDamage);
    }

    private static void extractStoredItems(AirtightReactorKettleBlockEntity kettle, Level level, Player player, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        boolean extractedInputItems = extractStoredItems(kettle.getInputInventory(), player);
        boolean extractedOutputItems = extractStoredItems(kettle.getOutputInventory(), player);
        if (!extractedInputItems && !extractedOutputItems) {
            return;
        }

        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1 + level.getRandom().nextFloat());
    }

    private static boolean extractStoredItems(IItemHandlerModifiable inventory, Player player) {
        boolean extractedAny = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack storedStack = inventory.getStackInSlot(slot);
            if (storedStack.isEmpty()) {
                continue;
            }

            ItemHandlerHelper.giveItemToPlayer(player, storedStack);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            extractedAny = true;
        }
        return extractedAny;
    }
}
