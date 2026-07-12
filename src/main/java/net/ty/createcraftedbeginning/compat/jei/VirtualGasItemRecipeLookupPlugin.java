package net.ty.createcraftedbeginning.compat.jei;

import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VirtualGasItemRecipeLookupPlugin implements IRecipeManagerPlugin {
    private final Supplier<IJeiRuntime> runtimeSupplier;

    public VirtualGasItemRecipeLookupPlugin(IJeiHelpers ignored, Supplier<IJeiRuntime> runtimeSupplier) {
        this.runtimeSupplier = runtimeSupplier;
    }

    private static boolean recipeMatches(Object recipeObject, GasFocus focus) {
        Object recipe = recipeObject instanceof RecipeHolder<?> holder ? holder.value() : recipeObject;
        if (recipe instanceof ProcessingWithGasRecipe<?, ?> gasRecipe) {
            return processingRecipeMatches(gasRecipe, focus);
        }
        return recipe instanceof SequencedAssemblyWithGasRecipe sequencedRecipe && sequencedRecipe.getSequence().stream().anyMatch(step -> processingRecipeMatches(step.getRecipe(), focus));
    }

    private static boolean processingRecipeMatches(ProcessingWithGasRecipe<?, ?> recipe, GasFocus focus) {
        boolean matchesInput = recipe.getGasIngredients().stream().anyMatch(ingredient -> ingredient.ingredient().test(focus.gas().copyWithAmount(Math.max(1, ingredient.amount()))));
        boolean matchesOutput = recipe.getGasResults().stream().anyMatch(result -> GasStack.isSameGasSameComponents(result, focus.gas()));
        RecipeIngredientRole role = focus.role();
        if (role == RecipeIngredientRole.INPUT) {
            return matchesInput;
        }
        else if (role == RecipeIngredientRole.OUTPUT) {
            return matchesOutput;
        }
        return matchesInput || matchesOutput;
    }

    private static @Nullable GasFocus readVirtualGasFocus(IFocus<?> focus) {
        Optional<ItemStack> focusedStack = focus.getTypedValue().getItemStack();
        if (focusedStack.isEmpty()) {
            return null;
        }

        ItemStack stack = focusedStack.get();
        if (!GasVirtualUtils.isVirtualItem(stack)) {
            return null;
        }

        GasStack gas = GasVirtualUtils.getGasType(stack);
        if (gas.isEmpty()) {
            return null;
        }

        return new GasFocus(gas.copyWithAmount(FluidType.BUCKET_VOLUME), focus.getRole());
    }

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        GasFocus gasFocus = readVirtualGasFocus(focus);
        if (gasFocus == null) {
            return List.of();
        }

        IJeiRuntime runtime = runtimeSupplier.get();
        if (runtime == null) {
            return List.of();
        }

        List<RecipeType<?>> matchingTypes = new ArrayList<>();
        runtime.getRecipeManager().createRecipeCategoryLookup().get().forEach(category -> {
            if (runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).get().anyMatch(recipe -> recipeMatches(recipe, gasFocus))) {
                matchingTypes.add(category.getRecipeType());
            }
        });

        return matchingTypes;
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        GasFocus gasFocus = readVirtualGasFocus(focus);
        if (gasFocus == null) {
            return List.of();
        }

        IJeiRuntime runtime = runtimeSupplier.get();
        if (runtime == null) {
            return List.of();
        }

        return runtime.getRecipeManager().createRecipeLookup(recipeCategory.getRecipeType()).get().filter(recipe -> recipeMatches(recipe, gasFocus)).toList();
    }

    @Override
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
        return List.of();
    }

    private record GasFocus(GasStack gas, RecipeIngredientRole role) {}
}