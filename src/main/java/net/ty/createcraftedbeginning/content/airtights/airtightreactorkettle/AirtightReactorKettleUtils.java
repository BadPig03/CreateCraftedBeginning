package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity.CraftPlan;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrieFinder;
import net.ty.createcraftedbeginning.registry.CCBDamageSources;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleUtils {
    private static final Object CRAFTING_RECIPE_CACHE_KEY = new Object();
    private static final Object REACTOR_KETTLE_RECIPE_CACHE_KEY = new Object();
    private static final AtomicLong RECIPE_CACHE_VERSION = new AtomicLong();

    private AirtightReactorKettleUtils() {
    }

    public static BlockPos getMaster(BlockPos pos, BlockState state) {
        return switch (state.getBlock()) {
            case AirtightReactorKettleStructuralBlock ignored -> pos.offset(state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).getPosition());
            case AirtightReactorKettleStructuralCogBlock ignored -> pos.offset(state.getValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION).getPosition());
            default -> pos;
        };
    }

    public static Optional<ReactorKettleRecipe> getMatchingRecipe(AirtightReactorKettleBlockEntity kettle) {
        if (kettle.isEmpty()) {
            return Optional.empty();
        }

        Level level = kettle.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        if (!AirtightWithGasRecipeTrieFinder.hasFailed(REACTOR_KETTLE_RECIPE_CACHE_KEY, level)) {
            Optional<ReactorKettleRecipe> recipe = findMatchingTrieRecipe(kettle, level);
            if (recipe.isPresent()) {
                return recipe;
            }
        }
        return findMatchingLinearRecipe(kettle, level);
    }

    private static Optional<ReactorKettleRecipe> findMatchingTrieRecipe(AirtightReactorKettleBlockEntity kettle, Level level) {
        try {
            IItemHandler availableItems = kettle.getAvailableItems();
            IFluidHandler availableFluids = kettle.getAvailableFluids();
            IGasHandler availableGases = kettle.getAvailableGases();
            AirtightWithGasRecipeTrie<?> trie = AirtightWithGasRecipeTrieFinder.get(REACTOR_KETTLE_RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof ReactorKettleRecipe);
            Set<AbstractVariant> availableVariants = AirtightWithGasRecipeTrie.getVariants(availableItems, availableFluids, availableGases);
            ReactorKettleRecipe compatibleMatch = null;
            int compatibleMatchPriority = 0;
            for (Recipe<?> candidate : trie.lookup(availableVariants)) {
                if (!(candidate instanceof ReactorKettleRecipe recipe) || !ReactorKettleRecipe.match(kettle, recipe)) {
                    continue;
                }
                if (ReactorKettleRecipe.isExactTemperatureMatch(kettle, recipe)) {
                    return Optional.of(recipe);
                }

                int matchPriority = ReactorKettleRecipe.getTemperatureMatchPriority(kettle, recipe);
                if (matchPriority > compatibleMatchPriority) {
                    compatibleMatch = recipe;
                    compatibleMatchPriority = matchPriority;
                }
            }
            return Optional.ofNullable(compatibleMatch);
        } catch (ExecutionException | UncheckedExecutionException exception) {
            if (AirtightWithGasRecipeTrieFinder.recordFailure(REACTOR_KETTLE_RECIPE_CACHE_KEY, level)) {
                CCBAPI.LOGGER.error("Failed to build the reactor kettle recipe trie; falling back to a linear recipe search", exception);
            }
        }
        return Optional.empty();
    }

    private static Optional<ReactorKettleRecipe> findMatchingLinearRecipe(AirtightReactorKettleBlockEntity kettle, Level level) {
        ReactorKettleRecipe compatibleMatch = null;
        int compatibleMatchPriority = 0;
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(REACTOR_KETTLE_RECIPE_CACHE_KEY, level, recipe -> recipe.value() instanceof ReactorKettleRecipe)) {
            if (!(holder.value() instanceof ReactorKettleRecipe recipe) || !ReactorKettleRecipe.match(kettle, recipe)) {
                continue;
            }
            if (ReactorKettleRecipe.isExactTemperatureMatch(kettle, recipe)) {
                return Optional.of(recipe);
            }

            int matchPriority = ReactorKettleRecipe.getTemperatureMatchPriority(kettle, recipe);
            if (matchPriority > compatibleMatchPriority) {
                compatibleMatch = recipe;
                compatibleMatchPriority = matchPriority;
            }
        }
        return Optional.ofNullable(compatibleMatch);
    }

    public static long getRecipeCacheVersion() {
        return RECIPE_CACHE_VERSION.get();
    }

    public static void invalidateRecipeCaches() {
        AirtightWithGasRecipeTrieFinder.invalidateFailures(REACTOR_KETTLE_RECIPE_CACHE_KEY);
        RECIPE_CACHE_VERSION.incrementAndGet();
    }

    public static float getTotalFluidUnits(SmartFluidTankBehaviour inputTank, SmartFluidTankBehaviour outputTank, float partialTicks) {
        return getFluidUnits(inputTank, partialTicks) + getFluidUnits(outputTank, partialTicks);
    }

    public static int getTotalFluidCapacity(SmartFluidTankBehaviour inputTank, SmartFluidTankBehaviour outputTank) {
        return getFluidCapacity(inputTank) + getFluidCapacity(outputTank);
    }

    private static int getFluidCapacity(SmartFluidTankBehaviour tankBehaviour) {
        IFluidHandler fluidHandler = tankBehaviour.getCapability();
        int capacity = 0;
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            capacity += fluidHandler.getTankCapacity(tank);
        }
        return capacity;
    }

    private static float getFluidUnits(SmartFluidTankBehaviour tankBehaviour, float partialTicks) {
        float totalUnits = 0;
        for (TankSegment tankSegment : tankBehaviour.getTanks()) {
            if (tankSegment.getRenderedFluid().isEmpty()) {
                continue;
            }

            float renderedUnits = tankSegment.getTotalUnits(partialTicks);
            if (renderedUnits >= 1) {
                totalUnits += renderedUnits;
            }
        }
        return totalUnits;
    }

    public static void insertItemEntity(AirtightReactorKettleStructuralBlockEntity structural, ItemEntity itemEntity) {
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

    public static void hurtInsideLivingEntities(AirtightReactorKettleStructuralBlockEntity structural, LivingEntity livingEntity) {
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

    public static ItemInteractionResult getUseItemOnResult(AirtightReactorKettleStructuralBlockEntity structural, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        AirtightReactorKettleBlockEntity kettle = structural.getMasterBlockEntity();
        if (kettle == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty()) {
            extractStoredItems(kettle, level, player, pos);
            return ItemInteractionResult.SUCCESS;
        }

        return transferFluidContainer(kettle, level, player, hand, stack);
    }

    private static ItemInteractionResult transferFluidContainer(AirtightReactorKettleBlockEntity kettle, Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) {
            boolean canEmptyContainer = GenericItemEmptying.canItemBeEmptied(level, stack);
            boolean canFillContainer = GenericItemFilling.canItemBeFilled(level, stack);
            return canEmptyContainer || canFillContainer ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, kettle) || GenericItemEmptying.canItemBeEmptied(level, stack)) {
            return ItemInteractionResult.SUCCESS;
        }

        if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, kettle) || GenericItemFilling.canItemBeFilled(level, stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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

        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
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

    public static void updateRecipeFilter(AirtightReactorKettleStructuralBlockEntity structural, ItemStack stack) {
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

    public static Optional<RecipeHolder<CraftingRecipe>> getMatchingCraftingRecipe(AirtightReactorKettleBlockEntity kettle) {
        Level level = kettle.getLevel();
        if (level == null || kettle.getInventories().getFirst().isEmpty()) {
            return Optional.empty();
        }

        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(CRAFTING_RECIPE_CACHE_KEY, level, AirtightReactorKettleUtils::isAllowedAutomaticMixingRecipe)) {
            if (!(holder.value() instanceof CraftingRecipe craftingRecipe) || !canResultPassTest(kettle, craftingRecipe) || !canApplyCraftingRecipe(kettle, craftingRecipe)) {
                continue;
            }

            return Optional.of(new RecipeHolder<>(holder.id(), craftingRecipe));
        }
        return Optional.empty();
    }

    public static boolean matchCraftingRecipe(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        return canApplyCraftingRecipe(kettle, recipe);
    }

    public static boolean applyCraftingRecipe(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        Level level = kettle.getLevel();
        IItemHandler inputInventory = kettle.getInventories().getFirst();
        if (level == null || !(recipe instanceof ShapelessRecipe)) {
            return false;
        }

        int[] itemConsumptionBySlot = new int[inputInventory.getSlots()];
        List<ItemStack> craftingInputStacks = new ArrayList<>();
        if (!planCraftingInputConsumption(recipe, inputInventory, itemConsumptionBySlot, craftingInputStacks)) {
            return false;
        }

        CraftingInput craftingInput = createCraftingInput(craftingInputStacks);
        if (!recipe.matches(craftingInput, level)) {
            return false;
        }

        List<ItemStack> craftingOutputs = getCraftingOutputs(recipe, craftingInput, level);
        if (!canApplyCraftingRecipe(kettle, recipe, craftingInput, craftingOutputs)) {
            return false;
        }

        int[] itemConsumptionAmounts = new int[kettle.getAvailableItems().getSlots()];
        System.arraycopy(itemConsumptionBySlot, 0, itemConsumptionAmounts, 0, itemConsumptionBySlot.length);
        CraftPlan craftPlan = kettle.createCraftPlan(itemConsumptionAmounts, new int[kettle.getAvailableFluids().getTanks()], new long[kettle.getAvailableGases().getTanks()], craftingOutputs, List.of(), List.of());
        return kettle.commitCraft(craftPlan);
    }

    private static boolean canApplyCraftingRecipe(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        Level level = kettle.getLevel();
        IItemHandler inputInventory = kettle.getInventories().getFirst();
        if (level == null || !(recipe instanceof ShapelessRecipe)) {
            return false;
        }

        int[] itemConsumptionBySlot = new int[inputInventory.getSlots()];
        List<ItemStack> craftingInputStacks = new ArrayList<>();
        if (!planCraftingInputConsumption(recipe, inputInventory, itemConsumptionBySlot, craftingInputStacks)) {
            return false;
        }

        CraftingInput craftingInput = createCraftingInput(craftingInputStacks);
        if (!recipe.matches(craftingInput, level)) {
            return false;
        }

        List<ItemStack> craftingOutputs = getCraftingOutputs(recipe, craftingInput, level);
        return canApplyCraftingRecipe(kettle, recipe, craftingInput, craftingOutputs);
    }

    private static boolean canApplyCraftingRecipe(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe, CraftingInput craftingInput, List<ItemStack> craftingOutputs) {
        Level level = kettle.getLevel();
        return level != null && recipe.matches(craftingInput, level) && !craftingOutputs.isEmpty() && kettle.testRecipeFilter(craftingOutputs.getFirst()) && kettle.acceptOutputs(craftingOutputs, new ArrayList<>(), new ArrayList<>(), true);
    }

    private static boolean planCraftingInputConsumption(CraftingRecipe recipe, IItemHandler inputInventory, int[] itemConsumptionBySlot, List<ItemStack> craftingInputStacks) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }

            ingredients.add(ingredient);
        }
        if (ingredients.isEmpty() || ingredients.size() > 9) {
            return false;
        }

        ingredients.sort(Comparator.comparingInt(ingredient -> getMatchingItemCount(inputInventory, ingredient)));
        return planCraftingInputConsumption(ingredients, 0, inputInventory, itemConsumptionBySlot, craftingInputStacks);
    }

    private static boolean planCraftingInputConsumption(List<Ingredient> ingredients, int ingredientIndex, IItemHandler inputInventory, int[] itemConsumptionBySlot, List<ItemStack> craftingInputStacks) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
            ItemStack storedStack = inputInventory.getStackInSlot(slot);
            if (storedStack.isEmpty() || storedStack.getCount() <= itemConsumptionBySlot[slot] || !ingredient.test(storedStack)) {
                continue;
            }

            itemConsumptionBySlot[slot]++;
            craftingInputStacks.add(storedStack.copyWithCount(1));
            if (planCraftingInputConsumption(ingredients, ingredientIndex + 1, inputInventory, itemConsumptionBySlot, craftingInputStacks)) {
                return true;
            }

            craftingInputStacks.removeLast();
            itemConsumptionBySlot[slot]--;
        }
        return false;
    }

    private static int getMatchingItemCount(IItemHandler inputInventory, Ingredient ingredient) {
        int matchingItemCount = 0;
        for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
            ItemStack storedStack = inputInventory.getStackInSlot(slot);
            if (storedStack.isEmpty() || !ingredient.test(storedStack)) {
                continue;
            }

            matchingItemCount += storedStack.getCount();
        }
        return matchingItemCount;
    }

    private static CraftingInput createCraftingInput(List<ItemStack> craftingInputStacks) {
        NonNullList<ItemStack> inputSlots = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int slot = 0; slot < craftingInputStacks.size(); slot++) {
            inputSlots.set(slot, craftingInputStacks.get(slot).copyWithCount(1));
        }
        return CraftingInput.of(3, 3, inputSlots);
    }

    private static List<ItemStack> getCraftingOutputs(CraftingRecipe recipe, CraftingInput input, Level level) {
        List<ItemStack> craftingOutputs = new ArrayList<>();
        ItemStack recipeResult = recipe.assemble(input, level.registryAccess());
        if (recipeResult.isEmpty()) {
            return craftingOutputs;
        }

        craftingOutputs.add(recipeResult.copy());
        NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(input);
        for (ItemStack remainingItem : remainingItems) {
            if (remainingItem.isEmpty()) {
                continue;
            }

            craftingOutputs.add(remainingItem.copy());
        }

        return craftingOutputs;
    }

    private static boolean isAllowedAutomaticMixingRecipe(RecipeHolder<? extends Recipe<?>> holder) {
        if (AllRecipeTypes.shouldIgnoreInAutomation(holder)) {
            return false;
        }

        Recipe<?> recipe = holder.value();
        if (!(recipe instanceof ShapelessRecipe)) {
            return false;
        }

        int ingredientCount = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty() && ++ingredientCount > 1) {
                break;
            }
        }
        return ingredientCount > 1 && !MechanicalPressBlockEntity.canCompress(recipe);
    }

    private static boolean canResultPassTest(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        ItemStack recipePreview = recipe.getResultItem(level.registryAccess());
        return !recipePreview.isEmpty() && kettle.testRecipeFilter(recipePreview);
    }
}
