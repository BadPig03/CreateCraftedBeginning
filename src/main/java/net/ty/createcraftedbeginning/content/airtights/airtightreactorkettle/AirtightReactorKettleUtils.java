package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.fluid.FluidHelper;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrieFinder;
import net.ty.createcraftedbeginning.registry.CCBDamageSources;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleUtils {
    private static final Object CRAFTING_RECIPE_CACHE_KEY = new Object();
    private static final Object REACTOR_KETTLE_RECIPE_CACHE_KEY = new Object();
    private static final AtomicBoolean RECIPE_TRIE_FAILURE_LOGGED = new AtomicBoolean();
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

        Optional<ReactorKettleRecipe> recipe = findMatchingTrieRecipe(kettle, level);
        if (recipe.isPresent()) {
            return recipe;
        }
        return findMatchingLinearRecipe(kettle, level);
    }

    private static Optional<ReactorKettleRecipe> findMatchingTrieRecipe(AirtightReactorKettleBlockEntity kettle, Level level) {
        try {
            IItemHandler availableItems = kettle.getItemCapability();
            IFluidHandler availableFluids = kettle.getFluidCapability();
            IGasHandler availableGases = kettle.getGasCapability();
            AirtightWithGasRecipeTrie<?> trie = AirtightWithGasRecipeTrieFinder.get(REACTOR_KETTLE_RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof ReactorKettleRecipe);
            Set<AbstractVariant> availableVariants = AirtightWithGasRecipeTrie.getVariants(availableItems, availableFluids, availableGases);
            for (Recipe<?> candidate : trie.lookup(availableVariants)) {
                if (candidate instanceof ReactorKettleRecipe recipe && ReactorKettleRecipe.match(kettle, recipe)) {
                    return Optional.of(recipe);
                }
            }
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (RECIPE_TRIE_FAILURE_LOGGED.compareAndSet(false, true)) {
                CreateCraftedBeginning.LOGGER.error("Failed to build the reactor kettle recipe trie; falling back to a linear recipe search", e);
            }
        }
        return Optional.empty();
    }

    private static Optional<ReactorKettleRecipe> findMatchingLinearRecipe(AirtightReactorKettleBlockEntity kettle, Level level) {
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(REACTOR_KETTLE_RECIPE_CACHE_KEY, level, recipe -> recipe.value() instanceof ReactorKettleRecipe)) {
            if (holder.value() instanceof ReactorKettleRecipe recipe && ReactorKettleRecipe.match(kettle, recipe)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static long getRecipeCacheVersion() {
        return RECIPE_CACHE_VERSION.get();
    }

    public static void invalidateRecipeCaches() {
        RECIPE_TRIE_FAILURE_LOGGED.set(false);
        RECIPE_CACHE_VERSION.incrementAndGet();
    }

    public static float getTotalFluidUnits(SmartFluidTankBehaviour inputTank, SmartFluidTankBehaviour outputTank, float partialTicks) {
        return getFluidUnits(inputTank, partialTicks) + getFluidUnits(outputTank, partialTicks);
    }

    public static int getTotalFluidCapacity(SmartFluidTankBehaviour inputTank, SmartFluidTankBehaviour outputTank) {
        return getFluidCapacity(inputTank) + getFluidCapacity(outputTank);
    }

    private static int getFluidCapacity(SmartFluidTankBehaviour behaviour) {
        IFluidHandler capability = behaviour.getCapability();
        int capacity = 0;
        for (int tank = 0; tank < capability.getTanks(); tank++) {
            capacity += capability.getTankCapacity(tank);
        }
        return capacity;
    }

    private static float getFluidUnits(SmartFluidTankBehaviour behaviour, float partialTicks) {
        float totalUnits = 0;
        for (TankSegment tankSegment : behaviour.getTanks()) {
            if (tankSegment.getRenderedFluid().isEmpty()) {
                continue;
            }

            float units = tankSegment.getTotalUnits(partialTicks);
            if (units >= 1) {
                totalUnits += units;
            }
        }
        return totalUnits;
    }

    public static void insertItemEntity(AirtightReactorKettleStructuralBlockEntity structural, ItemEntity itemEntity) {
        AirtightReactorKettleBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return;
        }

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(master.getInventories().getFirst(), itemEntity.getItem().copy(), false);
        if (remainder.isEmpty()) {
            itemEntity.discard();
            return;
        }

        itemEntity.setItem(remainder);
    }

    public static void hurtInsideLivingEntities(AirtightReactorKettleStructuralBlockEntity structural, LivingEntity livingEntity) {
        AirtightReactorKettleBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return;
        }

        Level level = master.getLevel();
        if (level == null) {
            return;
        }

        float damage = master.getDamage();
        if (damage == 0) {
            return;
        }

        livingEntity.hurt(CCBDamageSources.reactorKettleMixer(level), damage);
    }

    public static ItemInteractionResult getUseItemOnResult(AirtightReactorKettleStructuralBlockEntity structural, Level level, Player player, BlockPos pos, InteractionHand hand, ItemStack stack) {
        AirtightReactorKettleBlockEntity master = structural.getMasterBlockEntity();
        if (master == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty()) {
            extractStoredItems(master, level, player, pos);
            return ItemInteractionResult.SUCCESS;
        }

        return transferFluidContainer(master, level, player, hand, stack);
    }

    private static ItemInteractionResult transferFluidContainer(AirtightReactorKettleBlockEntity master, Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) {
            boolean canEmpty = GenericItemEmptying.canItemBeEmptied(level, stack);
            boolean canFill = GenericItemFilling.canItemBeFilled(level, stack);
            return canEmpty || canFill ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, master) || GenericItemEmptying.canItemBeEmptied(level, stack)) {
            return ItemInteractionResult.SUCCESS;
        }

        if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, master) || GenericItemFilling.canItemBeFilled(level, stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void extractStoredItems(AirtightReactorKettleBlockEntity master, Level level, Player player, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        IItemHandlerModifiable inventory = master.getItemCapability();
        boolean extractedAny = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemHandlerHelper.giveItemToPlayer(player, stack);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            extractedAny = true;
        }
        if (!extractedAny) {
            return;
        }

        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1 + level.getRandom().nextFloat());
    }

    public static void updateRecipeFilter(AirtightReactorKettleStructuralBlockEntity structural, ItemStack stack) {
        AirtightReactorKettleBlockEntity master = structural.getMasterBlockEntity();
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

            if (!(level.getBlockEntity(filterPos) instanceof AirtightReactorKettleStructuralBlockEntity otherFilter)) {
                continue;
            }

            otherFilter.getFilteringBehaviour().setFilter(stack);
        }
    }

    public static boolean canModifyFilter(AirtightReactorKettleStructuralBlockEntity structural) {
        AirtightReactorKettleBlockEntity master = structural.getMasterBlockEntity();
        return master != null && !master.isFilterChanged();
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

        int[] extractedItemsFromSlot = new int[inputInventory.getSlots()];
        List<ItemStack> craftingStacks = new ArrayList<>();
        if (!planCraftingInputConsumption(recipe, inputInventory, extractedItemsFromSlot, craftingStacks)) {
            return false;
        }

        CraftingInput input = createCraftingInput(craftingStacks);
        if (!recipe.matches(input, level)) {
            return false;
        }

        List<ItemStack> outputs = getCraftingOutputs(recipe, input, level);
        if (!canApplyCraftingRecipe(kettle, recipe, input, outputs)) {
            return false;
        }

        executePlannedCraftingConsumption(inputInventory, extractedItemsFromSlot);
        return kettle.acceptOutputs(outputs, new ArrayList<>(), new ArrayList<>(), false);
    }

    private static boolean canApplyCraftingRecipe(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        Level level = kettle.getLevel();
        IItemHandler inputInventory = kettle.getInventories().getFirst();
        if (level == null || !(recipe instanceof ShapelessRecipe)) {
            return false;
        }

        int[] extractedItemsFromSlot = new int[inputInventory.getSlots()];
        List<ItemStack> craftingStacks = new ArrayList<>();
        if (!planCraftingInputConsumption(recipe, inputInventory, extractedItemsFromSlot, craftingStacks)) {
            return false;
        }

        CraftingInput input = createCraftingInput(craftingStacks);
        if (!recipe.matches(input, level)) {
            return false;
        }

        List<ItemStack> outputs = getCraftingOutputs(recipe, input, level);
        return canApplyCraftingRecipe(kettle, recipe, input, outputs);
    }

    private static boolean canApplyCraftingRecipe(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe, CraftingInput input, List<ItemStack> outputs) {
        Level level = kettle.getLevel();
        if (level == null || !recipe.matches(input, level) || outputs.isEmpty()) {
            return false;
        }

        FilteringBehaviour filter = kettle.getFilteringBehaviour();
        return filter != null && filter.test(outputs.getFirst()) && kettle.acceptOutputs(outputs, new ArrayList<>(), new ArrayList<>(), true);
    }

    private static boolean planCraftingInputConsumption(CraftingRecipe recipe, IItemHandler inputInventory, int[] extractedItemsFromSlot, List<ItemStack> craftingStacks) {
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
        return planCraftingInputConsumption(ingredients, 0, inputInventory, extractedItemsFromSlot, craftingStacks);
    }

    private static boolean planCraftingInputConsumption(List<Ingredient> ingredients, int ingredientIndex, IItemHandler inputInventory, int[] extractedItemsFromSlot, List<ItemStack> craftingStacks) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
            ItemStack stackInSlot = inputInventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty() || stackInSlot.getCount() <= extractedItemsFromSlot[slot] || !ingredient.test(stackInSlot)) {
                continue;
            }

            extractedItemsFromSlot[slot]++;
            craftingStacks.add(stackInSlot.copyWithCount(1));
            if (planCraftingInputConsumption(ingredients, ingredientIndex + 1, inputInventory, extractedItemsFromSlot, craftingStacks)) {
                return true;
            }

            craftingStacks.removeLast();
            extractedItemsFromSlot[slot]--;
        }
        return false;
    }

    private static int getMatchingItemCount(IItemHandler inputInventory, Ingredient ingredient) {
        int count = 0;
        for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
            ItemStack stackInSlot = inputInventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty() || !ingredient.test(stackInSlot)) {
                continue;
            }

            count += stackInSlot.getCount();
        }
        return count;
    }

    private static CraftingInput createCraftingInput(List<ItemStack> craftingStacks) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int slot = 0; slot < craftingStacks.size(); slot++) {
            stacks.set(slot, craftingStacks.get(slot).copyWithCount(1));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static void executePlannedCraftingConsumption(IItemHandler inputInventory, int @NotNull [] extractedItemsFromSlot) {
        for (int slot = 0; slot < extractedItemsFromSlot.length; slot++) {
            int amount = extractedItemsFromSlot[slot];
            if (amount <= 0) {
                continue;
            }

            inputInventory.extractItem(slot, amount, false);
        }
    }

    private static List<ItemStack> getCraftingOutputs(CraftingRecipe recipe, CraftingInput input, Level level) {
        List<ItemStack> outputs = new ArrayList<>();
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            return outputs;
        }

        outputs.add(result.copy());
        NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(input);
        for (ItemStack remaining : remainingItems) {
            if (remaining.isEmpty()) {
                continue;
            }

            outputs.add(remaining.copy());
        }

        return outputs;
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
        FilteringBehaviour filter = kettle.getFilteringBehaviour();
        if (level == null || filter == null) {
            return false;
        }

        ItemStack previewResult = recipe.getResultItem(level.registryAccess());
        return !previewResult.isEmpty() && filter.test(previewResult);
    }
}
