package net.ty.createcraftedbeginning.recipe.generators;

import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.recipe.FreezingRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import java.util.concurrent.CompletableFuture;

public abstract class FreezingRecipeGen extends StandardProcessingRecipeGen<FreezingRecipe> {
    public FreezingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected CCBRecipeTypes getRecipeType() {
        return CCBRecipeTypes.FREEZING;
    }
}
