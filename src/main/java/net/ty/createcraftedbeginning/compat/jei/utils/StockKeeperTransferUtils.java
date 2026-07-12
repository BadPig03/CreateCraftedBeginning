package net.ty.createcraftedbeginning.compat.jei.utils;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.foundation.utility.CreateLang;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.stockkeeper.GasCraftableBigItemStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class StockKeeperTransferUtils {
    private StockKeeperTransferUtils() {
    }

    public static @Nullable List<BigItemStack> collectRequirements(IRecipeSlotsView recipeSlots, InventorySummary summary, List<BigItemStack> existingOrders) {
        List<BigItemStack> requirements = new ArrayList<>();
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<BigItemStack> candidates = getCandidates(slotView);
            if (candidates.isEmpty()) {
                continue;
            }

            BigItemStack chosen = chooseCandidate(candidates, summary, requirements, existingOrders);
            if (chosen == null) {
                return null;
            }

            mergeRequirement(requirements, chosen);
        }
        return requirements;
    }

    public static @Nullable OutputTarget getOutputTarget(IRecipeSlotsView recipeSlots, Player player, Recipe<?> recipe) {
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
            Optional<ItemStack> itemOutput = slotView.getItemStacks().filter(stack -> !stack.isEmpty()).findFirst();
            if (itemOutput.isPresent()) {
                ItemStack stack = itemOutput.get();
                return new OutputTarget(stack.copyWithCount(1), Math.max(1, stack.getCount()), Math.max(1, stack.getMaxStackSize()));
            }

            Optional<GasStack> gasOutput = slotView.getIngredients(CCBJEIPlugin.GAS_STACK).filter(gas -> gas != null && !gas.isEmpty()).findFirst();
            if (gasOutput.isPresent()) {
                GasStack gas = gasOutput.get();
                int outputAmount = GasRequestUtils.toLogisticsAmount(gas.getAmount());
                if (outputAmount <= 0) {
                    return null;
                }

                ItemStack virtualGas = GasVirtualUtils.createVirtualItem(gas.copyWithAmount(1));
                if (virtualGas.isEmpty()) {
                    return null;
                }

                return new OutputTarget(virtualGas.copyWithCount(1), outputAmount, outputAmount);
            }
        }

        ItemStack result = recipe.getResultItem(player.level().registryAccess());
        if (result.isEmpty()) {
            return null;
        }
        return new OutputTarget(result.copyWithCount(1), Math.max(1, result.getCount()), Math.max(1, result.getMaxStackSize()));
    }

    public static IRecipeTransferError throwError(String key) {
        return new IRecipeTransferError() {
            @Override
            public Type getType() {
                return Type.USER_FACING;
            }

            @Override
            public void getTooltip(ITooltipBuilder tooltip) {
                tooltip.add(CreateLang.translate(key).component());
            }
        };
    }

    public static boolean containsGasIngredient(IRecipeSlotsView recipeSlots, RecipeIngredientRole role) {
        return recipeSlots.getSlotViews(role).stream().anyMatch(slotView -> slotView.getIngredients(CCBJEIPlugin.GAS_STACK).anyMatch(gas -> gas != null && !gas.isEmpty()));
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

        boolean removing = requestedOutputDifference < 0;
        if (removing) {
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

            if (!ItemStack.isSameItemSameComponents(stack, target)) {
                continue;
            }

            return true;
        }
        return false;
    }

    public static int getMatchingCount(List<BigItemStack> stacks, ItemStack target) {
        int total = 0;
        for (BigItemStack entry : stacks) {
            if (ItemStack.isSameItemSameComponents(entry.stack, target)) {
                total = GasRequestUtils.toLogisticsAmount((long) total + entry.count);
            }
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

    private static @Nullable BigItemStack chooseCandidate(List<BigItemStack> candidates, InventorySummary summary, List<BigItemStack> selectedRequirements, List<BigItemStack> existingOrders) {
        BigItemStack best = null;
        int bestBatches = 0;
        boolean bestAlreadyUsed = false;
        for (BigItemStack candidate : candidates) {
            int alreadySelected = getMatchingCount(selectedRequirements, candidate.stack);
            int alreadyOrdered = getMatchingCount(existingOrders, candidate.stack);
            int available = summary.getCountOf(candidate.stack) - alreadySelected - alreadyOrdered;
            if (available < candidate.count) {
                continue;
            }

            int batches = available / candidate.count;
            boolean alreadyUsed = alreadySelected > 0 || alreadyOrdered > 0;
            if (best == null) {
                best = candidate;
                bestBatches = batches;
                bestAlreadyUsed = alreadyUsed;
                continue;
            }

            if (alreadyUsed && !bestAlreadyUsed) {
                best = candidate;
                bestBatches = batches;
                bestAlreadyUsed = true;
                continue;
            }

            if (alreadyUsed != bestAlreadyUsed || batches <= bestBatches) {
                continue;
            }

            best = candidate;
            bestBatches = batches;
        }
        if (best == null) {
            return null;
        }
        return new BigItemStack(best.stack.copyWithCount(1), best.count);
    }

    private static List<BigItemStack> getCandidates(IRecipeSlotView slotView) {
        List<BigItemStack> candidates = new ArrayList<>();
        slotView.getItemStacks().forEach(stack -> {
            if (stack.isEmpty()) {
                return;
            }

            candidates.add(new BigItemStack(stack.copyWithCount(1), Math.max(1, stack.getCount())));
        });

        slotView.getIngredients(CCBJEIPlugin.GAS_STACK).forEach(gas -> {
            if (gas == null || gas.isEmpty()) {
                return;
            }

            int amount = GasRequestUtils.toLogisticsAmount(gas.getAmount());
            if (amount <= 0) {
                return;
            }

            ItemStack virtualGas = GasVirtualUtils.createVirtualItem(gas.copyWithAmount(1));
            if (virtualGas.isEmpty()) {
                return;
            }

            candidates.add(new BigItemStack(virtualGas.copyWithCount(1), amount));
        });
        return candidates;
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
        screen.searchBox.setValue("");
        screen.refreshSearchNextTick = true;
        screen.moveToTopNextTick = true;
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
        screen.searchBox.setValue("");
        screen.refreshSearchNextTick = true;
        screen.moveToTopNextTick = true;
        return true;
    }

    private static int getMaxSetsFromOrderedItems(InventorySummary ordered, InventorySummary used, List<BigItemStack> requirements) {
        int maxSets = Integer.MAX_VALUE;
        for (BigItemStack requirement : requirements) {
            if (requirement.count <= 0) {
                return 0;
            }

            int available = ordered.getCountOf(requirement.stack) - used.getCountOf(requirement.stack);
            maxSets = Math.min(maxSets, available / requirement.count);
        }
        return maxSets == Integer.MAX_VALUE ? 0 : Math.max(0, maxSets);
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

    private static void mergeRequirement(List<BigItemStack> requirements, BigItemStack candidate) {
        BigItemStack existing = findMatchingOrder(requirements, candidate.stack);
        if (existing == null) {
            requirements.add(new BigItemStack(candidate.stack.copyWithCount(1), candidate.count));
            return;
        }

        existing.count = GasRequestUtils.toLogisticsAmount((long) existing.count + candidate.count);
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

            ItemStack normalized = candidateStack.copyWithCount(1);
            int requiredCount = Math.max(1, candidateStack.getCount());
            int alreadyUsed = usedItems.getCountOf(normalized);
            int alreadySelected = getMatchingCount(selectedRequirements, normalized);
            int available = orderedItems.getCountOf(normalized) - alreadyUsed - alreadySelected;
            if (available < requiredCount || available <= bestAvailable) {
                continue;
            }

            bestAvailable = available;
            best = new BigItemStack(normalized, requiredCount);
        }
        return best;
    }

    public record OutputTarget(ItemStack displayStack, int outputPerCraft, int transferLimit) {}
}
