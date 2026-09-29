package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightForgingPressInteraction {
    private AirtightForgingPressInteraction() {
    }

    static void insertItemEntity(AirtightForgingPressStructuralBlockEntity structuralPart, ItemEntity itemEntity) {
        AirtightForgingPressBlockEntity press = structuralPart.getMasterBlockEntity();
        if (press == null) {
            return;
        }

        ItemStack remainingStack = ItemHandlerHelper.insertItemStacked(press.getInputInventory(), itemEntity.getItem().copy(), false);
        if (remainingStack.isEmpty()) {
            itemEntity.discard();
            return;
        }

        itemEntity.setItem(remainingStack);
    }

    static ItemInteractionResult getUseItemOnResult(AirtightForgingPressBlockEntity press, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        SmartInventory pressHeadInventory = press.getPressHeadInventory();
        if (stack.isEmpty()) {
            ItemStack pressHeadStack = pressHeadInventory.getStackInSlot(0);
            if (pressHeadStack.isEmpty()) {
                return ItemInteractionResult.SUCCESS;
            }

            ItemHandlerHelper.giveItemToPlayer(player, pressHeadStack);
            pressHeadInventory.setStackInSlot(0, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1 + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }

        if (!pressHeadInventory.isItemValid(0, stack)) {
            return ItemInteractionResult.CONSUME;
        }

        ItemStack remainingStack = pressHeadInventory.insertItem(0, stack, false);
        if (!ItemStack.matches(stack, remainingStack)) {
            player.setItemInHand(hand, remainingStack);
            AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, pressHeadInventory.getStackInSlot(0));
        pressHeadInventory.setStackInSlot(0, remainingStack);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1 + level.getRandom().nextFloat());
        return ItemInteractionResult.SUCCESS;
    }

    static ItemInteractionResult getUseItemOnResult(AirtightForgingPressStructuralBlockEntity structuralPart, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        AirtightForgingPressBlockEntity press = structuralPart.getMasterBlockEntity();
        if (press == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty()) {
            boolean returnedAnyItems = false;

            for (SmartInventory inventory : List.of(press.getInputInventory(), press.getOutputInventory())) {
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack storedStack = inventory.getStackInSlot(slot);
                    if (storedStack.isEmpty()) {
                        continue;
                    }

                    ItemHandlerHelper.giveItemToPlayer(player, storedStack);
                    inventory.setStackInSlot(slot, ItemStack.EMPTY);
                    returnedAnyItems = true;
                }
            }

            if (returnedAnyItems) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1 + level.getRandom().nextFloat());
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(AllItems.WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(AllBlocks.MECHANICAL_ARM.asItem())) {
            return ItemInteractionResult.CONSUME;
        }

        SmartInventory inputInventory = press.getInputInventory();
        ItemStack remainingStack = ItemHandlerHelper.insertItemStacked(inputInventory, stack, false);
        if (ItemStack.matches(stack, remainingStack)) {
            player.setItemInHand(hand, inputInventory.getStackInSlot(0));
            inputInventory.setStackInSlot(0, remainingStack);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1 + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, remainingStack);
        AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
        return ItemInteractionResult.SUCCESS;
    }

    static ItemInteractionResult getUseItemOnResult(AirtightForgingPressStructuralShaftBlockEntity shaftPart, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        AirtightForgingPressBlockEntity press = shaftPart.getMasterBlockEntity();
        if (press == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty()) {
            SmartInventory processingInventory = press.getAdditionInventory();
            ItemStack processingStack = processingInventory.getStackInSlot(0);
            if (processingStack.isEmpty()) {
                return ItemInteractionResult.SUCCESS;
            }

            ItemHandlerHelper.giveItemToPlayer(player, processingStack);
            processingInventory.setStackInSlot(0, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1 + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(AllItems.WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(AllBlocks.MECHANICAL_ARM.asItem())) {
            return ItemInteractionResult.CONSUME;
        }

        SmartInventory processingInventory = press.getAdditionInventory();
        ItemStack remainingStack = ItemHandlerHelper.insertItemStacked(processingInventory, stack, false);
        if (ItemStack.matches(stack, remainingStack)) {
            player.setItemInHand(hand, processingInventory.getStackInSlot(0));
            processingInventory.setStackInSlot(0, remainingStack);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1 + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, remainingStack);
        AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
        return ItemInteractionResult.SUCCESS;
    }

    static void updateRecipeFilter(AirtightForgingPressStructuralBlockEntity filterPart, ItemStack filterStack) {
        Level level = filterPart.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        AirtightForgingPressBlockEntity press = filterPart.getMasterBlockEntity();
        if (press == null) {
            return;
        }

        press.setRecipeFilter(filterStack);
    }
}
