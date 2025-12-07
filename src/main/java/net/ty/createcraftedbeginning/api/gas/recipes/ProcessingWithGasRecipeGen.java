package net.ty.createcraftedbeginning.api.gas.recipes;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public abstract class ProcessingWithGasRecipeGen<P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>, B extends ProcessingWithGasRecipeBuilder<P, R, B>> extends BaseRecipeProviderWithGas {
    public ProcessingWithGasRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) {
        return create(CreateCraftedBeginning.MOD_ID, singleIngredient, transform);
    }

    protected GeneratedRecipe create(String namespace, Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) {
        GeneratedRecipe generatedRecipe = output -> {
            ItemLike itemLike = singleIngredient.get();
            transform.apply(getBuilder(ResourceLocation.fromNamespaceAndPath(namespace, RegisteredObjectsHelper.getKeyOrThrow(itemLike.asItem()).getPath())).withItemIngredients(Ingredient.of(itemLike))).build(output);
        };
        all.add(generatedRecipe);
        return generatedRecipe;
    }

    protected abstract B getBuilder(ResourceLocation id);

    protected GeneratedRecipe create(String name, UnaryOperator<B> transform) {
        return create(asResource(name), transform);
    }

    protected GeneratedRecipe create(ResourceLocation name, UnaryOperator<B> transform) {
        return createWithDeferredId(() -> name, transform);
    }

    protected GeneratedRecipe createWithDeferredId(Supplier<ResourceLocation> name, UnaryOperator<B> transform) {
        GeneratedRecipe generatedRecipe = output -> transform.apply(getBuilder(name.get())).build(output);
        all.add(generatedRecipe);
        return generatedRecipe;
    }

    protected abstract IRecipeTypeInfo getRecipeType();

    protected Supplier<ResourceLocation> idWithSuffix(Supplier<ItemLike> item, String suffix) {
        return () -> asResource(RegisteredObjectsHelper.getKeyOrThrow(item.get().asItem()).getPath() + suffix);
    }
}
