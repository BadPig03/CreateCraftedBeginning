package net.ty.createcraftedbeginning.compat.jei;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
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
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.recipe.gas.GasAwareRecipe;
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
        if (unwrappedRecipe instanceof GasAwareRecipe gasRecipe) {
            return gasRecipeMatches(gasRecipe, focus);
        }

        return unwrappedRecipe instanceof SequencedAssemblyRecipe sequencedRecipe && sequencedRecipe.getSequence().stream().anyMatch(step -> step.getRecipe() instanceof GasAwareRecipe gasRecipe && gasRecipeMatches(gasRecipe, focus));
    }

    private static boolean gasRecipeMatches(GasAwareRecipe recipe, GasFocus focus) {
        boolean gasInputMatches = recipe.getGasRequirements().stream().anyMatch(requirement -> requirement.ingredient().test(focus.gas().copyWithAmount(Math.max(1, requirement.amount()))));
        boolean gasOutputMatches = recipe.getGasRecipeData().results().stream().anyMatch(gasResult -> GasStack.isSameGasSameComponents(gasResult, focus.gas()));
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
        if (VirtualGasItems.isVirtualItem(itemStack)) {
            gasStack = VirtualGasItems.readGasSample(itemStack);
        }
        else {
            GasCanisterContainer canister = itemStack.getCapability(CanisterCapabilities.ITEM);
            if (canister == null || canister.getTanks() != 1) {
                return null;
            }

            gasStack = canister.getGasInTank(0);
        }

        if (gasStack.isEmpty()) {
            return null;
        }

        return new GasFocus(gasStack.copyWithAmount(GasUnits.GU_PER_KGU), focus.getRole());
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
            if (!hasMatchingRecipe) {
                return;
            }

            matchingTypes.add(category.getRecipeType());
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
