package net.ty.createcraftedbeginning.recipe.gas.processing;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.GasIngredient;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.SizedGasIngredient;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipe.Factory;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GasProcessingRecipeBuilder<P extends GasProcessingRecipeParams, R extends GasProcessingRecipe<?, P>, S extends GasProcessingRecipeBuilder<P, R, S>> extends ProcessingRecipeBuilder<P, R, S> {
    public GasProcessingRecipeBuilder(Factory<P, R> factory, ResourceLocation recipeId) {
        super(factory, recipeId);
    }

    @SuppressWarnings("unused")
    public S withGasIngredients(SizedGasIngredient... ingredients) {
        return withGasIngredients(NonNullList.of(new SizedGasIngredient(GasIngredient.empty(), GasUnits.GU_PER_KGU), ingredients));
    }

    public S withGasIngredients(NonNullList<SizedGasIngredient> ingredients) {
        params.gasRequirements.clear();
        ingredients.forEach(ingredient -> params.gasRequirements.add(GasRecipeRequirement.of(ingredient)));
        return self();
    }

    @SuppressWarnings("unused")
    public S withGasRequirements(NonNullList<GasRecipeRequirement> requirements) {
        params.gasRequirements = requirements;
        return self();
    }

    @SuppressWarnings("unused")
    public S withGasOutputs(GasStack... outputs) {
        return withGasOutputs(NonNullList.of(GasStack.EMPTY, outputs));
    }

    public S withGasOutputs(NonNullList<GasStack> outputs) {
        params.gasResults = outputs;
        return self();
    }

    public S require(Gas gasType, long amount) {
        return require(gasType, amount, PressureRequirement.NONE);
    }

    public S require(Gas gasType, long amount, PressureRequirement pressure) {
        return require(SizedGasIngredient.of(gasType, amount), pressure);
    }

    public S require(SizedGasIngredient ingredient) {
        return require(ingredient, PressureRequirement.NONE);
    }

    public S require(SizedGasIngredient ingredient, PressureRequirement pressure) {
        return require(GasRecipeRequirement.of(ingredient, pressure));
    }

    public S require(GasRecipeRequirement requirement) {
        params.gasRequirements.add(requirement);
        return self();
    }

    public S require(TagKey<Gas> gasTag, long amount) {
        return require(gasTag, amount, PressureRequirement.NONE);
    }

    public S require(TagKey<Gas> gasTag, long amount, PressureRequirement pressure) {
        return require(SizedGasIngredient.of(gasTag, amount), pressure);
    }

    public S output(Gas gasType, long amount) {
        return output(new GasStack(gasType, amount));
    }

    public S output(GasStack gasStack) {
        params.gasResults.add(gasStack);
        return self();
    }
}
