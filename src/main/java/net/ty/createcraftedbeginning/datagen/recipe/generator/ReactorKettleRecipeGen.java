package net.ty.createcraftedbeginning.datagen.recipe.generator;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe.Serializer;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipeParams;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeGen;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ReactorKettleRecipeGen extends GasProcessingRecipeGen<ReactorKettleRecipeParams, ReactorKettleRecipe, Builder<ReactorKettleRecipe>> {
    public ReactorKettleRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected Builder<ReactorKettleRecipe> getBuilder(ResourceLocation id) {
        return new Builder<>(getSerializer().factory(), id);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return CCBRecipeTypes.REACTOR_KETTLE;
    }

    protected Serializer<ReactorKettleRecipe> getSerializer() {
        return getRecipeType().getSerializer();
    }
}
