package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterItem;
import net.ty.createcraftedbeginning.recipe.trie.IAirtightWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
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

    @OnlyIn(Dist.CLIENT)
    public static RecipeHolder<ReactorKettleRecipe> convertToReactorKettleRecipe(RecipeHolder<?> holder) {
        Builder<ReactorKettleRecipe> builder = new Builder<>(ReactorKettleRecipe::new, holder.id());
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return new RecipeHolder<>(holder.id(), builder.build());
        }

        return new RecipeHolder<>(holder.id(), new Builder<>(ReactorKettleRecipe::new, holder.id()).withItemIngredients(holder.value().getIngredients()).withSingleItemOutput(holder.value().getResultItem(level.registryAccess())).build());
    }

    public static boolean match(AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe) {
        FilteringBehaviour filter = kettle.getFilteringBehaviour();
        Level level = kettle.getLevel();
        if (filter == null || level == null) {
            return false;
        }

        ItemStack filterItem = filter.getFilter();
        if (filterItem.getItem() instanceof GasFilterItem gasFilter && !recipe.getGasResults().isEmpty()) {
            return gasFilter.test(filterItem, recipe.getGasResults().getFirst()) && apply(kettle, recipe, true);
        }

        boolean filterTest = filter.test(recipe.getResultItem(level.registryAccess()));
        if (recipe.getRollableResults().isEmpty() && !recipe.getFluidResults().isEmpty()) {
            filterTest = filter.test(recipe.getFluidResults().getFirst());
        }
        return filterTest && apply(kettle, recipe, true);
    }

    public static boolean apply(AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe) {
        return apply(kettle, recipe, false);
    }

    public static @Unmodifiable List<Pair<Ingredient, Integer>> getCondensedIngredients(NonNullList<Ingredient> recipeIngredients) {
        Map<Ingredient, Integer> ingredientCountMap = new LinkedHashMap<>();
        for (Ingredient currentIngredient : recipeIngredients) {
            if (currentIngredient.isEmpty()) {
                continue;
            }

            boolean found = false;
            for (Entry<Ingredient, Integer> entry : ingredientCountMap.entrySet()) {
                Ingredient existingIngredient = entry.getKey();
                if (!existingIngredient.equals(currentIngredient)) {
                    continue;
                }

                ingredientCountMap.put(existingIngredient, entry.getValue() + 1);
                found = true;
                break;
            }

            if (!found) {
                ingredientCountMap.put(currentIngredient, 1);
            }
        }
        return ingredientCountMap.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).toList();
    }

    private static boolean apply(AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe, boolean test) {
        IItemHandler availableItems = kettle.getItemCapability();
        IFluidHandler availableFluids = kettle.getFluidCapability();
        IGasHandler availableGases = kettle.getGasCapability();
        if (!recipe.temperatureCondition.test(kettle.getCore().getStructureManager().getTemperature())) {
            return false;
        }

        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
        int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];
        long[] extractedGasesFromTank = new long[availableGases.getTanks()];
        if (!planInputConsumption(recipe, availableItems, availableFluids, availableGases, extractedItemsFromSlot, extractedFluidsFromTank, extractedGasesFromTank)) {
            return false;
        }

        List<ItemStack> recipeOutputItems = createRecipeOutputItems(recipe, level, availableItems, extractedItemsFromSlot, !test);
        List<FluidStack> recipeOutputFluids = createRecipeOutputFluids(recipe);
        List<GasStack> recipeOutputGases = createRecipeOutputGases(recipe);
        if (!canAcceptOutputsAfterInputsAreConsumed(kettle, availableItems, availableFluids, availableGases, recipeOutputItems, recipeOutputFluids, recipeOutputGases, extractedItemsFromSlot, extractedFluidsFromTank, extractedGasesFromTank)) {
            return false;
        }

        if (test) {
            return true;
        }

        executePlannedConsumption(kettle, availableItems, availableFluids, availableGases, extractedItemsFromSlot, extractedFluidsFromTank, extractedGasesFromTank);
        return kettle.acceptOutputs(recipeOutputItems, recipeOutputFluids, recipeOutputGases, false);
    }

    private static boolean planInputConsumption(ReactorKettleRecipe recipe, IItemHandler availableItems, IFluidHandler availableFluids, IGasHandler availableGases, int[] extractedItemsFromSlot, int[] extractedFluidsFromTank, long[] extractedGasesFromTank) {
        List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList());
        ingredients.sort(Comparator.comparingInt(ingredient -> getMatchingItemCount(availableItems, ingredient)));
        if (!planItemInputConsumption(ingredients, 0, availableItems, extractedItemsFromSlot)) {
            return false;
        }

        List<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        List<SizedGasIngredient> gasIngredients = recipe.getGasIngredients();

        FluidIngredients:
        for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
            int amountRequired = fluidIngredient.amount();
            for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
                FluidStack fluidStack = availableFluids.getFluidInTank(tank);
                if (!fluidIngredient.test(fluidStack)) {
                    continue;
                }

                int availableAmount = fluidStack.getAmount() - extractedFluidsFromTank[tank];
                if (availableAmount <= 0) {
                    continue;
                }

                int drainedAmount = Math.min(amountRequired, availableAmount);
                extractedFluidsFromTank[tank] += drainedAmount;
                amountRequired -= drainedAmount;
                if (amountRequired != 0) {
                    continue;
                }

                continue FluidIngredients;
            }

            return false;
        }

        GasIngredients:
        for (SizedGasIngredient gasIngredient : gasIngredients) {
            long amountRequired = gasIngredient.amount();
            for (int tank = 0; tank < availableGases.getTanks(); tank++) {
                GasStack gasStack = availableGases.getGasInTank(tank);
                if (!gasIngredient.test(gasStack)) {
                    continue;
                }

                long availableAmount = gasStack.getAmount() - extractedGasesFromTank[tank];
                if (availableAmount <= 0) {
                    continue;
                }

                long drainedAmount = Math.min(amountRequired, availableAmount);
                extractedGasesFromTank[tank] += drainedAmount;
                amountRequired -= drainedAmount;
                if (amountRequired != 0) {
                    continue;
                }

                continue GasIngredients;
            }

            return false;
        }

        return true;
    }

    private static List<ItemStack> createRecipeOutputItems(ReactorKettleRecipe recipe, Level level, IItemHandler availableItems, int[] extractedItemsFromSlot, boolean rollRandomOutputs) {
        List<ItemStack> recipeOutputItems = new ArrayList<>();
        if (rollRandomOutputs) {
            recipe.rollResults(level.random).stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).forEach(recipeOutputItems::add);
        }
        else {
            recipe.getRollableResults().stream().map(output -> output.getStack().copy()).filter(stack -> !stack.isEmpty()).forEach(recipeOutputItems::add);
        }
        CraftingInput remainderInput = new DummyCraftingContainer(availableItems, extractedItemsFromSlot).asCraftInput();
        recipe.getRemainingItems(remainderInput).stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).forEach(recipeOutputItems::add);
        return recipeOutputItems;
    }

    private static List<FluidStack> createRecipeOutputFluids(ReactorKettleRecipe recipe) {
        List<FluidStack> recipeOutputFluids = new ArrayList<>();
        recipe.getFluidResults().stream().filter(fluidStack -> !fluidStack.isEmpty()).map(FluidStack::copy).forEach(recipeOutputFluids::add);
        return recipeOutputFluids;
    }

    private static List<GasStack> createRecipeOutputGases(ReactorKettleRecipe recipe) {
        List<GasStack> recipeOutputGases = new ArrayList<>();
        recipe.getGasResults().stream().filter(gasStack -> !gasStack.isEmpty()).map(GasStack::copy).forEach(recipeOutputGases::add);
        return recipeOutputGases;
    }

    private static boolean canAcceptOutputsAfterInputsAreConsumed(AirtightReactorKettleBlockEntity kettle, IItemHandler availableItems, IFluidHandler availableFluids, IGasHandler availableGases, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, int[] extractedItemsFromSlot, int[] extractedFluidsFromTank, long[] extractedGasesFromTank) {
        IItemHandler outputInventory = kettle.getInventories().getSecond();
        IFluidHandler outputFluidTank = kettle.getFluidTanks().getSecond().getCapability();
        IGasHandler outputGasTank = kettle.getGasTanks().getSecond().getCapability();
        return canAcceptItemOutputsAfterInputsAreConsumed(availableItems, outputInventory, outputItems, extractedItemsFromSlot) && canAcceptFluidOutputsAfterInputsAreConsumed(availableFluids, outputFluidTank, outputFluids, extractedFluidsFromTank) && canAcceptGasOutputsAfterInputsAreConsumed(availableGases, outputGasTank, outputGases, extractedGasesFromTank);
    }

    private static boolean canAcceptItemOutputsAfterInputsAreConsumed(IItemHandler availableItems, IItemHandler outputInventory, List<ItemStack> outputItems, int[] extractedItemsFromSlot) {
        if (outputItems.isEmpty()) {
            return true;
        }

        ItemStack[] simulatedSlots = new ItemStack[outputInventory.getSlots()];
        int outputOffset = availableItems.getSlots() - outputInventory.getSlots();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            ItemStack stack = outputInventory.getStackInSlot(slot).copy();
            int combinedSlot = outputOffset + slot;
            if (combinedSlot >= 0 && combinedSlot < extractedItemsFromSlot.length && extractedItemsFromSlot[combinedSlot] > 0) {
                stack.shrink(extractedItemsFromSlot[combinedSlot]);
            }
            simulatedSlots[slot] = stack;
        }
        for (ItemStack outputItem : outputItems) {
            if (!insertItemIntoSimulatedInventory(simulatedSlots, outputInventory, outputItem.copy())) {
                return false;
            }
        }

        return true;
    }

    private static boolean insertItemIntoSimulatedInventory(ItemStack[] simulatedSlots, IItemHandler targetInventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < simulatedSlots.length; slot++) {
            if (remaining.isEmpty()) {
                return true;
            }

            ItemStack slotStack = simulatedSlots[slot];
            if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, remaining) || !targetInventory.isItemValid(slot, remaining)) {
                continue;
            }

            int limit = Math.min(targetInventory.getSlotLimit(slot), slotStack.getMaxStackSize());
            int inserted = Math.min(remaining.getCount(), limit - slotStack.getCount());
            if (inserted <= 0) {
                continue;
            }

            slotStack.grow(inserted);
            remaining.shrink(inserted);
        }

        if (remaining.isEmpty()) {
            return true;
        }
        if (hasSameItem(simulatedSlots, remaining)) {
            return false;
        }

        int firstEmptySlot = -1;
        for (int slot = 0; slot < simulatedSlots.length; slot++) {
            if (!simulatedSlots[slot].isEmpty()) {
                continue;
            }

            firstEmptySlot = slot;
            break;
        }

        if (firstEmptySlot == -1 || !targetInventory.isItemValid(firstEmptySlot, remaining)) {
            return false;
        }

        int limit = Math.min(targetInventory.getSlotLimit(firstEmptySlot), remaining.getMaxStackSize());
        int inserted = Math.min(remaining.getCount(), limit);
        if (inserted <= 0) {
            return false;
        }

        ItemStack insertedStack = remaining.copy();
        insertedStack.setCount(inserted);
        simulatedSlots[firstEmptySlot] = insertedStack;
        remaining.shrink(inserted);
        return remaining.isEmpty();
    }

    private static boolean hasSameItem(ItemStack @NotNull [] simulatedSlots, ItemStack stack) {
        for (ItemStack slotStack : simulatedSlots) {
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, stack)) {
                return true;
            }
        }
        return false;
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

    private static void executePlannedConsumption(AirtightReactorKettleBlockEntity kettle, IItemHandler availableItems, IFluidHandler availableFluids, IGasHandler availableGases, int @NotNull [] extractedItemsFromSlot, int[] extractedFluidsFromTank, long[] extractedGasesFromTank) {
        for (int slot = 0; slot < extractedItemsFromSlot.length; slot++) {
            int amount = extractedItemsFromSlot[slot];
            if (amount <= 0) {
                continue;
            }

            availableItems.extractItem(slot, amount, false);
        }

        boolean fluidsAffected = false;
        for (int tank = 0; tank < extractedFluidsFromTank.length; tank++) {
            int amount = extractedFluidsFromTank[tank];
            if (amount <= 0) {
                continue;
            }

            availableFluids.getFluidInTank(tank).shrink(amount);
            fluidsAffected = true;
        }
        if (fluidsAffected) {
            kettle.getFluidTanks().forEach(tank -> tank.forEach(TankSegment::onFluidStackChanged));
        }

        boolean gasesAffected = false;
        for (int tank = 0; tank < extractedGasesFromTank.length; tank++) {
            long amount = extractedGasesFromTank[tank];
            if (amount <= 0) {
                continue;
            }

            availableGases.getGasInTank(tank).shrink(amount);
            gasesAffected = true;
        }
        if (gasesAffected) {
            kettle.getGasTanks().forEach(tank -> tank.forEach(SmartGasTankBehaviour.TankSegment::onGasStackChanged));
        }
    }

    private static boolean planItemInputConsumption(List<Ingredient> ingredients, int ingredientIndex, IItemHandler availableItems, int[] extractedItemsFromSlot) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int slot = 0; slot < availableItems.getSlots(); slot++) {
            ItemStack stackInSlot = availableItems.getStackInSlot(slot);
            if (stackInSlot.isEmpty() || stackInSlot.getCount() <= extractedItemsFromSlot[slot]) {
                continue;
            }

            ItemStack extracted = availableItems.extractItem(slot, 1, true);
            if (extracted.isEmpty() || !ingredient.test(extracted)) {
                continue;
            }

            extractedItemsFromSlot[slot]++;
            if (planItemInputConsumption(ingredients, ingredientIndex + 1, availableItems, extractedItemsFromSlot)) {
                return true;
            }

            extractedItemsFromSlot[slot]--;
        }

        return false;
    }

    private static int getMatchingItemCount(IItemHandler availableItems, Ingredient ingredient) {
        int count = 0;
        for (int slot = 0; slot < availableItems.getSlots(); slot++) {
            ItemStack stackInSlot = availableItems.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            ItemStack extractable = availableItems.extractItem(slot, stackInSlot.getCount(), true);
            if (extractable.isEmpty() || !ingredient.test(extractable)) {
                continue;
            }

            count += extractable.getCount();
        }
        return count;
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