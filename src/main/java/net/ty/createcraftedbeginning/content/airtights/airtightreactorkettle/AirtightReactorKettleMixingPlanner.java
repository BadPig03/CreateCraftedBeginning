package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity.CraftPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleMixingPlanner {
    private AirtightReactorKettleMixingPlanner() {
    }

    @Internal
    public static Optional<CraftPlan> planCraftingRecipe(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        return planInputs(kettle, recipe).map(plan -> kettle.createCraftPlan(plan.itemAmounts(), new int[kettle.getAvailableFluids().getTanks()], GasConsumptionPlan.empty(kettle.getAvailableGases().getTanks()), plan.outputs(), List.of(), List.of()));
    }

    static boolean matches(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        return planInputs(kettle, recipe).isPresent();
    }

    private static Optional<MixingPlan> planInputs(AirtightReactorKettleBlockEntity kettle, CraftingRecipe recipe) {
        Level level = kettle.getLevel();
        IItemHandler inputInventory = kettle.getInventories().getFirst();
        if (level == null || !(recipe instanceof ShapelessRecipe)) {
            return Optional.empty();
        }

        int[] itemConsumptionBySlot = new int[inputInventory.getSlots()];
        List<ItemStack> craftingInputStacks = new ArrayList<>();
        List<Ingredient> ingredients = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }

            ingredients.add(ingredient);
        }
        if (ingredients.isEmpty() || ingredients.size() > 9) {
            return Optional.empty();
        }

        ingredients.sort(Comparator.comparingInt(ingredient -> {
            int count = 0;
            for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
                ItemStack stack = inputInventory.getStackInSlot(slot);
                if (!(!stack.isEmpty() && ingredient.test(stack))) {
                    continue;
                }

                count += stack.getCount();
            }
            return count;
        }));
        if (!planCraftingInputConsumption(ingredients, 0, inputInventory, itemConsumptionBySlot, craftingInputStacks)) {
            return Optional.empty();
        }

        NonNullList<ItemStack> inputSlots = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int slot = 0; slot < craftingInputStacks.size(); slot++) {
            inputSlots.set(slot, craftingInputStacks.get(slot).copyWithCount(1));
        }
        CraftingInput craftingInput = CraftingInput.of(3, 3, inputSlots);
        if (!recipe.matches(craftingInput, level)) {
            return Optional.empty();
        }

        ItemStack result = recipe.assemble(craftingInput, level.registryAccess());
        if (result.isEmpty()) {
            return Optional.empty();
        }

        List<ItemStack> craftingOutputs = new ArrayList<>();
        craftingOutputs.add(result.copy());
        for (ItemStack remaining : recipe.getRemainingItems(craftingInput)) {
            if (remaining.isEmpty()) {
                continue;
            }

            craftingOutputs.add(remaining.copy());
        }
        if (!kettle.testRecipeFilter(craftingOutputs.getFirst()) || !kettle.acceptOutputs(craftingOutputs, List.of(), List.of())) {
            return Optional.empty();
        }

        int[] itemConsumptionAmounts = new int[kettle.getAvailableItems().getSlots()];
        System.arraycopy(itemConsumptionBySlot, 0, itemConsumptionAmounts, 0, itemConsumptionBySlot.length);
        return Optional.of(new MixingPlan(itemConsumptionAmounts, craftingOutputs));
    }

    private static boolean planCraftingInputConsumption(List<Ingredient> ingredients, int ingredientIndex, IItemHandler inputInventory, int[] itemConsumptionBySlot, List<ItemStack> craftingInputStacks) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
            ItemStack storedStack = inputInventory.getStackInSlot(slot);
            if (storedStack.isEmpty() || storedStack.getCount() <= itemConsumptionBySlot[slot] || !ingredient.test(storedStack)) {
                continue;
            }

            itemConsumptionBySlot[slot]++;
            craftingInputStacks.add(storedStack.copyWithCount(1));
            if (planCraftingInputConsumption(ingredients, ingredientIndex + 1, inputInventory, itemConsumptionBySlot, craftingInputStacks)) {
                return true;
            }

            craftingInputStacks.removeLast();
            itemConsumptionBySlot[slot]--;
        }
        return false;
    }

    private record MixingPlan(int[] itemAmounts, List<ItemStack> outputs) {
    }
}
