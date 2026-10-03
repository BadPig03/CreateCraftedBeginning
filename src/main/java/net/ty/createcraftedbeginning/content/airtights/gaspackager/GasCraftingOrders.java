package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCraftingOrders {
    private final List<BigItemStack> orders;
    private final List<CraftableBigItemStack> recipes;

    public GasCraftingOrders(List<BigItemStack> orders, List<CraftableBigItemStack> recipes) {
        this.orders = orders;
        this.recipes = recipes;
    }

    public static int getMatchingCount(List<BigItemStack> stacks, ItemStack targetStack) {
        int matchingCount = 0;
        for (BigItemStack entry : stacks) {
            if (!ItemStack.isSameItemSameComponents(entry.stack, targetStack)) {
                continue;
            }

            matchingCount = (int) Mth.clamp((long) matchingCount + entry.count, 0L, BigItemStack.INF);
        }
        return matchingCount;
    }

    public static void mergeRequirement(List<BigItemStack> requirements, BigItemStack requirement) {
        BigItemStack existingRequirement = findMatchingOrder(requirements, requirement.stack);
        if (existingRequirement == null) {
            requirements.add(new BigItemStack(requirement.stack.copyWithCount(1), requirement.count));
            return;
        }

        existingRequirement.count = (int) Mth.clamp((long) existingRequirement.count + requirement.count, 0L, BigItemStack.INF);
    }

    private static boolean canFitNewOrderTypes(List<BigItemStack> existingOrders, List<BigItemStack> requirements) {
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

    private static int getMaxAdditionalSets(InventorySummary stockSummary, List<BigItemStack> existingOrders, List<BigItemStack> requirements) {
        int maxSets = Integer.MAX_VALUE;
        for (BigItemStack requirement : requirements) {
            if (requirement.count <= 0) {
                return 0;
            }

            int alreadyOrdered = getMatchingCount(existingOrders, requirement.stack);
            int availableCount = stockSummary.getCountOf(requirement.stack) - alreadyOrdered;
            maxSets = Math.min(maxSets, availableCount / requirement.count);
        }
        if (maxSets == Integer.MAX_VALUE) {
            return 0;
        }

        return Math.max(0, maxSets);
    }

    private static @Nullable BigItemStack findMatchingOrder(List<BigItemStack> orders, ItemStack targetStack) {
        return orders.stream().filter(order -> ItemStack.isSameItemSameComponents(order.stack, targetStack)).findFirst().orElse(null);
    }

    public TransferResult transfer(Recipe<?> recipe, ItemStack output, int outputPerCraft, List<BigItemStack> requirements, InventorySummary stock, boolean maximum, boolean execute, Supplier<InventorySummary> currentStock) {
        GasCraftableBigItemStack entry = recipes.stream().filter(candidate -> candidate instanceof GasCraftableBigItemStack gas && gas.matches(recipe, output)).map(candidate -> (GasCraftableBigItemStack) candidate).findFirst().orElse(null);
        boolean newEntry = entry == null;
        if (newEntry && recipes.size() >= 9) {
            return TransferResult.FULL;
        }

        if (entry == null) {
            entry = new GasCraftableBigItemStack(output, recipe, outputPerCraft, requirements);
        }
        List<BigItemStack> inputs = entry.getRequirements();
        if (!canFitNewOrderTypes(orders, inputs)) {
            return TransferResult.FULL;
        }

        int availableSets = getMaxAdditionalSets(stock, orders, inputs);
        int requestedSets = maximum ? availableSets : 1;
        if (availableSets <= 0) {
            return TransferResult.UNAVAILABLE;
        }

        if (!execute) {
            return TransferResult.SUCCESS;
        }

        if (!add(entry, requestedSets, currentStock.get())) {
            return TransferResult.UNAVAILABLE;
        }

        return TransferResult.SUCCESS;
    }

    public boolean add(GasCraftableBigItemStack recipe, int requestedSets, @Nullable InventorySummary stock) {
        List<BigItemStack> requirements = recipe.getRequirements();
        if (stock == null || !canFitNewOrderTypes(orders, requirements)) {
            return false;
        }

        int sets = Math.min(requestedSets, getMaxAdditionalSets(stock, orders, requirements));
        if (sets <= 0) {
            return false;
        }

        if (!recipes.contains(recipe)) {
            recipes.add(recipe);
        }
        recipe.count = (int) Mth.clamp((long) recipe.count + (long) recipe.getOutputPerCraft() * sets, 0L, BigItemStack.INF);
        requirements.forEach(requirement -> addToOrders(requirement, sets));
        return true;
    }

    public boolean remove(GasCraftableBigItemStack recipe, int requestedSets) {
        int output = recipe.getOutputPerCraft();
        int sets = Math.min(requestedSets, recipe.count / output);
        if (sets <= 0) {
            return false;
        }

        recipe.count -= output * sets;
        recipe.getRequirements().forEach(requirement -> removeFromOrders(requirement, sets));
        if (recipe.count <= 0) {
            recipes.remove(recipe);
        }
        return true;
    }

    private void addToOrders(BigItemStack requirement, int sets) {
        BigItemStack existingOrder = findMatchingOrder(orders, requirement.stack);
        int addedCount = (int) Mth.clamp((long) requirement.count * sets, 0L, BigItemStack.INF);
        if (addedCount <= 0) {
            return;
        }

        if (existingOrder == null) {
            orders.add(new BigItemStack(requirement.stack.copyWithCount(1), addedCount));
            return;
        }

        existingOrder.count = (int) Mth.clamp((long) existingOrder.count + addedCount, 0L, BigItemStack.INF);
    }

    private void removeFromOrders(BigItemStack requirement, int sets) {
        BigItemStack existingOrder = findMatchingOrder(orders, requirement.stack);
        if (existingOrder == null) {
            return;
        }

        int removedCount = (int) Mth.clamp((long) requirement.count * sets, 0L, BigItemStack.INF);
        existingOrder.count -= removedCount;
        if (existingOrder.count > 0) {
            return;
        }

        orders.remove(existingOrder);
    }

    public enum TransferResult {
        SUCCESS,
        FULL,
        UNAVAILABLE
    }
}
