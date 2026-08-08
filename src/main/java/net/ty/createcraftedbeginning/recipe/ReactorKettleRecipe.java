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
import net.ty.createcraftedbeginning.recipe.trie.IAirtightWithGasRecipe;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    public static @Unmodifiable List<Pair<Ingredient, Integer>> getCondensedIngredients(NonNullList<Ingredient> recipeIngredients) {
        Map<Ingredient, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ingredient : recipeIngredients) {
            if (ingredient.isEmpty()) {
                continue;
            }

            boolean found = false;
            for (Entry<Ingredient, Integer> entry : counts.entrySet()) {
                Ingredient existing = entry.getKey();
                if (!existing.equals(ingredient)) {
                    continue;
                }

                counts.put(existing, entry.getValue() + 1);
                found = true;
                break;
            }

            if (!found) {
                counts.put(ingredient, 1);
            }
        }
        return counts.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).toList();
    }

    private static boolean apply(ReactorKettleRecipeContext kettle, ReactorKettleRecipe recipe, boolean simulate) {
        IItemHandler availableItems = kettle.getItemCapability();
        IFluidHandler availableFluids = kettle.getFluidCapability();
        IGasHandler availableGases = kettle.getGasCapability();
        if (!recipe.temperatureCondition.test(kettle.getRecipeTemperature())) {
            return false;
        }

        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        int[] itemAmounts = new int[availableItems.getSlots()];
        int[] fluidAmounts = new int[availableFluids.getTanks()];
        long[] gasAmounts = new long[availableGases.getTanks()];
        if (!planInputConsumption(recipe, availableItems, availableFluids, availableGases, itemAmounts, fluidAmounts, gasAmounts)) {
            return false;
        }

        List<ItemStack> outputItems = createRecipeOutputItems(recipe, level, availableItems, itemAmounts, !simulate);
        List<FluidStack> outputFluids = createRecipeOutputFluids(recipe);
        List<GasStack> outputGases = createRecipeOutputGases(recipe);
        return canAcceptOutputsAfterInputsAreConsumed(kettle, availableItems, availableFluids, availableGases, outputItems, outputFluids, outputGases, itemAmounts, fluidAmounts, gasAmounts) && (simulate || kettle.commitRecipeCraft(itemAmounts, fluidAmounts, gasAmounts, outputItems, outputFluids, outputGases));
    }

    private static boolean planInputConsumption(ReactorKettleRecipe recipe, IItemHandler availableItems, IFluidHandler availableFluids, IGasHandler availableGases, int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts) {
        List<ItemRequirement> itemRequirements = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).map(ingredient -> createItemRequirement(availableItems, ingredient)).sorted(Comparator.comparingInt((ItemRequirement requirement) -> requirement.candidateSlots().length).thenComparingInt(ItemRequirement::matchingItemCount)).toList();
        if (!planItemInputConsumption(itemRequirements, 0, availableItems, itemAmounts)) {
            return false;
        }

        List<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        List<SizedGasIngredient> gasIngredients = recipe.getGasIngredients();
        return planFluidInputConsumption(fluidIngredients, availableFluids, fluidAmounts) && planGasInputConsumption(gasIngredients, availableGases, gasAmounts);
    }

    private static boolean planFluidInputConsumption(List<SizedFluidIngredient> ingredients, IFluidHandler fluids, int[] amounts) {
        if (ingredients.isEmpty()) {
            return true;
        }

        int tankCount = fluids.getTanks();
        long[] tankAmounts = new long[tankCount];
        long[] plannedAmounts = new long[tankCount];
        for (int tank = 0; tank < tankCount; tank++) {
            tankAmounts[tank] = fluids.getFluidInTank(tank).getAmount();
            plannedAmounts[tank] = amounts[tank];
        }

        long[] requiredAmounts = new long[ingredients.size()];
        boolean[][] matches = new boolean[tankCount][ingredients.size()];
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            SizedFluidIngredient ingredient = ingredients.get(ingredientIndex);
            requiredAmounts[ingredientIndex] = ingredient.amount();
            for (int tank = 0; tank < tankCount; tank++) {
                matches[tank][ingredientIndex] = ingredient.test(fluids.getFluidInTank(tank));
            }
        }

        if (!planTankInputConsumption(tankAmounts, requiredAmounts, matches, plannedAmounts)) {
            return false;
        }

        for (int tank = 0; tank < tankCount; tank++) {
            amounts[tank] = (int) plannedAmounts[tank];
        }
        return true;
    }

    private static boolean planGasInputConsumption(List<SizedGasIngredient> ingredients, IGasHandler gases, long[] amounts) {
        if (ingredients.isEmpty()) {
            return true;
        }

        int tankCount = gases.getTanks();
        long[] tankAmounts = new long[tankCount];
        long[] requiredAmounts = new long[ingredients.size()];
        boolean[][] matches = new boolean[tankCount][ingredients.size()];
        for (int tank = 0; tank < tankCount; tank++) {
            tankAmounts[tank] = gases.getGasInTank(tank).getAmount();
        }
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            SizedGasIngredient ingredient = ingredients.get(ingredientIndex);
            requiredAmounts[ingredientIndex] = ingredient.amount();
            for (int tank = 0; tank < tankCount; tank++) {
                matches[tank][ingredientIndex] = ingredient.test(gases.getGasInTank(tank));
            }
        }

        return planTankInputConsumption(tankAmounts, requiredAmounts, matches, amounts);
    }

    // Solve the small tank-to-ingredient allocation as a flow network so broad ingredients cannot starve restrictive ones.
    private static boolean planTankInputConsumption(long[] tankAmounts, long[] requiredAmounts, boolean[][] matches, long[] plannedAmounts) {
        int tankCount = tankAmounts.length;
        int ingredientCount = requiredAmounts.length;
        int source = 0;
        int tankOffset = 1;
        int ingredientOffset = tankOffset + tankCount;
        int sink = ingredientOffset + ingredientCount;
        long[][] residualCapacity = new long[sink + 1][sink + 1];
        long[] availableAmounts = new long[tankCount];
        long totalRequired = 0;

        for (int tank = 0; tank < tankCount; tank++) {
            long available = tankAmounts[tank] - plannedAmounts[tank];
            if (available <= 0) {
                continue;
            }

            availableAmounts[tank] = available;
            residualCapacity[source][tankOffset + tank] = available;
        }

        for (int ingredient = 0; ingredient < ingredientCount; ingredient++) {
            long required = requiredAmounts[ingredient];
            if (required < 0 || Long.MAX_VALUE - totalRequired < required) {
                return false;
            }

            totalRequired += required;
            residualCapacity[ingredientOffset + ingredient][sink] = required;
            for (int tank = 0; tank < tankCount; tank++) {
                if (matches[tank][ingredient] && availableAmounts[tank] > 0) {
                    residualCapacity[tankOffset + tank][ingredientOffset + ingredient] = Math.min(availableAmounts[tank], required);
                }
            }
        }

        long totalFlow = 0;
        int[] parent = new int[residualCapacity.length];
        while (totalFlow < totalRequired) {
            Arrays.fill(parent, -1);
            parent[source] = source;
            ArrayDeque<Integer> pending = new ArrayDeque<>();
            pending.add(source);
            while (!pending.isEmpty() && parent[sink] == -1) {
                int current = pending.removeFirst();
                for (int next = 0; next < residualCapacity.length; next++) {
                    if (parent[next] != -1 || residualCapacity[current][next] <= 0) {
                        continue;
                    }

                    parent[next] = current;
                    pending.addLast(next);
                }
            }

            if (parent[sink] == -1) {
                return false;
            }

            long pathFlow = totalRequired - totalFlow;
            for (int node = sink; node != source; node = parent[node]) {
                pathFlow = Math.min(pathFlow, residualCapacity[parent[node]][node]);
            }
            for (int node = sink; node != source; node = parent[node]) {
                int previous = parent[node];
                residualCapacity[previous][node] -= pathFlow;
                residualCapacity[node][previous] += pathFlow;
            }
            totalFlow += pathFlow;
        }

        for (int tank = 0; tank < tankCount; tank++) {
            plannedAmounts[tank] += availableAmounts[tank] - residualCapacity[source][tankOffset + tank];
        }
        return true;
    }

    private static List<ItemStack> createRecipeOutputItems(ReactorKettleRecipe recipe, Level level, IItemHandler availableItems, int[] itemAmounts, boolean rollRandomOutputs) {
        List<ItemStack> outputs = new ArrayList<>();
        if (rollRandomOutputs) {
            for (ItemStack stack : recipe.rollResults(level.random)) {
                if (stack.isEmpty()) {
                    continue;
                }

                outputs.add(stack.copy());
            }
        }
        else {
            for (ProcessingOutput output : recipe.getRollableResults()) {
                ItemStack stack = output.getStack();
                if (stack.isEmpty()) {
                    continue;
                }

                outputs.add(stack.copy());
            }
        }

        DummyCraftingContainer container = new DummyCraftingContainer(availableItems, itemAmounts);
        for (ItemStack stack : recipe.getRemainingItems(container.asCraftInput())) {
            if (stack.isEmpty()) {
                continue;
            }

            outputs.add(stack.copy());
        }
        return outputs;
    }

    private static List<FluidStack> createRecipeOutputFluids(ReactorKettleRecipe recipe) {
        List<FluidStack> outputs = new ArrayList<>();
        for (FluidStack stack : recipe.getFluidResults()) {
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }
        return outputs;
    }

    private static List<GasStack> createRecipeOutputGases(ReactorKettleRecipe recipe) {
        List<GasStack> outputs = new ArrayList<>();
        for (GasStack stack : recipe.getGasResults()) {
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }
        return outputs;
    }

    private static boolean canAcceptOutputsAfterInputsAreConsumed(ReactorKettleRecipeContext kettle, IItemHandler availableItems, IFluidHandler availableFluids, IGasHandler availableGases, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts) {
        IItemHandler outputInventory = kettle.getOutputItemCapability();
        IFluidHandler outputFluidTank = kettle.getOutputFluidCapability();
        IGasHandler outputGasTank = kettle.getOutputGasCapability();
        return canAcceptItemOutputsAfterInputsAreConsumed(availableItems, outputInventory, outputItems, itemAmounts) && canAcceptFluidOutputsAfterInputsAreConsumed(availableFluids, outputFluidTank, outputFluids, fluidAmounts) && canAcceptGasOutputsAfterInputsAreConsumed(availableGases, outputGasTank, outputGases, gasAmounts);
    }

    private static IItemHandlerModifiable createItemOutputSimulation(int slots) {
        return new ItemStackHandler(slots) {
            private static boolean isInsertionAllowed(IItemHandler inventory, int slot, ItemStack stack) {
                int firstFreeSlot = -1;
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack storedStack = inventory.getStackInSlot(i);
                    if (i != slot && ItemStack.isSameItemSameComponents(stack, storedStack)) {
                        return false;
                    }

                    if (!storedStack.isEmpty() || firstFreeSlot != -1) {
                        continue;
                    }

                    firstFreeSlot = i;
                }
                return !inventory.getStackInSlot(slot).isEmpty() || firstFreeSlot == slot;
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

        IItemHandlerModifiable simulatedOutput = createItemOutputSimulation(outputInventory.getSlots());
        int outputOffset = availableItems.getSlots() - outputInventory.getSlots();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            ItemStack stack = outputInventory.getStackInSlot(slot).copy();
            int combinedSlot = outputOffset + slot;
            if (combinedSlot >= 0 && combinedSlot < extractedItemsFromSlot.length && extractedItemsFromSlot[combinedSlot] > 0) {
                stack.shrink(extractedItemsFromSlot[combinedSlot]);
            }
            simulatedOutput.setStackInSlot(slot, stack);
        }
        for (ItemStack outputItem : outputItems) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(simulatedOutput, outputItem.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canAcceptFluidOutputsAfterInputsAreConsumed(IFluidHandler availableFluids, IFluidHandler outputTank, List<FluidStack> outputFluids, int[] extractedFluidsFromTank) {
        if (outputFluids.isEmpty()) {
            return true;
        }

        FluidStack[] simulatedTanks = new FluidStack[outputTank.getTanks()];
        int outputOffset = availableFluids.getTanks() - outputTank.getTanks();
        for (int tank = 0; tank < outputTank.getTanks(); tank++) {
            FluidStack fluidStack = outputTank.getFluidInTank(tank).copy();
            int combinedTank = outputOffset + tank;
            if (combinedTank >= 0 && combinedTank < extractedFluidsFromTank.length && extractedFluidsFromTank[combinedTank] > 0) {
                fluidStack.shrink(extractedFluidsFromTank[combinedTank]);
            }
            simulatedTanks[tank] = fluidStack;
        }
        for (FluidStack outputFluid : outputFluids) {
            if (!insertFluidIntoSimulatedTank(simulatedTanks, outputTank, outputFluid.copy())) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertFluidIntoSimulatedTank(FluidStack[] simulatedTanks, IFluidHandler targetTank, FluidStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        int remaining = stack.getAmount();
        for (int tank = 0; tank < simulatedTanks.length; tank++) {
            if (remaining <= 0) {
                return true;
            }

            FluidStack tankStack = simulatedTanks[tank];
            if (tankStack.isEmpty() || !FluidStack.isSameFluidSameComponents(tankStack, stack) || !targetTank.isFluidValid(tank, stack)) {
                continue;
            }

            int space = targetTank.getTankCapacity(tank) - tankStack.getAmount();
            int inserted = Math.min(remaining, space);
            if (inserted <= 0) {
                continue;
            }

            tankStack.setAmount(tankStack.getAmount() + inserted);
            remaining -= inserted;
        }

        for (int tank = 0; tank < simulatedTanks.length; tank++) {
            if (remaining <= 0) {
                return true;
            }

            FluidStack tankStack = simulatedTanks[tank];
            if (!tankStack.isEmpty() || !targetTank.isFluidValid(tank, stack)) {
                continue;
            }

            int inserted = Math.min(remaining, targetTank.getTankCapacity(tank));
            if (inserted <= 0) {
                continue;
            }

            FluidStack insertedStack = stack.copy();
            insertedStack.setAmount(inserted);
            simulatedTanks[tank] = insertedStack;
            remaining -= inserted;
        }

        return remaining <= 0;
    }

    private static boolean canAcceptGasOutputsAfterInputsAreConsumed(IGasHandler availableGases, IGasHandler outputTank, List<GasStack> outputGases, long[] extractedGasesFromTank) {
        if (outputGases.isEmpty()) {
            return true;
        }

        GasStack[] simulatedTanks = new GasStack[outputTank.getTanks()];
        int outputOffset = availableGases.getTanks() - outputTank.getTanks();
        for (int tank = 0; tank < outputTank.getTanks(); tank++) {
            GasStack gasStack = outputTank.getGasInTank(tank).copy();
            int combinedTank = outputOffset + tank;
            if (combinedTank >= 0 && combinedTank < extractedGasesFromTank.length && extractedGasesFromTank[combinedTank] > 0) {
                gasStack.shrink(extractedGasesFromTank[combinedTank]);
            }
            simulatedTanks[tank] = gasStack;
        }
        for (GasStack outputGas : outputGases) {
            if (!insertGasIntoSimulatedTank(simulatedTanks, outputTank, outputGas.copy())) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertGasIntoSimulatedTank(GasStack[] simulatedTanks, IGasHandler targetTank, GasStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        long remaining = stack.getAmount();
        for (int tank = 0; tank < simulatedTanks.length; tank++) {
            if (remaining <= 0) {
                return true;
            }

            GasStack tankStack = simulatedTanks[tank];
            if (tankStack.isEmpty() || !GasStack.isSameGasSameComponents(tankStack, stack) || !targetTank.isGasValid(tank, stack)) {
                continue;
            }

            long space = targetTank.getTankCapacity(tank) - tankStack.getAmount();
            long inserted = Math.min(remaining, space);
            if (inserted <= 0) {
                continue;
            }

            tankStack.setAmount(tankStack.getAmount() + inserted);
            remaining -= inserted;
        }

        for (int tank = 0; tank < simulatedTanks.length; tank++) {
            if (remaining <= 0) {
                return true;
            }

            GasStack tankStack = simulatedTanks[tank];
            if (!tankStack.isEmpty() || !targetTank.isGasValid(tank, stack)) {
                continue;
            }

            long inserted = Math.min(remaining, targetTank.getTankCapacity(tank));
            if (inserted <= 0) {
                continue;
            }

            GasStack insertedStack = stack.copy();
            insertedStack.setAmount(inserted);
            simulatedTanks[tank] = insertedStack;
            remaining -= inserted;
        }

        return remaining <= 0;
    }

    private static boolean planItemInputConsumption(List<ItemRequirement> requirements, int requirementIndex, IItemHandler availableItems, int[] itemAmounts) {
        if (requirementIndex >= requirements.size()) {
            return true;
        }

        ItemRequirement requirement = requirements.get(requirementIndex);
        for (int slot : requirement.candidateSlots()) {
            ItemStack stackInSlot = availableItems.getStackInSlot(slot);
            if (stackInSlot.isEmpty() || stackInSlot.getCount() <= itemAmounts[slot]) {
                continue;
            }

            itemAmounts[slot]++;
            if (planItemInputConsumption(requirements, requirementIndex + 1, availableItems, itemAmounts)) {
                return true;
            }

            itemAmounts[slot]--;
        }

        return false;
    }

    private static ItemRequirement createItemRequirement(IItemHandler availableItems, Ingredient ingredient) {
        int[] candidateSlots = new int[availableItems.getSlots()];
        int candidateCount = 0;
        int matchingCount = 0;
        for (int slot = 0; slot < availableItems.getSlots(); slot++) {
            ItemStack stack = availableItems.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack extractable = availableItems.extractItem(slot, stack.getCount(), true);
            if (extractable.isEmpty() || !ingredient.test(extractable)) {
                continue;
            }

            candidateSlots[candidateCount++] = slot;
            matchingCount += extractable.getCount();
        }
        return new ItemRequirement(Arrays.copyOf(candidateSlots, candidateCount), matchingCount);
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

    private record ItemRequirement(int[] candidateSlots, int matchingItemCount) {}
}