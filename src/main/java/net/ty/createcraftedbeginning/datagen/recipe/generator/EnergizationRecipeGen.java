package net.ty.createcraftedbeginning.datagen.recipe.generator;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.EnergizationRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipeGen;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class EnergizationRecipeGen extends StandardGasProcessingRecipeGen<EnergizationRecipe> {
    public EnergizationRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected CCBRecipeTypes getRecipeType() {
        return CCBRecipeTypes.ENERGIZATION;
    }
}
