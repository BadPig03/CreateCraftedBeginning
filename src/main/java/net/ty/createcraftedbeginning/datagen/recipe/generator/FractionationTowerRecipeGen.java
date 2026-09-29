package net.ty.createcraftedbeginning.datagen.recipe.generator;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipeParams;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeGen;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class FractionationTowerRecipeGen extends GasProcessingRecipeGen<FractionationTowerRecipeParams, FractionationTowerRecipe, Builder> {
    protected FractionationTowerRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected Builder getBuilder(ResourceLocation id) {
        return new Builder(id);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return CCBRecipeTypes.FRACTIONATION_TOWER;
    }
}
