package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.TemperatureMatching;
import net.ty.createcraftedbeginning.recipe.interfaces.ReactorKettleRecipeContext;
import net.ty.createcraftedbeginning.recipe.trie.IAirtightWithGasRecipe;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorKettleRecipe extends StandardProcessingWithGasRecipe<RecipeInput> implements IAirtightWithGasRecipe {
    public ReactorKettleRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.REACTOR_KETTLE, params);
    }

    public static boolean match(ReactorKettleRecipeContext kettle, ReactorKettleRecipe recipe) {
        return kettle.matchesRecipeFilter(recipe) && apply(kettle, recipe, true);
    }

    public static boolean apply(ReactorKettleRecipeContext kettle, ReactorKettleRecipe recipe) {
        return apply(kettle, recipe, false);
    }

    public static boolean isExactTemperatureMatch(ReactorKettleRecipeContext kettle, ReactorKettleRecipe recipe) {
        return recipe.temperatureCondition.test(kettle.getRecipeTemperature());
    }

    public static int getTemperatureMatchPriority(ReactorKettleRecipeContext kettle, ReactorKettleRecipe recipe) {
        return TemperatureMatching.getMatchPriority(recipe.temperatureMatching, recipe.temperatureCondition, kettle.getRecipeTemperature());
    }

    public static @Unmodifiable List<Pair<Ingredient, Integer>> getCondensedIngredients(NonNullList<Ingredient> recipeIngredients) {
        Map<Ingredient, Integer> ingredientCounts = new LinkedHashMap<>();
        for (Ingredient ingredient : recipeIngredients) {
            if (ingredient.isEmpty()) {
                continue;
            }

            boolean foundMatch = false;
            for (Entry<Ingredient, Integer> countEntry : ingredientCounts.entrySet()) {
                Ingredient existingIngredient = countEntry.getKey();
                if (!existingIngredient.equals(ingredient)) {
                    continue;
                }

                ingredientCounts.put(existingIngredient, countEntry.getValue() + 1);
                foundMatch = true;
                break;
            }

            if (foundMatch) {
                continue;
            }

            ingredientCounts.put(ingredient, 1);
        }
        return ingredientCounts.entrySet().stream().map(countEntry -> Pair.of(countEntry.getKey(), countEntry.getValue())).toList();
    }

    private static boolean apply(ReactorKettleRecipeContext kettle, ReactorKettleRecipe recipe, boolean simulate) {
        IItemHandler availableItems = kettle.getAvailableItems();
        IFluidHandler availableFluids = kettle.getAvailableFluids();
        IGasHandler availableGases = kettle.getAvailableGases();
        if (!recipe.temperatureMatching.test(recipe.temperatureCondition, kettle.getRecipeTemperature())) {
            return false;
        }

        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        int[] plannedItemAmounts = new int[availableItems.getSlots()];
        int[] plannedFluidAmounts = new int[availableFluids.getTanks()];
        long[] plannedGasAmounts = new long[availableGases.getTanks()];
        if (!planInputConsumption(recipe, availableItems, availableFluids, availableGases, plannedItemAmounts, plannedFluidAmounts, plannedGasAmounts)) {
            return false;
        }

        List<ItemStack> outputItems = createRecipeOutputItems(recipe, level, availableItems, plannedItemAmounts, !simulate);
        List<FluidStack> outputFluids = createRecipeOutputFluids(recipe);
        List<GasStack> outputGases = createRecipeOutputGases(recipe);
        return canAcceptOutputsAfterInputsAreConsumed(kettle, availableItems, availableFluids, availableGases, outputItems, outputFluids, outputGases, plannedItemAmounts, plannedFluidAmounts, plannedGasAmounts) && (simulate || kettle.commitRecipeCraft(plannedItemAmounts, plannedFluidAmounts, plannedGasAmounts, outputItems, outputFluids, outputGases));
    }

    private static boolean planInputConsumption(ReactorKettleRecipe recipe, IItemHandler availableItems, IFluidHandler availableFluids, IGasHandler availableGases, int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts) {
        if (!planItemInputConsumption(recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList(), availableItems, itemAmounts)) {
            return false;
        }

        List<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        List<SizedGasIngredient> gasIngredients = recipe.getGasIngredients();
        return planFluidInputConsumption(fluidIngredients, availableFluids, fluidAmounts) && planGasInputConsumption(gasIngredients, availableGases, gasAmounts);
    }

    private static boolean planFluidInputConsumption(List<SizedFluidIngredient> fluidIngredients, IFluidHandler fluidHandler, int[] fluidAmounts) {
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

        if (!planResourceInputConsumption(availableTankAmounts, requiredAmounts, ingredientMatches, plannedTankAmounts)) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            fluidAmounts[tankIndex] = (int) plannedTankAmounts[tankIndex];
        }
        return true;
    }

    private static boolean planGasInputConsumption(List<SizedGasIngredient> gasIngredients, IGasHandler gasHandler, long[] gasAmounts) {
        if (gasIngredients.isEmpty()) {
            return true;
        }

        int tankCount = gasHandler.getTanks();
        long[] availableTankAmounts = new long[tankCount];
        long[] requiredAmounts = new long[gasIngredients.size()];
        boolean[][] ingredientMatches = new boolean[tankCount][gasIngredients.size()];
        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            availableTankAmounts[tankIndex] = gasHandler.getGasInTank(tankIndex).getAmount();
        }
        for (int ingredientIndex = 0; ingredientIndex < gasIngredients.size(); ingredientIndex++) {
            SizedGasIngredient ingredient = gasIngredients.get(ingredientIndex);
            requiredAmounts[ingredientIndex] = ingredient.amount();
            for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
                ingredientMatches[tankIndex][ingredientIndex] = ingredient.ingredient().test(gasHandler.getGasInTank(tankIndex));
            }
        }

        return planResourceInputConsumption(availableTankAmounts, requiredAmounts, ingredientMatches, gasAmounts);
    }

    private static boolean planResourceInputConsumption(long[] sourceAmounts, long[] requiredAmounts, boolean[][] ingredientMatches, long[] plannedAmounts) {
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

    private static List<ItemStack> createRecipeOutputItems(ReactorKettleRecipe recipe, Level level, IItemHandler availableItems, int[] itemAmounts, boolean rollRandomOutputs) {
        List<ItemStack> outputs = new ArrayList<>();
        if (rollRandomOutputs) {
            for (ItemStack itemStack : recipe.rollResults(level.random)) {
                if (itemStack.isEmpty()) {
                    continue;
                }

                outputs.add(itemStack.copy());
            }
        }
        else {
            for (ProcessingOutput output : recipe.getRollableResults()) {
                ItemStack itemStack = output.getStack();
                if (itemStack.isEmpty()) {
                    continue;
                }

                outputs.add(itemStack.copy());
            }
        }

        DummyCraftingContainer craftingContainer = new DummyCraftingContainer(availableItems, itemAmounts);
        for (ItemStack remainingItem : recipe.getRemainingItems(craftingContainer.asCraftInput())) {
            if (remainingItem.isEmpty()) {
                continue;
            }

            outputs.add(remainingItem.copy());
        }
        return outputs;
    }

    private static List<FluidStack> createRecipeOutputFluids(ReactorKettleRecipe recipe) {
        List<FluidStack> outputs = new ArrayList<>();
        for (FluidStack fluidStack : recipe.getFluidResults()) {
            if (fluidStack.isEmpty()) {
                continue;
            }

            outputs.add(fluidStack.copy());
        }
        return outputs;
    }

    private static List<GasStack> createRecipeOutputGases(ReactorKettleRecipe recipe) {
        List<GasStack> outputs = new ArrayList<>();
        for (GasStack gasStack : recipe.getGasResults()) {
            if (gasStack.isEmpty()) {
                continue;
            }

            outputs.add(gasStack.copy());
        }
        return outputs;
    }

    private static boolean canAcceptOutputsAfterInputsAreConsumed(ReactorKettleRecipeContext kettle, IItemHandler availableItems, IFluidHandler availableFluids, IGasHandler availableGases, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts) {
        IItemHandler outputInventory = kettle.getOutputItemCapability();
        IFluidHandler outputFluidTank = kettle.getOutputFluidCapability();
        IGasHandler outputGasTank = kettle.getOutputGasCapability();
        return canAcceptItemOutputsAfterInputsAreConsumed(availableItems, outputInventory, outputItems, itemAmounts) && canAcceptFluidOutputsAfterInputsAreConsumed(availableFluids, outputFluidTank, outputFluids, fluidAmounts) && canAcceptGasOutputsAfterInputsAreConsumed(availableGases, outputGasTank, outputGases, gasAmounts);
    }

    private static IItemHandlerModifiable createItemOutputSimulation(int slotCount) {
        return new ItemStackHandler(slotCount) {
            private static boolean isInsertionAllowed(IItemHandler inventory, int targetSlot, ItemStack itemStack) {
                int firstFreeSlot = -1;
                for (int candidateSlot = 0; candidateSlot < inventory.getSlots(); candidateSlot++) {
                    ItemStack storedStack = inventory.getStackInSlot(candidateSlot);
                    if (candidateSlot != targetSlot && ItemStack.isSameItemSameComponents(itemStack, storedStack)) {
                        return false;
                    }

                    if (!storedStack.isEmpty() || firstFreeSlot != -1) {
                        continue;
                    }

                    firstFreeSlot = candidateSlot;
                }
                return !inventory.getStackInSlot(targetSlot).isEmpty() || firstFreeSlot == targetSlot;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isInsertionAllowed(this, slot, stack)) {
                    return stack;
                }
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }
        };
    }

    private static boolean canAcceptItemOutputsAfterInputsAreConsumed(IItemHandler availableItems, IItemHandler outputInventory, List<ItemStack> outputItems, int[] extractedItemsFromSlot) {
        if (outputItems.isEmpty()) {
            return true;
        }

        IItemHandlerModifiable simulatedInventory = createItemOutputSimulation(outputInventory.getSlots());
        int outputOffset = availableItems.getSlots() - outputInventory.getSlots();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            ItemStack simulatedStack = outputInventory.getStackInSlot(slot).copy();
            int combinedSlot = outputOffset + slot;
            if (combinedSlot >= 0 && combinedSlot < extractedItemsFromSlot.length && extractedItemsFromSlot[combinedSlot] > 0) {
                simulatedStack.shrink(extractedItemsFromSlot[combinedSlot]);
            }
            simulatedInventory.setStackInSlot(slot, simulatedStack);
        }
        for (ItemStack outputItem : outputItems) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(simulatedInventory, outputItem.copy(), false);
            if (remainder.isEmpty()) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean canAcceptFluidOutputsAfterInputsAreConsumed(IFluidHandler availableFluids, IFluidHandler outputTank, List<FluidStack> outputFluids, int[] extractedFluidsFromTank) {
        if (outputFluids.isEmpty()) {
            return true;
        }

        FluidStack[] simulatedFluidTanks = new FluidStack[outputTank.getTanks()];
        int outputOffset = availableFluids.getTanks() - outputTank.getTanks();
        for (int tankIndex = 0; tankIndex < outputTank.getTanks(); tankIndex++) {
            FluidStack simulatedFluid = outputTank.getFluidInTank(tankIndex).copy();
            int combinedTankIndex = outputOffset + tankIndex;
            if (combinedTankIndex >= 0 && combinedTankIndex < extractedFluidsFromTank.length && extractedFluidsFromTank[combinedTankIndex] > 0) {
                simulatedFluid.shrink(extractedFluidsFromTank[combinedTankIndex]);
            }
            simulatedFluidTanks[tankIndex] = simulatedFluid;
        }
        for (FluidStack outputFluid : outputFluids) {
            if (insertFluidIntoSimulatedTank(simulatedFluidTanks, outputTank, outputFluid.copy())) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean insertFluidIntoSimulatedTank(FluidStack[] simulatedTanks, IFluidHandler targetTank, FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            return true;
        }

        for (int tankIndex = 0; tankIndex < simulatedTanks.length; tankIndex++) {
            FluidStack tankStack = simulatedTanks[tankIndex];
            if (tankStack.isEmpty() || !FluidStack.isSameFluidSameComponents(tankStack, fluidStack) || !targetTank.isFluidValid(tankIndex, fluidStack)) {
                continue;
            }

            int insertedAmount = Math.min(fluidStack.getAmount(), targetTank.getTankCapacity(tankIndex) - tankStack.getAmount());
            if (insertedAmount > 0) {
                tankStack.setAmount(tankStack.getAmount() + insertedAmount);
            }
            return insertedAmount == fluidStack.getAmount();
        }

        int remainingAmount = fluidStack.getAmount();
        for (int tankIndex = 0; tankIndex < simulatedTanks.length; tankIndex++) {
            if (remainingAmount <= 0) {
                return true;
            }

            FluidStack tankStack = simulatedTanks[tankIndex];
            if (!tankStack.isEmpty() || !targetTank.isFluidValid(tankIndex, fluidStack)) {
                continue;
            }

            int insertedAmount = Math.min(remainingAmount, targetTank.getTankCapacity(tankIndex));
            if (insertedAmount <= 0) {
                continue;
            }

            FluidStack insertedStack = fluidStack.copy();
            insertedStack.setAmount(insertedAmount);
            simulatedTanks[tankIndex] = insertedStack;
            remainingAmount -= insertedAmount;
        }
        return remainingAmount <= 0;
    }

    private static boolean canAcceptGasOutputsAfterInputsAreConsumed(IGasHandler availableGases, IGasHandler outputTank, List<GasStack> outputGases, long[] extractedGasesFromTank) {
        if (outputGases.isEmpty()) {
            return true;
        }

        GasStack[] simulatedGasTanks = new GasStack[outputTank.getTanks()];
        int outputOffset = availableGases.getTanks() - outputTank.getTanks();
        for (int tankIndex = 0; tankIndex < outputTank.getTanks(); tankIndex++) {
            GasStack simulatedGas = outputTank.getGasInTank(tankIndex).copy();
            int combinedTankIndex = outputOffset + tankIndex;
            if (combinedTankIndex >= 0 && combinedTankIndex < extractedGasesFromTank.length && extractedGasesFromTank[combinedTankIndex] > 0) {
                simulatedGas.shrink(extractedGasesFromTank[combinedTankIndex]);
            }
            simulatedGasTanks[tankIndex] = simulatedGas;
        }
        for (GasStack outputGas : outputGases) {
            if (insertGasIntoSimulatedTank(simulatedGasTanks, outputTank, outputGas.copy())) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean insertGasIntoSimulatedTank(GasStack[] simulatedTanks, IGasHandler targetTank, GasStack gasStack) {
        if (gasStack.isEmpty()) {
            return true;
        }

        for (int tankIndex = 0; tankIndex < simulatedTanks.length; tankIndex++) {
            GasStack tankStack = simulatedTanks[tankIndex];
            if (tankStack.isEmpty() || !GasStack.isSameGasSameComponents(tankStack, gasStack) || !targetTank.isGasValid(tankIndex, gasStack)) {
                continue;
            }

            long insertedAmount = Math.min(gasStack.getAmount(), targetTank.getTankCapacity(tankIndex) - tankStack.getAmount());
            if (insertedAmount > 0) {
                tankStack.setAmount(tankStack.getAmount() + insertedAmount);
            }
            return insertedAmount == gasStack.getAmount();
        }

        long remainingAmount = gasStack.getAmount();
        for (int tankIndex = 0; tankIndex < simulatedTanks.length; tankIndex++) {
            if (remainingAmount <= 0) {
                return true;
            }

            GasStack tankStack = simulatedTanks[tankIndex];
            if (!tankStack.isEmpty() || !targetTank.isGasValid(tankIndex, gasStack)) {
                continue;
            }

            long insertedAmount = Math.min(remainingAmount, targetTank.getTankCapacity(tankIndex));
            if (insertedAmount <= 0) {
                continue;
            }

            GasStack insertedStack = gasStack.copy();
            insertedStack.setAmount(insertedAmount);
            simulatedTanks[tankIndex] = insertedStack;
            remainingAmount -= insertedAmount;
        }

        return remainingAmount <= 0;
    }

    private static boolean planItemInputConsumption(List<Ingredient> ingredients, IItemHandler availableItems, int[] itemAmounts) {
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

        if (!planResourceInputConsumption(availableSlotAmounts, requiredAmounts, ingredientMatches, plannedSlotAmounts)) {
            return false;
        }

        for (int slot = 0; slot < slotCount; slot++) {
            itemAmounts[slot] = (int) plannedSlotAmounts[slot];
        }
        return true;
    }

    @Override
    protected int getMaxInputCount() {
        return 64;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    @Override
    protected boolean requireTemperatureCondition() {
        return true;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 3;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 2;
    }

    @Override
    protected int getMaxGasInputCount() {
        return 3;
    }

    @Override
    protected int getMaxGasOutputCount() {
        return 2;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return true;
    }
}