package net.ty.createcraftedbeginning.recipe.generators;

import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class WindChargingRecipeGen extends BaseRecipeProvider {
    public WindChargingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    protected GeneratedRecipe create(String name, WindChargingAction action, UnaryOperator<Builder<WindChargingRecipe>> transform) {
        GeneratedRecipe recipe = consumer -> transform.apply(new Builder<>(params -> new WindChargingRecipe(params, action), asResource(name))).build(consumer);
        all.add(recipe);
        return recipe;
    }
}
