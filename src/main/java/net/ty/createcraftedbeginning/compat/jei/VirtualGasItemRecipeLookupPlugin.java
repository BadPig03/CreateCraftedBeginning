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
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
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
        Object unwrappedRecipe = recipeObject instanceof RecipeHolder<?> holder ? holder.value() : recipeObject;
        if (unwrappedRecipe instanceof ProcessingWithGasRecipe<?, ?> gasRecipe) {
            return processingRecipeMatches(gasRecipe, focus);
        }
        return unwrappedRecipe instanceof SequencedAssemblyWithGasRecipe sequencedRecipe && sequencedRecipe.getSequence().stream().anyMatch(step -> processingRecipeMatches(step.getRecipe(), focus));
    }

    private static boolean processingRecipeMatches(ProcessingWithGasRecipe<?, ?> recipe, GasFocus focus) {
        boolean gasInputMatches = recipe.getGasIngredients().stream().anyMatch(ingredient -> ingredient.ingredient().test(focus.gas().copyWithAmount(Math.max(1, ingredient.amount()))));
        boolean gasOutputMatches = recipe.getGasResults().stream().anyMatch(gasResult -> GasStack.isSameGasSameComponents(gasResult, focus.gas()));
        return switch (focus.role()) {
            case INPUT -> gasInputMatches;
            case OUTPUT -> gasOutputMatches;
            default -> gasInputMatches || gasOutputMatches;
        };
    }

    private static @Nullable GasFocus readGasFocus(IFocus<?> focus) {
        Optional<ItemStack> focusedStack = focus.getTypedValue().getItemStack();
        if (focusedStack.isEmpty()) {
            return null;
        }

        ItemStack itemStack = focusedStack.get();
        GasStack gasStack;
        if (GasVirtualUtils.isVirtualItem(itemStack)) {
            gasStack = GasVirtualUtils.getGasType(itemStack);
        }
        else {
            IGasCanisterContainer canister = itemStack.getCapability(GasHandler.ITEM);
            if (canister == null || canister.getTanks() != 1) {
                return null;
            }
            gasStack = canister.getGasInTank(0);
        }

        if (gasStack.isEmpty()) {
            return null;
        }
        return new GasFocus(gasStack.copyWithAmount(FluidType.BUCKET_VOLUME), focus.getRole());
    }

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        GasFocus gasFocus = readGasFocus(focus);
        if (gasFocus == null) {
            return List.of();
        }

        IJeiRuntime runtime = runtimeSupplier.get();
        if (runtime == null) {
            return List.of();
        }

        List<RecipeType<?>> matchingTypes = new ArrayList<>();
        runtime.getRecipeManager().createRecipeCategoryLookup().get().forEach(category -> {
            boolean hasMatchingRecipe = runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).get().anyMatch(recipe -> recipeMatches(recipe, gasFocus));
            if (hasMatchingRecipe) {
                matchingTypes.add(category.getRecipeType());
            }
        });

        return matchingTypes;
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        GasFocus gasFocus = readGasFocus(focus);
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