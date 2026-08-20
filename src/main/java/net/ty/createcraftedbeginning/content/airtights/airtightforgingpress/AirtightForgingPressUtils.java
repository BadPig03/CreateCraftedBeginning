package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.recipe.RecipeApplier;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipeContext.ConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipeContext.OutputPlan;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrieFinder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressUtils {
    private static final Object FORGING_PRESS_RECIPE_CACHE_KEY = new Object();
    private static final Object AUTOMATIC_PRESSING_RECIPE_CACHE_KEY = new Object();
    private static final Object AUTOMATIC_SMITHING_RECIPE_CACHE_KEY = new Object();
    private static final int SMITHING_BASE_SLOT = 1;
    private static final int SMITHING_ADDITION_SLOT = 2;
    private static final AtomicLong RECIPE_CACHE_VERSION = new AtomicLong();

    private AirtightForgingPressUtils() {
    }

    public static BlockPos getMaster(BlockPos pos, BlockState state) {
        return switch (state.getBlock()) {
            case AirtightForgingPressStructuralBlock ignored -> pos.offset(state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).getMasterOffset());
            case AirtightForgingPressStructuralShaftBlock ignored -> pos.offset(state.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION).getMasterOffset());
            default -> pos;
        };
    }

    public static void invalidateRecipeCaches() {
        AirtightWithGasRecipeTrieFinder.invalidateFailures(FORGING_PRESS_RECIPE_CACHE_KEY);
        RECIPE_CACHE_VERSION.incrementAndGet();
    }

    public static boolean isAllowedAutomaticPressingRecipe(RecipeHolder<? extends Recipe<?>> holder) {
        return holder.value() instanceof PressingRecipe && !AllRecipeTypes.shouldIgnoreInAutomation(holder);
    }

    public static boolean isAllowedAutomaticSmithingRecipe(RecipeHolder<? extends Recipe<?>> holder) {
        return holder.value().getType() == RecipeType.SMITHING && holder.value() instanceof SmithingRecipe && !AllRecipeTypes.shouldIgnoreInAutomation(holder);
    }

    static Optional<ForgingPressRecipe> getMatchingRecipe(AirtightForgingPressBlockEntity press) {
        if (!press.hasRecipeInputs()) {
            return Optional.empty();
        }

        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        if (!AirtightWithGasRecipeTrieFinder.hasFailed(FORGING_PRESS_RECIPE_CACHE_KEY, level)) {
            Optional<ForgingPressRecipe> forgingRecipe = findMatchingTrieRecipe(press, level);
            if (forgingRecipe.isPresent()) {
                return forgingRecipe;
            }
        }
        return findMatchingLinearRecipe(press, level);
    }

    static long getRecipeCacheVersion() {
        return RECIPE_CACHE_VERSION.get();
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
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
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
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
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
            if (returnStoredItems(press, player)) {
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

        SmartInventory inputInventory = press.getInputInventory();
        ItemStack remainingStack = ItemHandlerHelper.insertItemStacked(inputInventory, stack, false);
        if (ItemStack.matches(stack, remainingStack)) {
            player.setItemInHand(hand, inputInventory.getStackInSlot(0));
            inputInventory.setStackInSlot(0, remainingStack);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
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
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
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
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
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

    static Optional<RecipeHolder<PressingRecipe>> getMatchingPressingRecipe(AirtightForgingPressBlockEntity press) {
        Level level = press.getLevel();
        if (level == null || !press.getPressHeadInventory().isEmpty() || !press.getAdditionInventory().isEmpty()) {
            return Optional.empty();
        }

        ItemStack inputStack = press.getInputInventory().getStackInSlot(0);
        if (inputStack.isEmpty()) {
            return Optional.empty();
        }

        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(AUTOMATIC_PRESSING_RECIPE_CACHE_KEY, level, AirtightForgingPressUtils::isAllowedAutomaticPressingRecipe)) {
            if (!(holder.value() instanceof PressingRecipe pressingRecipe) || !canApplyPressingRecipe(press, pressingRecipe, inputStack)) {
                continue;
            }

            return Optional.of(new RecipeHolder<>(holder.id(), pressingRecipe));
        }
        return Optional.empty();
    }

    static boolean applyPressingRecipe(AirtightForgingPressBlockEntity press, PressingRecipe recipe) {
        Level level = press.getLevel();
        if (level == null || !press.getPressHeadInventory().isEmpty() || !press.getAdditionInventory().isEmpty()) {
            return false;
        }

        IItemHandler inputInventory = press.getInputInventory();
        ItemStack inputStack = inputInventory.getStackInSlot(0);
        int batchSize = findLargestPressingBatch(press, recipe, inputStack);
        if (batchSize <= 0) {
            return false;
        }

        ItemStack batchInput = inputStack.copyWithCount(batchSize);
        List<ItemStack> outputStacks = RecipeApplier.applyRecipeOn(level, batchInput, recipe, true);
        Optional<OutputPlan> plannedOutput = press.planOutputs(outputStacks);
        if (plannedOutput.isEmpty()) {
            return false;
        }

        int[] fluidAmounts = new int[press.getFluidCapability().getTanks()];
        long[] gasAmounts = new long[press.getGasCapability().getTanks()];
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(ItemStack.EMPTY, 0, inputStack.copy(), batchSize, fluidAmounts, gasAmounts);
        return press.commitCraft(consumptionPlan, plannedOutput.get());
    }

    static Optional<RecipeHolder<SmithingRecipe>> getMatchingSmithingRecipe(AirtightForgingPressBlockEntity press) {
        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        SmithingRecipeInput smithingInput = createSmithingInput(press);
        if (smithingInput.template().isEmpty() || smithingInput.base().isEmpty() || smithingInput.addition().isEmpty()) {
            return Optional.empty();
        }

        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(AUTOMATIC_SMITHING_RECIPE_CACHE_KEY, level, AirtightForgingPressUtils::isAllowedAutomaticSmithingRecipe)) {
            if (!(holder.value() instanceof SmithingRecipe smithingRecipe) || !canApplySmithingRecipe(press, smithingRecipe, smithingInput)) {
                continue;
            }

            return Optional.of(new RecipeHolder<>(holder.id(), smithingRecipe));
        }
        return Optional.empty();
    }

    static boolean applySmithingRecipe(AirtightForgingPressBlockEntity press, SmithingRecipe recipe) {
        Level level = press.getLevel();
        if (level == null) {
            return false;
        }

        SmithingRecipeInput smithingInput = createSmithingInput(press);
        if (!canApplySmithingRecipe(press, recipe, smithingInput)) {
            return false;
        }

        List<ItemStack> outputStacks = getSmithingOutputs(recipe, smithingInput, level);
        Optional<OutputPlan> plannedOutput = press.planOutputs(outputStacks);
        if (plannedOutput.isEmpty()) {
            return false;
        }

        IItemHandler processingInventory = press.getAdditionInventory();
        IItemHandler inputInventory = press.getInputInventory();
        ItemStack simulatedProcessingStack = processingInventory.extractItem(0, 1, true);
        ItemStack simulatedInputStack = inputInventory.extractItem(0, 1, true);
        if (simulatedProcessingStack.getCount() != 1 || simulatedInputStack.getCount() != 1) {
            return false;
        }

        int[] fluidAmounts = new int[press.getFluidCapability().getTanks()];
        long[] gasAmounts = new long[press.getGasCapability().getTanks()];
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(simulatedProcessingStack, 1, simulatedInputStack, 1, fluidAmounts, gasAmounts);
        return press.commitCraft(consumptionPlan, plannedOutput.get());
    }

    static SmithingRecipeInput createSmithingInput(AirtightForgingPressBlockEntity press) {
        ItemStack templateStack = press.getPressHeadInventory().getStackInSlot(0).copy();
        ItemStack additionStack = press.getAdditionInventory().getStackInSlot(0).copy();
        ItemStack baseStack = press.getInputInventory().getStackInSlot(0).copy();
        return new SmithingRecipeInput(templateStack, baseStack, additionStack);
    }

    private static Optional<ForgingPressRecipe> findMatchingTrieRecipe(AirtightForgingPressBlockEntity press, Level level) {
        try {
            IItemHandler availableItems = press.getRecipeInputCapability();
            IFluidHandler availableFluids = press.getFluidCapability();
            IGasHandler availableGases = press.getGasCapability();
            AirtightWithGasRecipeTrie<?> recipeTrie = AirtightWithGasRecipeTrieFinder.get(FORGING_PRESS_RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof ForgingPressRecipe);
            Set<AbstractVariant> availableVariants = AirtightWithGasRecipeTrie.getVariants(availableItems, availableFluids, availableGases);
            for (Recipe<?> candidateRecipe : recipeTrie.lookup(availableVariants)) {
                if (candidateRecipe instanceof ForgingPressRecipe forgingRecipe && ForgingPressRecipe.match(press, forgingRecipe)) {
                    return Optional.of(forgingRecipe);
                }
            }
        } catch (ExecutionException | UncheckedExecutionException exception) {
            if (AirtightWithGasRecipeTrieFinder.recordFailure(FORGING_PRESS_RECIPE_CACHE_KEY, level)) {
                CCBAPI.LOGGER.error("Failed to build the airtight forging press recipe trie; falling back to a linear recipe search until recipes are reloaded", exception);
            }
        }
        return Optional.empty();
    }

    private static Optional<ForgingPressRecipe> findMatchingLinearRecipe(AirtightForgingPressBlockEntity press, Level level) {
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(FORGING_PRESS_RECIPE_CACHE_KEY, level, recipeHolder -> recipeHolder.value() instanceof ForgingPressRecipe)) {
            if (holder.value() instanceof ForgingPressRecipe forgingRecipe && ForgingPressRecipe.match(press, forgingRecipe)) {
                return Optional.of(forgingRecipe);
            }
        }
        return Optional.empty();
    }

    private static boolean returnStoredItems(AirtightForgingPressBlockEntity press, Player player) {
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
        return returnedAnyItems;
    }

    private static boolean canApplyPressingRecipe(AirtightForgingPressBlockEntity press, PressingRecipe recipe, ItemStack inputStack) {
        return findLargestPressingBatch(press, recipe, inputStack) > 0;
    }

    private static int findLargestPressingBatch(AirtightForgingPressBlockEntity press, PressingRecipe recipe, ItemStack inputStack) {
        Level level = press.getLevel();
        if (level == null || inputStack.isEmpty() || !recipe.matches(new SingleRecipeInput(inputStack), level)) {
            return 0;
        }

        List<ItemStack> singleCraftOutputs = getPotentialPressingOutputs(recipe, inputStack, 1);
        if (singleCraftOutputs.isEmpty() || !press.testRecipeFilter(singleCraftOutputs.getFirst())) {
            return 0;
        }

        int lowerBound = 1;
        int upperBound = inputStack.getCount();
        int largestBatch = 0;
        while (lowerBound <= upperBound) {
            int batchSize = lowerBound + upperBound >>> 1;
            boolean canFitOutputs = press.acceptOutputs(getPotentialPressingOutputs(recipe, inputStack, batchSize), true);
            if (!canFitOutputs) {
                upperBound = batchSize - 1;
                continue;
            }

            largestBatch = batchSize;
            lowerBound = batchSize + 1;
        }
        return largestBatch;
    }

    private static List<ItemStack> getPotentialPressingOutputs(PressingRecipe recipe, ItemStack inputStack, int craftCount) {
        List<ItemStack> outputStacks = new ArrayList<>();
        for (ProcessingOutput processingOutput : recipe.getRollableResults()) {
            ItemStack outputStack = processingOutput.getStack();
            if (outputStack.isEmpty()) {
                continue;
            }

            for (int craft = 0; craft < craftCount; craft++) {
                outputStacks.add(outputStack.copy());
            }
        }
        if (!inputStack.hasCraftingRemainingItem()) {
            return outputStacks;
        }

        ItemStack craftingRemainder = inputStack.getCraftingRemainingItem();
        if (craftingRemainder.isEmpty()) {
            return outputStacks;
        }

        for (int craft = 0; craft < craftCount; craft++) {
            outputStacks.add(craftingRemainder.copy());
        }
        return outputStacks;
    }

    private static boolean canApplySmithingRecipe(AirtightForgingPressBlockEntity press, SmithingRecipe recipe, SmithingRecipeInput smithingInput) {
        Level level = press.getLevel();
        if (level == null || !recipe.matches(smithingInput, level)) {
            return false;
        }

        List<ItemStack> outputStacks = getSmithingOutputs(recipe, smithingInput, level);
        return !outputStacks.isEmpty() && press.testRecipeFilter(outputStacks.getFirst()) && press.acceptOutputs(outputStacks, true);
    }

    private static List<ItemStack> getSmithingOutputs(SmithingRecipe recipe, SmithingRecipeInput smithingInput, Level level) {
        List<ItemStack> outputStacks = new ArrayList<>();
        ItemStack smithingResult = recipe.assemble(smithingInput, level.registryAccess());
        if (smithingResult.isEmpty()) {
            return outputStacks;
        }

        outputStacks.add(smithingResult.copy());
        NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(smithingInput);
        addConsumedSlotRemainder(outputStacks, remainingItems, SMITHING_BASE_SLOT);
        addConsumedSlotRemainder(outputStacks, remainingItems, SMITHING_ADDITION_SLOT);
        return outputStacks;
    }

    private static void addConsumedSlotRemainder(List<ItemStack> outputStacks, NonNullList<ItemStack> remainingItems, int slot) {
        if (slot < 0 || slot >= remainingItems.size()) {
            return;
        }

        ItemStack remainingStack = remainingItems.get(slot);
        if (remainingStack.isEmpty()) {
            return;
        }

        outputStacks.add(remainingStack.copy());
    }
}
