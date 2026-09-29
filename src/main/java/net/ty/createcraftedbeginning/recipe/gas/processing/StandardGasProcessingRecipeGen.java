package net.ty.createcraftedbeginning.recipe.gas.processing;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Serializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class StandardGasProcessingRecipeGen<R extends StandardGasProcessingRecipe<?>> extends GasProcessingRecipeGen<GasProcessingRecipeParams, R, Builder<R>> {
    public StandardGasProcessingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
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
