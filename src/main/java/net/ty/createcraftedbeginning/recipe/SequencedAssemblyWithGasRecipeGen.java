package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasRecipeBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public abstract class SequencedAssemblyWithGasRecipeGen extends BaseRecipeProvider {
    public SequencedAssemblyWithGasRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    protected GeneratedRecipe create(String name, UnaryOperator<SequencedAssemblyWithGasRecipeBuilder> transform) {
        GeneratedRecipe generatedRecipe = c -> transform.apply(new SequencedAssemblyWithGasRecipeBuilder(asResource(name))).build(c);
        all.add(generatedRecipe);
        return generatedRecipe;
    }
}
