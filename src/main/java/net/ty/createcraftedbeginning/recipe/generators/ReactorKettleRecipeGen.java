package net.ty.createcraftedbeginning.recipe.generators;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipeGen;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import java.util.concurrent.CompletableFuture;

public abstract class ReactorKettleRecipeGen extends StandardProcessingWithGasRecipeGen<ReactorKettleRecipe> {
    public ReactorKettleRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return CCBRecipeTypes.REACTOR_KETTLE;
    }
}
