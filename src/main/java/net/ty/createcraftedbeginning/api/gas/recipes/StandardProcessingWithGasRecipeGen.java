package net.ty.createcraftedbeginning.api.gas.recipes;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe.Builder;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe.Serializer;

import java.util.concurrent.CompletableFuture;

public abstract class StandardProcessingWithGasRecipeGen<R extends StandardProcessingWithGasRecipe<?>> extends ProcessingWithGasRecipeGen<ProcessingWithGasRecipeParams, R, Builder<R>> {
    public StandardProcessingWithGasRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected Builder<R> getBuilder(ResourceLocation id) {
        return new Builder<>(getSerializer().factory(), id);
    }

    protected Serializer<R> getSerializer() {
        return getRecipeType().getSerializer();
    }
}
