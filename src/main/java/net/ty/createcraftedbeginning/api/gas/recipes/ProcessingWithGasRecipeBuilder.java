package net.ty.createcraftedbeginning.api.gas.recipes;

import com.google.common.base.Joiner;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.data.SimpleDatagenIngredient;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe.Factory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class ProcessingWithGasRecipeBuilder<P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>, S extends ProcessingWithGasRecipeBuilder<P, R, S>> {
    protected ResourceLocation recipeId;
    protected Factory<P, R> factory;
    protected P params;
    protected List<ICondition> recipeConditions;

    public ProcessingWithGasRecipeBuilder(Factory<P, R> factory, ResourceLocation recipeId) {
        this.recipeId = recipeId;
        this.factory = factory;
        params = createParams();
        recipeConditions = new ArrayList<>();
    }

    protected abstract P createParams();

    public S withItemIngredients(Ingredient... ingredients) {
        return withItemIngredients(NonNullList.of(Ingredient.EMPTY, ingredients));
    }

    public S withItemIngredients(NonNullList<Ingredient> ingredients) {
        params.ingredients = ingredients;
        return self();
    }

    public abstract S self();

    public S withSingleItemOutput(ItemStack output) {
        return withItemOutputs(new ProcessingOutput(output, 1));
    }

    public S withItemOutputs(ProcessingOutput... outputs) {
        return withItemOutputs(NonNullList.of(ProcessingOutput.EMPTY, outputs));
    }

    public S withItemOutputs(NonNullList<ProcessingOutput> outputs) {
        params.results = outputs;
        return self();
    }

    public S withFluidIngredients(SizedFluidIngredient... ingredients) {
        return withFluidIngredients(NonNullList.of(new SizedFluidIngredient(FluidIngredient.empty(), 1000), ingredients));
    }

    public S withFluidIngredients(NonNullList<SizedFluidIngredient> ingredients) {
        params.fluidIngredients = ingredients;
        return self();
    }

    public S withGasIngredients(GasIngredient... ingredients) {
        return withGasIngredients(NonNullList.of(GasIngredient.empty(), ingredients));
    }

    public S withGasIngredients(NonNullList<GasIngredient> ingredients) {
        params.gasIngredients = ingredients;
        return self();
    }

    public S withFluidOutputs(FluidStack... outputs) {
        return withFluidOutputs(NonNullList.of(FluidStack.EMPTY, outputs));
    }

    public S withFluidOutputs(NonNullList<FluidStack> outputs) {
        params.fluidResults = outputs;
        return self();
    }

    public S withGasOutputs(GasStack... outputs) {
        return withGasOutputs(NonNullList.of(GasStack.EMPTY, outputs));
    }

    public S withGasOutputs(NonNullList<GasStack> outputs) {
        params.gasResults = outputs;
        return self();
    }

    public S averageProcessingDuration() {
        return duration(100);
    }

    public S duration(int ticks) {
        params.processingDuration = ticks;
        return self();
    }

    public S requiresHeat(HeatCondition condition) {
        params.requiredHeat = condition;
        return self();
    }

    public void build(RecipeOutput consumer) {
        R recipe = build();
        IRecipeTypeInfo recipeType = recipe.getTypeInfo();
        ResourceLocation id = recipeId.withPrefix(recipeType.getId().getPath() + '/');
        List<String> errors = recipe.validate();
        if (!errors.isEmpty()) {
            errors.add(recipe.getClass().getSimpleName() + "with id " + id + " failed validation:");
            CreateCraftedBeginning.LOGGER.warn(Joiner.on('\n').join(errors));
        }
        consumer.accept(id, recipe, null, recipeConditions.toArray(new ICondition[0]));
    }

    public R build() {
        return factory.create(params);
    }

    public S require(TagKey<Item> tag) {
        return require(Ingredient.of(tag));
    }

    public S require(Ingredient ingredient) {
        params.ingredients.add(ingredient);
        return self();
    }

    public S require(ItemLike item) {
        return require(Ingredient.of(item));
    }

    public S require(@NotNull ICustomIngredient ingredient) {
        params.ingredients.add(ingredient.toVanilla());
        return self();
    }

    public S require(Mods mod, String id) {
        params.ingredients.add(new SimpleDatagenIngredient(mod, id).toVanilla());
        return self();
    }

    public S require(@NotNull FlowingFluid fluid, int amount) {
		return require(SizedFluidIngredient.of(fluid.getSource(), amount));
	}

	public S require(TagKey<Fluid> fluidTag, int amount) {
		return require(SizedFluidIngredient.of(fluidTag, amount));
	}

    public S require(SizedFluidIngredient ingredient) {
        params.fluidIngredients.add(ingredient);
        return self();
    }

    public S require(Gas gas, int amount) {
        return require(GasIngredient.fromGas(gas, amount));
    }

    public S require(GasIngredient ingredient) {
        params.gasIngredients.add(ingredient);
        return self();
    }

    public S requireGasTag(TagKey<Gas> gasTag, int amount) {
        return require(GasIngredient.fromTag(gasTag, amount));
    }

    public S output(ItemLike item) {
        return output(item, 1);
    }

    public S output(ItemLike item, int amount) {
        return output(1, item, amount);
    }

    public S output(float chance, ItemLike item, int amount) {
        return output(chance, new ItemStack(item, amount));
    }

    public S output(float chance, ItemStack output) {
        return output(new ProcessingOutput(output, chance));
    }

    public S output(ProcessingOutput output) {
        params.results.add(output);
        return self();
    }

    public S output(float chance, ItemLike item) {
        return output(chance, item, 1);
    }

    public S output(ItemStack output) {
        return output(1, output);
    }

    public S output(float chance, @NotNull Mods mod, String id, int amount) {
        return output(new ProcessingOutput(mod.asResource(id), amount, chance));
    }

    public S output(ResourceLocation id) {
        return output(1, id, 1);
    }

    public S output(float chance, ResourceLocation registryName, int amount) {
        return output(new ProcessingOutput(registryName, amount, chance));
    }

    public S output(@NotNull Mods mod, String id) {
        return output(1, mod.asResource(id), 1);
    }

    public S output(Fluid fluid, int amount) {
        return output(new FluidStack(FluidHelper.convertToStill(fluid), amount));
    }

    public S output(FluidStack fluidStack) {
        params.fluidResults.add(fluidStack);
        return self();
    }

    public S output(Gas gas, long amount) {
        return output(new GasStack(gas, amount));
    }

    public S output(GasStack gasStack) {
        params.gasResults.add(gasStack);
        return self();
    }

    public S whenModLoaded(String modId) {
        return withCondition(new ModLoadedCondition(modId));
    }

    public S withCondition(ICondition condition) {
        recipeConditions.add(condition);
        return self();
    }

    public S whenModMissing(String modId) {
        return withCondition(new NotCondition(new ModLoadedCondition(modId)));
    }
}
