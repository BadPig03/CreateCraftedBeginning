package net.ty.createcraftedbeginning.api.gas.recipes;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ProcessingWithGasRecipeGen<P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>, B extends ProcessingWithGasRecipeBuilder<P, R, B>> extends BaseRecipeProviderWithGas {
    /**
     * Creates a new {@code ProcessingWithGasRecipeGen} instance.
     *
     * @param output           the output to add or process
     * @param registries       the registries to use
     * @param defaultNamespace the default namespace to use
     */
    public ProcessingWithGasRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) {
        return create(CCBAPI.MOD_ID, singleIngredient, transform);
    }

    protected GeneratedRecipe create(String namespace, Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) {
        GeneratedRecipe recipe = output -> {
            ItemLike item = singleIngredient.get();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, RegisteredObjectsHelper.getKeyOrThrow(item.asItem()).getPath());
            B builder = getBuilder(id).withItemIngredients(Ingredient.of(item));
            transform.apply(builder).build(output);
        };
        all.add(recipe);
        return recipe;
    }

    protected abstract B getBuilder(ResourceLocation id);

    protected GeneratedRecipe create(String name, UnaryOperator<B> transform) {
        return create(asResource(name), transform);
    }

    protected GeneratedRecipe create(ResourceLocation name, UnaryOperator<B> transform) {
        return createWithDeferredId(() -> name, transform);
    }

    protected GeneratedRecipe createWithDeferredId(Supplier<ResourceLocation> name, UnaryOperator<B> transform) {
        GeneratedRecipe recipe = output -> transform.apply(getBuilder(name.get())).build(output);
        all.add(recipe);
        return recipe;
    }

    protected abstract IRecipeTypeInfo getRecipeType();
}
