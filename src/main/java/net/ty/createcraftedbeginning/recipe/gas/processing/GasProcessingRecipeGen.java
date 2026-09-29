package net.ty.createcraftedbeginning.recipe.gas.processing;

import com.simibubi.create.api.data.recipe.ProcessingRecipeGen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GasProcessingRecipeGen<P extends GasProcessingRecipeParams, R extends GasProcessingRecipe<?, P>, B extends GasProcessingRecipeBuilder<P, R, B>> extends ProcessingRecipeGen<P, R, B> {
    public GasProcessingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) {
        return create(modid, singleIngredient, transform);
    }
}
