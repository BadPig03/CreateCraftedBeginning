package net.ty.createcraftedbeginning.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IAssemblyRecipeWithGas;
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory.AssemblyInjecting;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GasInjectionRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> implements IAssemblyRecipeWithGas {
    public GasInjectionRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.GAS_INJECTION, params);
    }

    public static ItemStack getResultItem(Level level, ItemStack itemStack, GasStack gasStack) {
        if (level == null) {
            return ItemStack.EMPTY;
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class, matchItemAndGas(level, gasStack, input));
        if (assemblyRecipe.isPresent()) {
            SizedGasIngredient requiredGas = assemblyRecipe.get().value().getRequiredGas();
            GasStack requiredGasStack = requiredGas.getFirstGas();
            if (GasStack.isSameGasSameComponents(requiredGasStack, gasStack)) {
                List<ItemStack> results = assemblyRecipe.get().value().rollResults(level.random);
                return results.getFirst();
            }
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();
            if (!recipe.getIngredientsItem().test(itemStack)) {
                continue;
            }

            SizedGasIngredient gasIngredient = recipe.getIngredientsGas();
            GasStack ingredientGasStack = gasIngredient.getFirstGas();
            if (!GasStack.isSameGasSameComponents(ingredientGasStack, gasStack)) {
                continue;
            }

            return recipe.rollResults(level.random).getFirst();
        }

        return ItemStack.EMPTY;
    }

    @Contract(pure = true)
    private static @NotNull Predicate<RecipeHolder<GasInjectionRecipe>> matchItemAndGas(Level level, GasStack gasStack, SingleRecipeInput input) {
        return r -> r.value().matches(input, level) && GasStack.isSameGasSameComponents(r.value().getRequiredGas().getFirstGas(), gasStack);
    }

    public static long getRequiredGasAmountForItem(Level level, ItemStack itemStack, GasStack gasStack) {
        if (level == null) {
            return -1;
        }

        if (GasCanisterQueryUtils.isCanisterInjectable(itemStack, gasStack)) {
            long maxCapacity = GasInjectionChamberBlockEntity.getMaxCapacity();
            GasStack content = GasCanisterQueryUtils.getCanisterContent(itemStack);
            long capacity = GasCanisterQueryUtils.getCanisterCapacity(itemStack, content.getGas());
            return Math.min(maxCapacity, capacity - content.getAmount());
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class, matchItemAndGas(level, gasStack, input));
        if (assemblyRecipe.isPresent()) {
            SizedGasIngredient requiredGas = assemblyRecipe.get().value().getRequiredGas();
            GasStack requiredGasStack = requiredGas.getFirstGas();
            if (GasStack.isSameGasSameComponents(requiredGasStack, gasStack)) {
                return requiredGas.amount();
            }
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();
            if (!recipe.getIngredientsItem().test(itemStack)) {
                continue;
            }

            SizedGasIngredient gasIngredient = recipe.getIngredientsGas();
            GasStack ingredientGasStack = gasIngredient.getFirstGas();
            if (!GasStack.isSameGasSameComponents(ingredientGasStack, gasStack)) {
                continue;
            }

            return ingredientGasStack.getAmount();
        }

        return -1;
    }

    public static boolean isItemInvalidForInjection(Level level, ItemStack itemStack, GasStack gasStack) {
        if (level == null) {
            return true;
        }
        if (GasCanisterQueryUtils.isCanisterInjectable(itemStack, gasStack)) {
            return false;
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class);
        if (assemblyRecipe.isPresent()) {
            return false;
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();
            Ingredient ingredient = recipe.getIngredientsItem();
            if (ingredient.test(itemStack)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput inv, @NotNull Level level) {
        return ingredients.getFirst().test(inv.getItem(0));
    }

    public SizedGasIngredient getRequiredGas() {
        if (gasIngredients.isEmpty()) {
            throw new IllegalStateException("Filling Recipe has no gas ingredient!");
        }

        return gasIngredients.getFirst();
    }

    public Ingredient getIngredientsItem() {
        return ingredients.getFirst();
    }

    public SizedGasIngredient getIngredientsGas() {
        return gasIngredients.getFirst();
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected int getMaxGasInputCount() {
        return 1;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        return CCBLang.translateDirect("recipe.assembly.gas_injection_injecting_gas", gasIngredients.getFirst().getFirstGas().getHoverName().getString());
    }

    @Override
    public void addRequiredMachines(@NotNull Set<ItemLike> list) {
        list.add(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.get());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public void addAssemblyGasIngredients(@NotNull List<SizedGasIngredient> list) {
        list.add(getRequiredGas());
    }

    @Override
    public Supplier<Supplier<SequencedAssemblyWithGasSubCategory>> getJEISubCategory() {
        return () -> AssemblyInjecting::new;
    }
}