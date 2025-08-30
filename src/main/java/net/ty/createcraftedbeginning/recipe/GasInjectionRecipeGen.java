package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import java.util.concurrent.CompletableFuture;

public class GasInjectionRecipeGen extends StandardProcessingRecipeGen<GasInjectionRecipe> {
    public GasInjectionRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected CCBRecipeTypes getRecipeType() {
        return CCBRecipeTypes.GAS_INJECTION;
    }
}
