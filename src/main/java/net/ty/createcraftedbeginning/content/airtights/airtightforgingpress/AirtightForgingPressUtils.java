package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity.ConsumptionPlan;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity.OutputPlan;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrieFinder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressUtils {
    private static final Object FORGING_RECIPE_CACHE_KEY = new Object();
    private static final int SMITHING_BASE_SLOT = 1;
    private static final int SMITHING_ADDITION_SLOT = 2;
    private static final AtomicBoolean RECIPE_TRIE_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicLong RECIPE_CACHE_EPOCH = new AtomicLong();
    private static volatile boolean recipeTrieDisabled;

    private AirtightForgingPressUtils() {
    }

    public static BlockPos getMaster(BlockPos pos, BlockState state) {
        return switch (state.getBlock()) {
            case AirtightForgingPressStructuralBlock ignored -> pos.offset(state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).getMasterOffset());
            case AirtightForgingPressStructuralShaftBlock ignored -> pos.offset(state.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION).getMasterOffset());
            default -> pos;
        };
    }

    public static Optional<ForgingPressRecipe> getMatchingRecipe(AirtightForgingPressBlockEntity press) {
        if (!press.hasRecipeInputs()) {
            return Optional.empty();
        }

        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        if (!recipeTrieDisabled) {
            Optional<ForgingPressRecipe> recipe = findMatchingTrieRecipe(press, level);
            if (recipe.isPresent()) {
                return recipe;
            }
        }

        return findMatchingLinearRecipe(press, level);
    }

    private static Optional<ForgingPressRecipe> findMatchingTrieRecipe(AirtightForgingPressBlockEntity press, Level level) {
        try {
            IItemHandler availableItems = press.getRecipeInputCapability();
            IFluidHandler availableFluids = press.getFluidCapability();
            IGasHandler availableGases = press.getGasCapability();
            AirtightWithGasRecipeTrie<?> trie = AirtightWithGasRecipeTrieFinder.get(FORGING_RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof ForgingPressRecipe);
            Set<AbstractVariant> availableVariants = AirtightWithGasRecipeTrie.getVariants(availableItems, availableFluids, availableGases);
            for (Recipe<?> candidate : trie.lookup(availableVariants)) {
                if (candidate instanceof ForgingPressRecipe recipe && ForgingPressRecipe.match(press, recipe)) {
                    return Optional.of(recipe);
                }
            }
        } catch (ExecutionException | UncheckedExecutionException e) {
            recipeTrieDisabled = true;
            if (RECIPE_TRIE_FAILURE_LOGGED.compareAndSet(false, true)) {
                CreateCraftedBeginning.LOGGER.error("Failed to build the airtight forging press recipe trie; falling back to a linear recipe search until recipes are reloaded", e);
            }
        }

        return Optional.empty();
    }

    private static Optional<ForgingPressRecipe> findMatchingLinearRecipe(AirtightForgingPressBlockEntity press, Level level) {
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(FORGING_RECIPE_CACHE_KEY, level, recipe -> recipe.value() instanceof ForgingPressRecipe)) {
            if (holder.value() instanceof ForgingPressRecipe recipe && ForgingPressRecipe.match(press, recipe)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static void invalidateRecipeCaches() {
        recipeTrieDisabled = false;
        RECIPE_TRIE_FAILURE_LOGGED.set(false);
        RECIPE_CACHE_EPOCH.incrementAndGet();
    }

    public static long getRecipeCacheEpoch() {
        return RECIPE_CACHE_EPOCH.get();
    }

    public static void insertItemEntity(AirtightForgingPressStructuralBlockEntity structural, ItemEntity itemEntity) {
        AirtightForgingPressBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return;
        }

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(master.getInputInventory(), itemEntity.getItem().copy(), false);
        if (remainder.isEmpty()) {
            itemEntity.discard();
            return;
        }

        itemEntity.setItem(remainder);
    }

    public static ItemInteractionResult getUseItemOnResult(AirtightForgingPressBlockEntity press, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        SmartInventory inventory = press.getPressHeadInventory();
        if (stack.isEmpty()) {
            ItemStack item = inventory.getStackInSlot(0);
            if (item.isEmpty()) {
                return ItemInteractionResult.SUCCESS;
            }

            ItemHandlerHelper.giveItemToPlayer(player, item);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }
        if (!inventory.isItemValid(0, stack)) {
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
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
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
            if (returnStoredItems(master, player)) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(AllItems.WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(AllBlocks.MECHANICAL_ARM.asItem())) {
            return ItemInteractionResult.CONSUME;
        }

        SmartInventory inputInventory = master.getInputInventory();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(inputInventory, stack, false);
        if (ItemStack.matches(stack, remainder)) {
            player.setItemInHand(hand, inputInventory.getStackInSlot(0));
            inputInventory.setStackInSlot(0, remainder);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, remainder);
        AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
        return ItemInteractionResult.SUCCESS;
    }

    private static boolean returnStoredItems(AirtightForgingPressBlockEntity press, Player player) {
        boolean returnedAny = false;
        for (SmartInventory inventory : List.of(press.getInputInventory(), press.getOutputInventory())) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }

                ItemHandlerHelper.giveItemToPlayer(player, stack);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                returnedAny = true;
            }
        }
        return returnedAny;
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
            SmartInventory inventory = master.getAdditionInventory();
            ItemStack stackInSlot = inventory.getStackInSlot(0);
            if (stackInSlot.isEmpty()) {
                return ItemInteractionResult.SUCCESS;
            }

            ItemHandlerHelper.giveItemToPlayer(player, stackInSlot);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(AllItems.WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(AllBlocks.MECHANICAL_ARM.asItem())) {
            return ItemInteractionResult.CONSUME;
        }

        SmartInventory processingInventory = master.getAdditionInventory();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(processingInventory, stack, false);
        if (ItemStack.matches(stack, remainder)) {
            player.setItemInHand(hand, processingInventory.getStackInSlot(0));
            processingInventory.setStackInSlot(0, remainder);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, remainder);
        AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
        return ItemInteractionResult.SUCCESS;
    }

    public static void updateRecipeFilter(AirtightForgingPressStructuralBlockEntity structural, ItemStack stack) {
        Level level = structural.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        AirtightForgingPressBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return;
        }

        master.setRecipeFilter(stack);
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
        Optional<OutputPlan> plannedOutput = press.planOutputs(outputs);
        if (plannedOutput.isEmpty()) {
            return false;
        }

        IItemHandler processingInventory = press.getAdditionInventory();
        IItemHandler inputInventory = press.getInputInventory();
        ItemStack simulatedProcessing = processingInventory.extractItem(0, 1, true);
        ItemStack simulatedInput = inputInventory.extractItem(0, 1, true);
        if (simulatedProcessing.getCount() != 1 || simulatedInput.getCount() != 1) {
            return false;
        }

        int[] fluidAmounts = new int[press.getFluidCapability().getTanks()];
        long[] gasAmounts = new long[press.getGasCapability().getTanks()];
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(simulatedProcessing, 1, simulatedInput, 1, fluidAmounts, gasAmounts);
        return press.commitCraft(consumptionPlan, plannedOutput.get());
    }

    public static SmithingRecipeInput createSmithingInput(AirtightForgingPressBlockEntity press) {
        ItemStack template = press.getPressHeadInventory().getStackInSlot(0).copy();
        ItemStack processing = press.getAdditionInventory().getStackInSlot(0).copy();
        ItemStack input = press.getInputInventory().getStackInSlot(0).copy();
        return new SmithingRecipeInput(template, input, processing);
    }

    private static boolean canApplySmithingRecipe(AirtightForgingPressBlockEntity press, SmithingRecipe recipe, SmithingRecipeInput input) {
        Level level = press.getLevel();
        if (level == null || !recipe.matches(input, level)) {
            return false;
        }

        List<ItemStack> outputs = getSmithingOutputs(recipe, input, level);
        return !outputs.isEmpty() && press.testRecipeFilter(outputs.getFirst()) && press.acceptOutputs(outputs, true);
    }

    private static List<ItemStack> getSmithingOutputs(SmithingRecipe recipe, SmithingRecipeInput input, Level level) {
        List<ItemStack> outputs = new ArrayList<>();
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            return outputs;
        }

        outputs.add(result.copy());
        NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(input);
        addConsumedSlotRemainder(outputs, remainingItems, SMITHING_BASE_SLOT);
        addConsumedSlotRemainder(outputs, remainingItems, SMITHING_ADDITION_SLOT);
        return outputs;
    }

    private static void addConsumedSlotRemainder(List<ItemStack> outputs, NonNullList<ItemStack> remainingItems, int slot) {
        if (slot < 0 || slot >= remainingItems.size()) {
            return;
        }

        ItemStack remaining = remainingItems.get(slot);
        if (remaining.isEmpty()) {
            return;
        }

        outputs.add(remaining.copy());
    }
}
