package net.ty.createcraftedbeginning.api.gas.recipes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BaseRecipeProviderWithGas extends RecipeProvider {
    protected final String modId;
    protected final List<GeneratedRecipe> all = new ArrayList<>();

    /**
     * Creates a new {@code BaseRecipeProviderWithGas} instance.
     *
     * @param output     the output to add or process
     * @param registries the registries to use
     * @param modId      the mod identifier to test
     */
    public BaseRecipeProviderWithGas(PackOutput output, CompletableFuture<Provider> registries, String modId) {
        super(output, registries);
        this.modId = modId;
    }

    protected ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(modId, path);
    }

    protected GeneratedRecipe register(GeneratedRecipe recipe) {
        all.add(recipe);
        return recipe;
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        all.forEach(recipe -> recipe.register(recipeOutput));
    }

    @FunctionalInterface
    public interface GeneratedRecipe {
        /**
         * Registers the supplied value with the appropriate API registry.
         *
         * @param recipeOutput the recipe output to use
         */
        void register(RecipeOutput recipeOutput);
    }
}
