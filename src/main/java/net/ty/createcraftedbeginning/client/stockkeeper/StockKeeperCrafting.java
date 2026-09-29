package net.ty.createcraftedbeginning.client.stockkeeper;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasCraftableBigItemStack;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasCraftingOrders;
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

    public static void requestCraftable(AbstractContainerScreen<?> screen, GasCraftableBigItemStack recipe, int requestedOutputDifference) {
        if (!(screen instanceof StockKeeperRequestScreen requestScreen)) {
            return;
        }

        if (requestedOutputDifference == 0) {
            return;
        }

        int outputPerCraft = recipe.getOutputPerCraft();
        int requestedSets = Mth.positiveCeilDiv(Mth.abs(requestedOutputDifference), outputPerCraft);
        if (requestedSets <= 0) {
            return;
        }

        if (requestedOutputDifference < 0) {
            boolean changed = new GasCraftingOrders(requestScreen.itemsToOrder, requestScreen.recipesToOrder).remove(recipe, requestedSets);
            if (changed) {
                refreshScreen(requestScreen);
            }
            return;
        }

        InventorySummary stock = requestScreen.getMenu().contentHolder.getLastClientsideStockSnapshotAsSummary();
        if (!new GasCraftingOrders(requestScreen.itemsToOrder, requestScreen.recipesToOrder).add(recipe, requestedSets, stock)) {
            return;
        }

        refreshScreen(requestScreen);
    }

    public static boolean hasGasCraftable(AbstractContainerScreen<?> screen) {
        return screen instanceof StockKeeperRequestScreen requestScreen && requestScreen.recipesToOrder.stream().anyMatch(recipe -> recipe instanceof GasCraftableBigItemStack);
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

    private static int getMaxSetsFromOrderedItems(InventorySummary orderedItems, InventorySummary usedItems, List<BigItemStack> requirements) {
        int maxSets = Integer.MAX_VALUE;
        for (BigItemStack requirement : requirements) {
            if (requirement.count <= 0) {
                return 0;
            }

            int availableCount = orderedItems.getCountOf(requirement.stack) - usedItems.getCountOf(requirement.stack);
            maxSets = Math.min(maxSets, availableCount / requirement.count);
        }
        if (maxSets == Integer.MAX_VALUE) {
            return 0;
        }

        return Math.max(0, maxSets);
    }

    private static void refreshScreen(StockKeeperRequestScreen screen) {
        screen.searchBox.setValue("");
        screen.refreshSearchNextTick = true;
        screen.moveToTopNextTick = true;
    }

    private static void updateGasCraftable(Iterator<CraftableBigItemStack> recipeIterator, GasCraftableBigItemStack gasCraftable, InventorySummary orderedItems, InventorySummary usedItems) {
        int outputPerCraft = Math.max(1, gasCraftable.getOutputPerCraft());
        int requestedSets = gasCraftable.count / outputPerCraft;
        if (requestedSets <= 0) {
            recipeIterator.remove();
            return;
        }

        List<BigItemStack> requirements = gasCraftable.getRequirements();
        int maxSets = getMaxSetsFromOrderedItems(orderedItems, usedItems, requirements);
        int appliedSets = Math.min(requestedSets, maxSets);
        if (appliedSets <= 0) {
            gasCraftable.count = 0;
            recipeIterator.remove();
            return;
        }

        gasCraftable.count = outputPerCraft * appliedSets;
        requirements.forEach(requirement -> usedItems.add(requirement.stack, requirement.count * appliedSets));
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

            GasCraftingOrders.mergeRequirement(requirements, selectedRequirement);
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
            int alreadySelected = GasCraftingOrders.getMatchingCount(selectedRequirements, candidateUnitStack);
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
