package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterItem;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ReactorKettleRecipe extends StandardProcessingWithGasRecipe<RecipeInput> {
    public ReactorKettleRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.REACTOR_KETTLE, params);
    }

    public static boolean match(@NotNull AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe) {
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

    private static boolean apply(@NotNull AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe, boolean test) {
        IItemHandler availableItems = kettle.getItemCapability();
        IFluidHandler availableFluids = kettle.getFluidCapability();
        IGasHandler availableGases = kettle.getGasCapability();
        if (availableItems == null || availableFluids == null || availableGases == null) {
            return false;
        }
        if (!recipe.temperatureCondition.test(kettle.getCore().getStructureManager().getTemperature())) {
            return false;
        }

        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        List<ItemStack> recipeOutputItems = new ArrayList<>();
        List<FluidStack> recipeOutputFluids = new ArrayList<>();
        List<GasStack> recipeOutputGases = new ArrayList<>();
        List<Ingredient> ingredients = new LinkedList<>(recipe.getIngredients());
        List<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        List<SizedGasIngredient> gasIngredients = recipe.getGasIngredients();
        for (boolean simulate : Iterate.trueAndFalse) {
            if (!simulate && test) {
                return true;
            }

            int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
            int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];
            long[] extractedGasesFromTank = new long[availableGases.getTanks()];

            Ingredients:
            for (Ingredient ingredient : ingredients) {
                for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                    if (simulate && availableItems.getStackInSlot(slot).getCount() <= extractedItemsFromSlot[slot]) {
                        continue;
                    }

                    ItemStack extracted = availableItems.extractItem(slot, 1, true);
                    if (!ingredient.test(extracted)) {
                        continue;
                    }

                    if (!simulate) {
                        availableItems.extractItem(slot, 1, false);
                    }
                    extractedItemsFromSlot[slot]++;
                    continue Ingredients;
                }
                return false;
            }

            boolean fluidsAffected = false;
            FluidIngredients:
            for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
                int amountRequired = fluidIngredient.amount();
                for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
                    FluidStack fluidStack = availableFluids.getFluidInTank(tank);
                    if (simulate && fluidStack.getAmount() <= extractedFluidsFromTank[tank] || !fluidIngredient.test(fluidStack)) {
                        continue;
                    }

                    int drainedAmount = Math.min(amountRequired, fluidStack.getAmount());
                    if (!simulate) {
                        fluidStack.shrink(drainedAmount);
                        fluidsAffected = true;
                    }
                    amountRequired -= drainedAmount;
                    if (amountRequired != 0) {
                        continue;
                    }

                    extractedFluidsFromTank[tank] += drainedAmount;
                    continue FluidIngredients;
                }
                return false;
            }
            if (fluidsAffected) {
                kettle.getFluidTanks().forEach(tank -> tank.forEach(TankSegment::onFluidStackChanged));
            }

            boolean gasesAffected = false;
            GasIngredients:
            for (SizedGasIngredient gasIngredient : gasIngredients) {
                long amountRequired = gasIngredient.amount();
                for (int tank = 0; tank < availableGases.getTanks(); tank++) {
                    GasStack gasStack = availableGases.getGasInTank(tank);
                    if (simulate && gasStack.getAmount() <= extractedGasesFromTank[tank] || !gasIngredient.test(gasStack)) {
                        continue;
                    }

                    long drainedAmount = Math.min(amountRequired, gasStack.getAmount());
                    if (!simulate) {
                        gasStack.shrink(drainedAmount);
                        gasesAffected = true;
                    }
                    amountRequired -= drainedAmount;
                    if (amountRequired != 0) {
                        continue;
                    }

                    extractedGasesFromTank[tank] += drainedAmount;
                    continue GasIngredients;
                }
                return false;
            }
            if (gasesAffected) {
                kettle.getGasTanks().forEach(tank -> tank.forEach(SmartGasTankBehaviour.TankSegment::onGasStackChanged));
            }

            if (simulate) {
                CraftingInput remainderInput = new DummyCraftingContainer(availableItems, extractedItemsFromSlot).asCraftInput();
                recipeOutputItems.addAll(recipe.rollResults(kettle.getLevel().random));
                recipe.getRemainingItems(remainderInput).stream().filter(stack -> !stack.isEmpty()).forEach(recipeOutputItems::add);
                recipe.getFluidResults().stream().filter(fluidStack -> !fluidStack.isEmpty()).forEach(recipeOutputFluids::add);
                recipe.getGasResults().stream().filter(gasStack -> !gasStack.isEmpty()).forEach(recipeOutputGases::add);
            }

            if (!kettle.acceptOutputs(recipeOutputItems, recipeOutputFluids, recipeOutputGases, simulate)) {
                return false;
            }
        }

        return true;
    }

    public static boolean areIngredientsEqual(@NotNull Ingredient ing1, @NotNull Ingredient ing2) {
        ItemStack[] stacks1 = ing1.getItems();
        ItemStack[] stacks2 = ing2.getItems();
        if (stacks1.length != stacks2.length) {
            return false;
        }

        for (int i = 0; i < stacks1.length; i++) {
            if (!ItemStack.matches(stacks1[i], stacks2[i])) {
                return false;
            }
        }

        return true;
    }

    public static @NotNull @Unmodifiable List<Pair<Ingredient, Integer>> getCondensedIngredients(@NotNull NonNullList<Ingredient> recipeIngredients) {
        Map<Ingredient, Integer> ingredientCountMap = new HashMap<>();
        for (Ingredient currentIngredient : recipeIngredients) {
            if (currentIngredient.isEmpty()) {
                continue;
            }

            boolean found = false;
            for (Entry<Ingredient, Integer> entry : ingredientCountMap.entrySet()) {
                Ingredient existingIngredient = entry.getKey();
                if (!areIngredientsEqual(existingIngredient, currentIngredient)) {
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
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    public boolean matches(@NotNull RecipeInput input, @NotNull Level level) {
        return false;
    }
}
