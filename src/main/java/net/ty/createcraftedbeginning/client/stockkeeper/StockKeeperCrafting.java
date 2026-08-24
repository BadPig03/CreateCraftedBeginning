package net.ty.createcraftedbeginning.client.stockkeeper;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class StockKeeperCrafting {
    private StockKeeperCrafting() {
    }

    public static boolean requestCraftable(AbstractContainerScreen<?> screen, GasCraftableBigItemStack recipe, int requestedOutputDifference) {
        if (!(screen instanceof StockKeeperRequestScreen requestScreen)) {
            return false;
        }

        if (requestedOutputDifference == 0) {
            return false;
        }

        int outputPerCraft = recipe.getOutputPerCraft();
        int requestedSets = (Math.abs(requestedOutputDifference) + outputPerCraft - 1) / outputPerCraft;
        if (requestedSets <= 0) {
            return false;
        }

        if (requestedOutputDifference < 0) {
            return removeSets(requestScreen, recipe, requestedSets);
        }
        return addSets(requestScreen, recipe, requestedSets);
    }

    public static boolean canFitNewOrderTypes(List<BigItemStack> existingOrders, List<BigItemStack> requirements) {
        int orderTypeCount = existingOrders.size();
        List<ItemStack> newOrderTypes = new ArrayList<>();
        for (BigItemStack requirement : requirements) {
            if (hasMatchingStack(existingOrders, requirement.stack) || hasMatchingStack(newOrderTypes, requirement.stack)) {
                continue;
            }

            newOrderTypes.add(requirement.stack.copyWithCount(1));
            orderTypeCount++;
            if (orderTypeCount <= 9) {
                continue;
            }

            return false;
        }
        return true;
    }

    public static boolean hasGasCraftable(AbstractContainerScreen<?> screen) {
        return screen instanceof StockKeeperRequestScreen requestScreen && requestScreen.recipesToOrder.stream().anyMatch(recipe -> recipe instanceof GasCraftableBigItemStack);
    }

    private static boolean hasMatchingStack(List<?> entries, ItemStack targetStack) {
        for (Object entry : entries) {
            ItemStack entryStack;
            switch (entry) {
                case BigItemStack bigItemStack -> entryStack = bigItemStack.stack;
                case ItemStack itemStack -> entryStack = itemStack;
                default -> {
                    continue;
                }
            }

            if (!ItemStack.isSameItemSameComponents(entryStack, targetStack)) {
                continue;
            }

            return true;
        }
        return false;
    }

    public static int getMatchingCount(List<BigItemStack> stacks, ItemStack targetStack) {
        int matchingCount = 0;
        for (BigItemStack entry : stacks) {
            if (!ItemStack.isSameItemSameComponents(entry.stack, targetStack)) {
                continue;
            }

            matchingCount = GasRequestUtils.toLogisticsAmount((long) matchingCount + entry.count);
        }
        return matchingCount;
    }

    public static int getMaxAdditionalSets(InventorySummary stockSummary, List<BigItemStack> existingOrders, List<BigItemStack> requirements) {
        int maxSets = Integer.MAX_VALUE;
        for (BigItemStack requirement : requirements) {
            if (requirement.count <= 0) {
                return 0;
            }

            int alreadyOrdered = getMatchingCount(existingOrders, requirement.stack);
            int availableCount = stockSummary.getCountOf(requirement.stack) - alreadyOrdered;
            maxSets = Math.min(maxSets, availableCount / requirement.count);
        }
        return maxSets == Integer.MAX_VALUE ? 0 : Math.max(0, maxSets);
    }

    public static void updateCraftableAmounts(AbstractContainerScreen<?> screen) {
        if (!(screen instanceof StockKeeperRequestScreen requestScreen)) {
            return;
        }

        Level level = requestScreen.getMenu().contentHolder.getLevel();
        if (level == null) {
            return;
        }

        InventorySummary orderedItems = new InventorySummary();
        InventorySummary usedItems = new InventorySummary();
        requestScreen.itemsToOrder.forEach(ordered -> orderedItems.add(ordered.stack, ordered.count));
        Iterator<CraftableBigItemStack> recipeIterator = requestScreen.recipesToOrder.iterator();
        while (recipeIterator.hasNext()) {
            CraftableBigItemStack craftable = recipeIterator.next();
            if (craftable instanceof GasCraftableBigItemStack gasCraftable) {
                updateGasCraftable(recipeIterator, gasCraftable, orderedItems, usedItems);
                continue;
            }

            updateNormalCraftable(recipeIterator, craftable, orderedItems, usedItems, level);
        }
    }

    private static @Nullable BigItemStack findMatchingOrder(List<BigItemStack> orders, ItemStack targetStack) {
        return orders.stream().filter(order -> ItemStack.isSameItemSameComponents(order.stack, targetStack)).findFirst().orElse(null);
    }

    public static void mergeRequirement(List<BigItemStack> requirements, BigItemStack requirement) {
        BigItemStack existingRequirement = findMatchingOrder(requirements, requirement.stack);
        if (existingRequirement == null) {
            requirements.add(new BigItemStack(requirement.stack.copyWithCount(1), requirement.count));
            return;
        }

        existingRequirement.count = GasRequestUtils.toLogisticsAmount((long) existingRequirement.count + requirement.count);
    }

    private static boolean addSets(StockKeeperRequestScreen screen, GasCraftableBigItemStack recipe, int requestedSets) {
        InventorySummary stockSummary = screen.getMenu().contentHolder.getLastClientsideStockSnapshotAsSummary();
        if (stockSummary == null) {
            return false;
        }

        List<BigItemStack> requirements = recipe.getRequirements();
        if (!canFitNewOrderTypes(screen.itemsToOrder, requirements)) {
            return false;
        }

        int maxAdditionalSets = getMaxAdditionalSets(stockSummary, screen.itemsToOrder, requirements);
        int setsToAdd = Math.min(requestedSets, maxAdditionalSets);
        if (setsToAdd <= 0) {
            return false;
        }

        if (!screen.recipesToOrder.contains(recipe)) {
            screen.recipesToOrder.add(recipe);
        }
        recipe.count = GasRequestUtils.toLogisticsAmount((long) recipe.count + (long) recipe.getOutputPerCraft() * setsToAdd);
        requirements.forEach(requirement -> addToOrders(screen.itemsToOrder, requirement, setsToAdd));
        refreshScreen(screen);
        return true;
    }

    private static boolean removeSets(StockKeeperRequestScreen screen, GasCraftableBigItemStack recipe, int requestedSets) {
        int currentSets = recipe.count / recipe.getOutputPerCraft();
        int setsToRemove = Math.min(requestedSets, currentSets);
        if (setsToRemove <= 0) {
            return false;
        }

        recipe.count -= recipe.getOutputPerCraft() * setsToRemove;
        recipe.getRequirements().forEach(requirement -> removeFromOrders(screen.itemsToOrder, requirement, setsToRemove));
        if (recipe.count <= 0) {
            screen.recipesToOrder.remove(recipe);
        }
        refreshScreen(screen);
        return true;
    }

    private static int getMaxSetsFromOrderedItems(InventorySummary orderedItems, InventorySummary usedItems, List<BigItemStack> requirements) {
        int maxSets = Integer.MAX_VALUE;
        for (BigItemStack requirement : requirements) {
            if (requirement.count <= 0) {
                return 0;
            }

            int availableCount = orderedItems.getCountOf(requirement.stack) - usedItems.getCountOf(requirement.stack);
            maxSets = Math.min(maxSets, availableCount / requirement.count);
        }
        return maxSets == Integer.MAX_VALUE ? 0 : Math.max(0, maxSets);
    }

    private static void refreshScreen(StockKeeperRequestScreen screen) {
        screen.searchBox.setValue("");
        screen.refreshSearchNextTick = true;
        screen.moveToTopNextTick = true;
    }

    private static void addToOrders(List<BigItemStack> orders, BigItemStack requirement, int sets) {
        BigItemStack existingOrder = findMatchingOrder(orders, requirement.stack);
        int addedCount = GasRequestUtils.toLogisticsAmount((long) requirement.count * sets);
        if (addedCount <= 0) {
            return;
        }

        if (existingOrder == null) {
            orders.add(new BigItemStack(requirement.stack.copyWithCount(1), addedCount));
            return;
        }

        existingOrder.count = GasRequestUtils.toLogisticsAmount((long) existingOrder.count + addedCount);
    }

    private static void removeFromOrders(List<BigItemStack> orders, BigItemStack requirement, int sets) {
        BigItemStack existingOrder = findMatchingOrder(orders, requirement.stack);
        if (existingOrder == null) {
            return;
        }

        int removedCount = GasRequestUtils.toLogisticsAmount((long) requirement.count * sets);
        existingOrder.count -= removedCount;
        if (existingOrder.count > 0) {
            return;
        }

        orders.remove(existingOrder);
    }

    private static void updateGasCraftable(Iterator<CraftableBigItemStack> recipeIterator, GasCraftableBigItemStack gasCraftable, InventorySummary orderedItems, InventorySummary usedItems) {
        int outputPerCraft = Math.max(1, gasCraftable.getOutputPerCraft());
        int requestedSets = gasCraftable.count / outputPerCraft;
        if (requestedSets <= 0) {
            recipeIterator.remove();
            return;
        }

        int maxSets = getMaxSetsFromOrderedItems(orderedItems, usedItems, gasCraftable.getRequirements());
        int appliedSets = Math.min(requestedSets, maxSets);
        if (appliedSets <= 0) {
            gasCraftable.count = 0;
            recipeIterator.remove();
            return;
        }

        gasCraftable.count = outputPerCraft * appliedSets;
        gasCraftable.getRequirements().forEach(requirement -> usedItems.add(requirement.stack, requirement.count * appliedSets));
    }

    private static void updateNormalCraftable(Iterator<CraftableBigItemStack> recipeIterator, CraftableBigItemStack craftable, InventorySummary orderedItems, InventorySummary usedItems, Level level) {
        int outputPerCraft = Math.max(1, craftable.getOutputCount(level));
        int requestedSets = craftable.count / outputPerCraft;
        if (requestedSets <= 0) {
            recipeIterator.remove();
            return;
        }

        List<BigItemStack> requirements = collectNormalRequirements(craftable, orderedItems, usedItems);
        if (requirements == null || requirements.isEmpty()) {
            craftable.count = 0;
            recipeIterator.remove();
            return;
        }

        int maxSets = getMaxSetsFromOrderedItems(orderedItems, usedItems, requirements);
        int appliedSets = Math.min(requestedSets, maxSets);
        if (appliedSets <= 0) {
            craftable.count = 0;
            recipeIterator.remove();
            return;
        }

        craftable.count = outputPerCraft * appliedSets;
        requirements.forEach(requirement -> usedItems.add(requirement.stack, requirement.count * appliedSets));
    }

    private static @Nullable List<BigItemStack> collectNormalRequirements(CraftableBigItemStack craftable, InventorySummary orderedItems, InventorySummary usedItems) {
        List<BigItemStack> requirements = new ArrayList<>();
        for (Ingredient ingredient : craftable.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }

            BigItemStack selectedRequirement = chooseIngredientCandidate(ingredient, orderedItems, usedItems, requirements);
            if (selectedRequirement == null) {
                return null;
            }

            mergeRequirement(requirements, selectedRequirement);
        }
        return requirements;
    }

    private static @Nullable BigItemStack chooseIngredientCandidate(Ingredient ingredient, InventorySummary orderedItems, InventorySummary usedItems, List<BigItemStack> selectedRequirements) {
        BigItemStack bestCandidate = null;
        int bestAvailableCount = -1;
        for (ItemStack candidateStack : ingredient.getItems()) {
            if (candidateStack.isEmpty()) {
                continue;
            }

            ItemStack candidateUnitStack = candidateStack.copyWithCount(1);
            int requiredCount = Math.max(1, candidateStack.getCount());
            int alreadyUsed = usedItems.getCountOf(candidateUnitStack);
            int alreadySelected = getMatchingCount(selectedRequirements, candidateUnitStack);
            int availableCount = orderedItems.getCountOf(candidateUnitStack) - alreadyUsed - alreadySelected;
            if (availableCount < requiredCount || availableCount <= bestAvailableCount) {
                continue;
            }

            bestAvailableCount = availableCount;
            bestCandidate = new BigItemStack(candidateUnitStack, requiredCount);
        }
        return bestCandidate;
    }
}
