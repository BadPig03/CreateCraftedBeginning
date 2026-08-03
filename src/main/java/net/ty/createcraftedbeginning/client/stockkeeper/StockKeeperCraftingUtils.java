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
public final class StockKeeperCraftingUtils {
    private StockKeeperCraftingUtils() {
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
        int totalTypes = existingOrders.size();
        List<ItemStack> newTypes = new ArrayList<>();
        for (BigItemStack requirement : requirements) {
            if (hasMatchingStack(existingOrders, requirement.stack) || hasMatchingStack(newTypes, requirement.stack)) {
                continue;
            }

            newTypes.add(requirement.stack.copyWithCount(1));
            totalTypes++;
            if (totalTypes <= 9) {
                continue;
            }

            return false;
        }
        return true;
    }

    public static boolean hasGasCraftable(AbstractContainerScreen<?> screen) {
        return screen instanceof StockKeeperRequestScreen requestScreen && requestScreen.recipesToOrder.stream().anyMatch(recipe -> recipe instanceof GasCraftableBigItemStack);
    }

    public static boolean hasMatchingStack(List<?> stacks, ItemStack target) {
        for (Object object : stacks) {
            ItemStack stack;
            if (object instanceof BigItemStack bigItemStack) {
                stack = bigItemStack.stack;
            }
            else if (object instanceof ItemStack itemStack) {
                stack = itemStack;
            }
            else {
                continue;
            }

            if (ItemStack.isSameItemSameComponents(stack, target)) {
                return true;
            }
        }
        return false;
    }

    public static int getMatchingCount(List<BigItemStack> stacks, ItemStack target) {
        int total = 0;
        for (BigItemStack entry : stacks) {
            if (!ItemStack.isSameItemSameComponents(entry.stack, target)) {
                continue;
            }

            total = GasRequestUtils.toLogisticsAmount((long) total + entry.count);
        }
        return total;
    }

    public static int getMaxAdditionalSets(InventorySummary summary, List<BigItemStack> existingOrders, List<BigItemStack> requirements) {
        int maxSets = Integer.MAX_VALUE;
        for (BigItemStack requirement : requirements) {
            if (requirement.count <= 0) {
                return 0;
            }

            int alreadyOrdered = getMatchingCount(existingOrders, requirement.stack);
            int available = summary.getCountOf(requirement.stack) - alreadyOrdered;
            maxSets = Math.min(maxSets, available / requirement.count);
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
        Iterator<CraftableBigItemStack> iterator = requestScreen.recipesToOrder.iterator();
        while (iterator.hasNext()) {
            CraftableBigItemStack craftable = iterator.next();
            if (craftable instanceof GasCraftableBigItemStack gasCraftable) {
                updateGasCraftable(iterator, gasCraftable, orderedItems, usedItems);
                continue;
            }

            updateNormalCraftable(iterator, craftable, orderedItems, usedItems, level);
        }
    }

    public static @Nullable BigItemStack findMatchingOrder(List<BigItemStack> stacks, ItemStack target) {
        return stacks.stream().filter(entry -> ItemStack.isSameItemSameComponents(entry.stack, target)).findFirst().orElse(null);
    }

    public static void mergeRequirement(List<BigItemStack> requirements, BigItemStack candidate) {
        BigItemStack existing = findMatchingOrder(requirements, candidate.stack);
        if (existing == null) {
            requirements.add(new BigItemStack(candidate.stack.copyWithCount(1), candidate.count));
            return;
        }

        existing.count = GasRequestUtils.toLogisticsAmount((long) existing.count + candidate.count);
    }

    private static boolean addSets(StockKeeperRequestScreen screen, GasCraftableBigItemStack recipe, int requestedSets) {
        InventorySummary summary = screen.getMenu().contentHolder.getLastClientsideStockSnapshotAsSummary();
        if (summary == null) {
            return false;
        }

        List<BigItemStack> requirements = recipe.getRequirements();
        if (!canFitNewOrderTypes(screen.itemsToOrder, requirements)) {
            return false;
        }

        int maxAdditionalSets = getMaxAdditionalSets(summary, screen.itemsToOrder, requirements);
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

            int available = orderedItems.getCountOf(requirement.stack) - usedItems.getCountOf(requirement.stack);
            maxSets = Math.min(maxSets, available / requirement.count);
        }
        return maxSets == Integer.MAX_VALUE ? 0 : Math.max(0, maxSets);
    }

    private static void refreshScreen(StockKeeperRequestScreen screen) {
        screen.searchBox.setValue("");
        screen.refreshSearchNextTick = true;
        screen.moveToTopNextTick = true;
    }

    private static void addToOrders(List<BigItemStack> orders, BigItemStack requirement, int sets) {
        BigItemStack existing = findMatchingOrder(orders, requirement.stack);
        int delta = GasRequestUtils.toLogisticsAmount((long) requirement.count * sets);
        if (delta <= 0) {
            return;
        }

        if (existing == null) {
            orders.add(new BigItemStack(requirement.stack.copyWithCount(1), delta));
            return;
        }

        existing.count = GasRequestUtils.toLogisticsAmount((long) existing.count + delta);
    }

    private static void removeFromOrders(List<BigItemStack> orders, BigItemStack requirement, int sets) {
        BigItemStack existing = findMatchingOrder(orders, requirement.stack);
        if (existing == null) {
            return;
        }

        int delta = GasRequestUtils.toLogisticsAmount((long) requirement.count * sets);
        existing.count -= delta;
        if (existing.count > 0) {
            return;
        }

        orders.remove(existing);
    }

    private static void updateGasCraftable(Iterator<CraftableBigItemStack> iterator, GasCraftableBigItemStack gasCraftable, InventorySummary orderedItems, InventorySummary usedItems) {
        int outputPerCraft = Math.max(1, gasCraftable.getOutputPerCraft());
        int requestedSets = gasCraftable.count / outputPerCraft;
        if (requestedSets <= 0) {
            iterator.remove();
            return;
        }

        int maxSets = getMaxSetsFromOrderedItems(orderedItems, usedItems, gasCraftable.getRequirements());
        int appliedSets = Math.min(requestedSets, maxSets);
        if (appliedSets <= 0) {
            gasCraftable.count = 0;
            iterator.remove();
            return;
        }

        gasCraftable.count = outputPerCraft * appliedSets;
        gasCraftable.getRequirements().forEach(requirement -> usedItems.add(requirement.stack, requirement.count * appliedSets));
    }

    private static void updateNormalCraftable(Iterator<CraftableBigItemStack> iterator, CraftableBigItemStack craftable, InventorySummary orderedItems, InventorySummary usedItems, Level level) {
        int outputPerCraft = Math.max(1, craftable.getOutputCount(level));
        int requestedSets = craftable.count / outputPerCraft;
        if (requestedSets <= 0) {
            iterator.remove();
            return;
        }

        List<BigItemStack> requirements = collectNormalRequirements(craftable, orderedItems, usedItems);
        if (requirements == null || requirements.isEmpty()) {
            craftable.count = 0;
            iterator.remove();
            return;
        }

        int maxSets = getMaxSetsFromOrderedItems(orderedItems, usedItems, requirements);
        int appliedSets = Math.min(requestedSets, maxSets);
        if (appliedSets <= 0) {
            craftable.count = 0;
            iterator.remove();
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

            BigItemStack chosen = chooseIngredientCandidate(ingredient, orderedItems, usedItems, requirements);
            if (chosen == null) {
                return null;
            }

            mergeRequirement(requirements, chosen);
        }
        return requirements;
    }

    private static @Nullable BigItemStack chooseIngredientCandidate(Ingredient ingredient, InventorySummary orderedItems, InventorySummary usedItems, List<BigItemStack> selectedRequirements) {
        BigItemStack best = null;
        int bestAvailable = -1;
        for (ItemStack candidateStack : ingredient.getItems()) {
            if (candidateStack.isEmpty()) {
                continue;
            }

            ItemStack stack = candidateStack.copyWithCount(1);
            int requiredCount = Math.max(1, candidateStack.getCount());
            int alreadyUsed = usedItems.getCountOf(stack);
            int alreadySelected = getMatchingCount(selectedRequirements, stack);
            int available = orderedItems.getCountOf(stack) - alreadyUsed - alreadySelected;
            if (available < requiredCount || available <= bestAvailable) {
                continue;
            }

            bestAvailable = available;
            best = new BigItemStack(stack, requiredCount);
        }
        return best;
    }
}
