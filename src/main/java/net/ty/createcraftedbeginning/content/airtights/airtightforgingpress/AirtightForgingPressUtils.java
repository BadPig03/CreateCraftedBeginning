package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrieFinder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressUtils {
    private AirtightForgingPressUtils() {
    }

    public static BlockPos getMaster(BlockPos pos, BlockState state) {
        return switch (state.getBlock()) {
            case AirtightForgingPressStructuralBlock ignored -> pos.offset(state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).getPosition());
            case AirtightForgingPressStructuralShaftBlock ignored -> pos.offset(state.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION).getPosition());
            default -> pos;
        };
    }

    public static List<ForgingPressRecipe> getMatchingRecipes(AirtightForgingPressBlockEntity press, Object cacheObject) {
        List<ForgingPressRecipe> list = new ArrayList<>();
        if (press.isEmpty()) {
            return list;
        }

        Level level = press.getLevel();
        if (level == null) {
            return list;
        }

        try {
            IItemHandler availableItems = press.getItemCapability();
            IFluidHandler availableFluids = press.getFluidCapability();
            IGasHandler availableGases = press.getGasCapability();

            AirtightWithGasRecipeTrie<?> trie = AirtightWithGasRecipeTrieFinder.get(cacheObject, level, holder -> holder.value() instanceof ForgingPressRecipe);
            Set<AbstractVariant> availableVariants = AirtightWithGasRecipeTrie.getVariants(availableItems, availableFluids, availableGases);
            for (Recipe<?> r : trie.lookup(availableVariants)) {
                if (!(r instanceof ForgingPressRecipe pressRecipe) || !ForgingPressRecipe.match(press, pressRecipe)) {
                    continue;
                }

                list.add(pressRecipe);
            }
        } catch (Exception e) {
            CreateCraftedBeginning.LOGGER.error("Failed to get recipe trie, falling back to slow logic: ", e);
            list.clear();
            for (RecipeHolder<? extends Recipe<?>> r : RecipeFinder.get(cacheObject, level, holder -> holder.value() instanceof ForgingPressRecipe)) {
                if (!(r.value() instanceof ForgingPressRecipe pressRecipe) || !ForgingPressRecipe.match(press, pressRecipe)) {
                    continue;
                }

                list.add(pressRecipe);
            }
        }
        return list;
    }

    public static void insertItemEntity(AirtightForgingPressStructuralBlockEntity structural, ItemEntity itemEntity) {
        AirtightForgingPressBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return;
        }

        ItemStack insertItem = ItemHandlerHelper.insertItemStacked(master.getInputOutputInventories().getFirst(), itemEntity.getItem().copy(), false);
        if (insertItem.isEmpty()) {
            itemEntity.discard();
            return;
        }

        itemEntity.setItem(insertItem);
    }

    public static ItemInteractionResult getUseItemOnResult(AirtightForgingPressBlockEntity press, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        SmartInventory inventory = press.getProcessingInventories().getFirst();
        if (stack.isEmpty()) {
            ItemStack item = inventory.getStackInSlot(0);
            if (item.isEmpty()) {
                return ItemInteractionResult.SUCCESS;
            }

            ItemHandlerHelper.giveItemToPlayer(player, item);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }
        else if (!inventory.isItemValid(0, stack)) {
            return ItemInteractionResult.CONSUME;
        }

        ItemStack remainder = inventory.insertItem(0, stack, false);
        if (!ItemStack.matches(stack, remainder)) {
            player.setItemInHand(hand, remainder);
            AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, inventory.getStackInSlot(0));
        inventory.setStackInSlot(0, remainder);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f + level.getRandom().nextFloat());
        return ItemInteractionResult.SUCCESS;
    }

    public static ItemInteractionResult getUseItemOnResult(AirtightForgingPressStructuralBlockEntity structural, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        AirtightForgingPressBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty()) {
            boolean success = false;
            IItemHandlerModifiable inventory = master.getInputOutputCapability();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stackInSlot = inventory.getStackInSlot(slot);
                if (stackInSlot.isEmpty()) {
                    continue;
                }

                ItemHandlerHelper.giveItemToPlayer(player, stackInSlot);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                success = true;
            }
            if (success) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f + level.getRandom().nextFloat());
            }
            return ItemInteractionResult.SUCCESS;
        }
        else if (stack.is(AllItems.WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else if (stack.is(AllBlocks.MECHANICAL_ARM.asItem())) {
            return ItemInteractionResult.CONSUME;
        }

        SmartInventory inputInventory = master.getInputOutputInventories().getFirst();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(inputInventory, stack, false);
        if (ItemStack.matches(stack, remainder)) {
            player.setItemInHand(hand, inputInventory.getStackInSlot(0));
            inputInventory.setStackInSlot(0, remainder);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, remainder);
        AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
        return ItemInteractionResult.SUCCESS;
    }

    public static ItemInteractionResult getUseItemOnResult(AirtightForgingPressStructuralShaftBlockEntity structural, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        AirtightForgingPressBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty()) {
            SmartInventory inventory = master.getProcessingInventories().getSecond();
            ItemStack stackInSlot = inventory.getStackInSlot(0);
            if (stackInSlot.isEmpty()) {
                return ItemInteractionResult.SUCCESS;
            }

            ItemHandlerHelper.giveItemToPlayer(player, stackInSlot);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }
        else if (stack.is(AllItems.WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else if (stack.is(AllBlocks.MECHANICAL_ARM.asItem())) {
            return ItemInteractionResult.CONSUME;
        }

        SmartInventory processingInventory = master.getProcessingInventories().getSecond();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(processingInventory, stack, false);
        if (ItemStack.matches(stack, remainder)) {
            player.setItemInHand(hand, processingInventory.getStackInSlot(0));
            processingInventory.setStackInSlot(0, remainder);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, remainder);
        AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
        return ItemInteractionResult.SUCCESS;
    }

    public static void refreshOtherFilters(AirtightForgingPressStructuralBlockEntity structural, ItemStack stack) {
        AirtightForgingPressBlockEntity master = structural.getMasterBlockEntity();
        if (master == null || master.isFilterChanged()) {
            return;
        }

        Level level = structural.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        master.notifyFiltersChanged();
        master.notifyContentsChanged();
        BlockPos centerPos = master.getBlockPos().below();
        for (Direction direction : Iterate.horizontalDirections) {
            BlockPos filterPos = centerPos.relative(direction);
            if (filterPos.equals(structural.getBlockPos())) {
                continue;
            }
            if (!(level.getBlockEntity(filterPos) instanceof AirtightForgingPressStructuralBlockEntity structuralBlockEntity)) {
                continue;
            }

            structuralBlockEntity.getFilteringBehaviour().setFilter(stack);
        }
    }

    public static boolean canModifyFilter(AirtightForgingPressStructuralBlockEntity structural) {
        AirtightForgingPressBlockEntity master = structural.getMasterBlockEntity();
        return master != null && !master.isFilterChanged();
    }

    public static Optional<RecipeHolder<SmithingRecipe>> getMatchingSmithingRecipe(AirtightForgingPressBlockEntity press) {
        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        SmithingRecipeInput input = createSmithingInput(press);
        if (input.template().isEmpty() || input.base().isEmpty() || input.addition().isEmpty()) {
            return Optional.empty();
        }

        return level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level).filter(holder -> canApplySmithingRecipe(press, holder.value(), input));
    }

    public static boolean applySmithingRecipe(AirtightForgingPressBlockEntity press, SmithingRecipe recipe) {
        Level level = press.getLevel();
        if (level == null) {
            return false;
        }

        SmithingRecipeInput input = createSmithingInput(press);
        if (!canApplySmithingRecipe(press, recipe, input)) {
            return false;
        }

        List<ItemStack> outputs = getSmithingOutputs(recipe, input, level);
        if (!press.acceptOutputs(outputs, true)) {
            return false;
        }

        press.getProcessingInventories().getSecond().extractItem(0, 1, false);
        press.getInputOutputInventories().getFirst().extractItem(0, 1, false);
        return press.acceptOutputs(outputs, false);
    }

    public static SmithingRecipeInput createSmithingInput(AirtightForgingPressBlockEntity press) {
        ItemStack template = press.getProcessingInventories().getFirst().getStackInSlot(0).copy();
        ItemStack addition = press.getProcessingInventories().getSecond().getStackInSlot(0).copy();
        ItemStack base = press.getInputOutputInventories().getFirst().getStackInSlot(0).copy();
        return new SmithingRecipeInput(template, base, addition);
    }

    private static boolean canApplySmithingRecipe(AirtightForgingPressBlockEntity press, SmithingRecipe recipe, SmithingRecipeInput input) {
        Level level = press.getLevel();
        if (level == null || !recipe.matches(input, level)) {
            return false;
        }

        List<ItemStack> outputs = getSmithingOutputs(recipe, input, level);
        if (outputs.isEmpty()) {
            return false;
        }

        FilteringBehaviour filter = press.getFilteringBehaviour();
        return filter != null && filter.test(outputs.getFirst()) && press.acceptOutputs(outputs, true);
    }

    private static List<ItemStack> getSmithingOutputs(SmithingRecipe recipe, SmithingRecipeInput input, Level level) {
        List<ItemStack> outputs = new ArrayList<>();
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            return outputs;
        }

        outputs.add(result.copy());
        NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(input);
        for (ItemStack remaining : remainingItems) {
            if (!remaining.isEmpty()) {
                outputs.add(remaining.copy());
            }
        }

        return outputs;
    }
}
