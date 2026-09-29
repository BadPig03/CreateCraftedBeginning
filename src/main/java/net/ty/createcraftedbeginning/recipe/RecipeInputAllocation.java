package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class RecipeInputAllocation {
    private RecipeInputAllocation() {
    }

    static boolean planFluids(List<SizedFluidIngredient> fluidIngredients, IFluidHandler fluidHandler, int[] fluidAmounts) {
        if (fluidIngredients.isEmpty()) {
            return true;
        }

        int tankCount = fluidHandler.getTanks();
        long[] availableTankAmounts = new long[tankCount];
        long[] plannedTankAmounts = new long[tankCount];
        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            availableTankAmounts[tankIndex] = fluidHandler.getFluidInTank(tankIndex).getAmount();
            plannedTankAmounts[tankIndex] = fluidAmounts[tankIndex];
        }

        long[] requiredAmounts = new long[fluidIngredients.size()];
        boolean[][] ingredientMatches = new boolean[tankCount][fluidIngredients.size()];
        for (int ingredientIndex = 0; ingredientIndex < fluidIngredients.size(); ingredientIndex++) {
            SizedFluidIngredient ingredient = fluidIngredients.get(ingredientIndex);
            requiredAmounts[ingredientIndex] = ingredient.amount();
            for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
                ingredientMatches[tankIndex][ingredientIndex] = ingredient.ingredient().test(fluidHandler.getFluidInTank(tankIndex));
            }
        }

        if (!RecipeInputAllocation.plan(availableTankAmounts, requiredAmounts, ingredientMatches, plannedTankAmounts)) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            fluidAmounts[tankIndex] = (int) plannedTankAmounts[tankIndex];
        }
        return true;
    }

    static boolean planItems(List<Ingredient> ingredients, IItemHandler availableItems, int[] itemAmounts) {
        if (ingredients.isEmpty()) {
            return true;
        }

        int slotCount = availableItems.getSlots();
        long[] availableSlotAmounts = new long[slotCount];
        long[] plannedSlotAmounts = new long[slotCount];
        ItemStack[] extractableStacks = new ItemStack[slotCount];
        for (int slot = 0; slot < slotCount; slot++) {
            plannedSlotAmounts[slot] = itemAmounts[slot];
            ItemStack storedStack = availableItems.getStackInSlot(slot);
            if (storedStack.isEmpty()) {
                extractableStacks[slot] = ItemStack.EMPTY;
                continue;
            }

            ItemStack extractableStack = availableItems.extractItem(slot, storedStack.getCount(), true);
            extractableStacks[slot] = extractableStack;
            availableSlotAmounts[slot] = extractableStack.getCount();
        }

        long[] requiredAmounts = new long[ingredients.size()];
        boolean[][] ingredientMatches = new boolean[slotCount][ingredients.size()];
        Arrays.fill(requiredAmounts, 1);
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            Ingredient ingredient = ingredients.get(ingredientIndex);
            for (int slot = 0; slot < slotCount; slot++) {
                ItemStack extractableStack = extractableStacks[slot];
                ingredientMatches[slot][ingredientIndex] = !extractableStack.isEmpty() && ingredient.test(extractableStack);
            }
        }

        if (!RecipeInputAllocation.plan(availableSlotAmounts, requiredAmounts, ingredientMatches, plannedSlotAmounts)) {
            return false;
        }

        for (int slot = 0; slot < slotCount; slot++) {
            itemAmounts[slot] = (int) plannedSlotAmounts[slot];
        }
        return true;
    }

    static boolean plan(long[] sourceAmounts, long[] requiredAmounts, boolean[][] ingredientMatches, long[] plannedAmounts) {
        int sourceCount = sourceAmounts.length;
        int ingredientCount = requiredAmounts.length;
        int sourceNode = 0;
        int resourceOffset = 1;
        int ingredientOffset = resourceOffset + sourceCount;
        int sinkNode = ingredientOffset + ingredientCount;
        long[][] residualCapacity = new long[sinkNode + 1][sinkNode + 1];
        long[] availableAmounts = new long[sourceCount];
        long totalRequired = 0;

        for (int resourceSource = 0; resourceSource < sourceCount; resourceSource++) {
            long availableAmount = sourceAmounts[resourceSource] - plannedAmounts[resourceSource];
            if (availableAmount <= 0) {
                continue;
            }

            availableAmounts[resourceSource] = availableAmount;
            residualCapacity[sourceNode][resourceOffset + resourceSource] = availableAmount;
        }

        for (int ingredientIndex = 0; ingredientIndex < ingredientCount; ingredientIndex++) {
            long requiredAmount = requiredAmounts[ingredientIndex];
            if (requiredAmount < 0 || Long.MAX_VALUE - totalRequired < requiredAmount) {
                return false;
            }

            totalRequired += requiredAmount;
            residualCapacity[ingredientOffset + ingredientIndex][sinkNode] = requiredAmount;
            for (int resourceSource = 0; resourceSource < sourceCount; resourceSource++) {
                if (!ingredientMatches[resourceSource][ingredientIndex] || availableAmounts[resourceSource] <= 0) {
                    continue;
                }

                residualCapacity[resourceOffset + resourceSource][ingredientOffset + ingredientIndex] = Math.min(availableAmounts[resourceSource], requiredAmount);
            }
        }

        long totalFlow = 0;
        int[] parent = new int[residualCapacity.length];
        while (totalFlow < totalRequired) {
            Arrays.fill(parent, -1);
            parent[sourceNode] = sourceNode;
            ArrayDeque<Integer> pendingNodes = new ArrayDeque<>();
            pendingNodes.add(sourceNode);
            while (!pendingNodes.isEmpty() && parent[sinkNode] == -1) {
                int currentNode = pendingNodes.removeFirst();
                for (int nextNode = 0; nextNode < residualCapacity.length; nextNode++) {
                    if (parent[nextNode] != -1 || residualCapacity[currentNode][nextNode] <= 0) {
                        continue;
                    }

                    parent[nextNode] = currentNode;
                    pendingNodes.addLast(nextNode);
                }
            }

            if (parent[sinkNode] == -1) {
                return false;
            }

            long pathFlow = totalRequired - totalFlow;
            for (int node = sinkNode; node != sourceNode; node = parent[node]) {
                pathFlow = Math.min(pathFlow, residualCapacity[parent[node]][node]);
            }
            for (int node = sinkNode; node != sourceNode; node = parent[node]) {
                int previousNode = parent[node];
                residualCapacity[previousNode][node] -= pathFlow;
                residualCapacity[node][previousNode] += pathFlow;
            }
            totalFlow += pathFlow;
        }

        for (int resourceSource = 0; resourceSource < sourceCount; resourceSource++) {
            plannedAmounts[resourceSource] += availableAmounts[resourceSource] - residualCapacity[sourceNode][resourceOffset + resourceSource];
        }
        return true;
    }
}
