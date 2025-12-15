package net.ty.createcraftedbeginning.recipe.generators;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipeGen;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import java.util.concurrent.CompletableFuture;

public abstract class GasInjectionRecipeGen extends StandardProcessingWithGasRecipeGen<GasInjectionRecipe> {
    public GasInjectionRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected CCBRecipeTypes getRecipeType() {
        return CCBRecipeTypes.GAS_INJECTION;
    }
}
