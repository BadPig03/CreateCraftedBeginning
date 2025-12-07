package net.ty.createcraftedbeginning.api.gas.recipes;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class BaseRecipeProviderWithGas extends RecipeProvider {
    protected final String modId;
    protected final List<GeneratedRecipe> all = new ArrayList<>();

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
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        all.forEach(c -> c.register(recipeOutput));
    }

    @FunctionalInterface
    public interface GeneratedRecipe {
        void register(RecipeOutput recipeOutput);
    }
}
