package net.ty.createcraftedbeginning.api.gas.recipes;

import com.google.common.base.Joiner;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.data.SimpleDatagenIngredient;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.GasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe.Factory;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.TemperatureCondition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public abstract class ProcessingWithGasRecipeBuilder<P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>, S extends ProcessingWithGasRecipeBuilder<P, R, S>> {
    protected ResourceLocation recipeId;
    protected Factory<P, R> factory;
    protected P params;
    protected List<ICondition> recipeConditions;

    /**
     * Creates a new {@code ProcessingWithGasRecipeBuilder} instance.
     *
     * @param factory  the factory used to create the requested value
     * @param recipeId the resource location identifying the recipe
     */
    public ProcessingWithGasRecipeBuilder(Factory<P, R> factory, ResourceLocation recipeId) {
        this.recipeId = recipeId;
        this.factory = factory;
        params = createParams();
        recipeConditions = new ArrayList<>();
    }

    protected abstract P createParams();

    /**
     * Configures the item ingredients for this builder.
     *
     * @param ingredients the ingredients to add or inspect
     * @return the resulting value
     */
    public S withItemIngredients(Ingredient... ingredients) {
        return withItemIngredients(NonNullList.of(Ingredient.EMPTY, ingredients));
    }

    /**
     * Configures the item ingredients for this builder.
     *
     * @param ingredients the ingredients to add or inspect
     * @return the resulting value
     */
    public S withItemIngredients(NonNullList<Ingredient> ingredients) {
        params.ingredients = ingredients;
        return self();
    }

    /**
     * Returns this instance with its concrete generic type.
     *
     * @return the resulting value
     */
    public abstract S self();

    /**
     * Configures the single item output for this builder.
     *
     * @param output the output to add or process
     * @return the resulting value
     */
    public S withSingleItemOutput(ItemStack output) {
        return withItemOutputs(new ProcessingOutput(output, 1));
    }

    /**
     * Configures the item outputs for this builder.
     *
     * @param outputs the outputs to add or process
     * @return the resulting value
     */
    public S withItemOutputs(ProcessingOutput... outputs) {
        return withItemOutputs(NonNullList.of(ProcessingOutput.EMPTY, outputs));
    }

    /**
     * Configures the item outputs for this builder.
     *
     * @param outputs the outputs to add or process
     * @return the resulting value
     */
    public S withItemOutputs(NonNullList<ProcessingOutput> outputs) {
        params.results = outputs;
        return self();
    }

    /**
     * Configures the fluid ingredients for this builder.
     *
     * @param ingredients the ingredients to add or inspect
     * @return the resulting value
     */
    public S withFluidIngredients(SizedFluidIngredient... ingredients) {
        return withFluidIngredients(NonNullList.of(new SizedFluidIngredient(FluidIngredient.empty(), FluidType.BUCKET_VOLUME), ingredients));
    }

    /**
     * Configures the fluid ingredients for this builder.
     *
     * @param ingredients the ingredients to add or inspect
     * @return the resulting value
     */
    public S withFluidIngredients(NonNullList<SizedFluidIngredient> ingredients) {
        params.fluidIngredients = ingredients;
        return self();
    }

    /**
     * Configures the gas ingredients for this builder.
     *
     * @param ingredients the ingredients to add or inspect
     * @return the resulting value
     */
    public S withGasIngredients(SizedGasIngredient... ingredients) {
        return withGasIngredients(NonNullList.of(new SizedGasIngredient(GasIngredient.empty(), FluidType.BUCKET_VOLUME), ingredients));
    }

    /**
     * Configures the gas ingredients for this builder.
     *
     * @param ingredients the ingredients to add or inspect
     * @return the resulting value
     */
    public S withGasIngredients(NonNullList<SizedGasIngredient> ingredients) {
        params.gasIngredients = ingredients;
        return self();
    }

    /**
     * Configures the fluid outputs for this builder.
     *
     * @param outputs the outputs to add or process
     * @return the resulting value
     */
    public S withFluidOutputs(FluidStack... outputs) {
        return withFluidOutputs(NonNullList.of(FluidStack.EMPTY, outputs));
    }

    /**
     * Configures the fluid outputs for this builder.
     *
     * @param outputs the outputs to add or process
     * @return the resulting value
     */
    public S withFluidOutputs(NonNullList<FluidStack> outputs) {
        params.fluidResults = outputs;
        return self();
    }

    /**
     * Configures the gas outputs for this builder.
     *
     * @param outputs the outputs to add or process
     * @return the resulting value
     */
    public S withGasOutputs(GasStack... outputs) {
        return withGasOutputs(NonNullList.of(GasStack.EMPTY, outputs));
    }

    /**
     * Configures the gas outputs for this builder.
     *
     * @param outputs the outputs to add or process
     * @return the resulting value
     */
    public S withGasOutputs(NonNullList<GasStack> outputs) {
        params.gasResults = outputs;
        return self();
    }

    /**
     * Sets the average processing duration for generated recipes.
     *
     * @return the resulting value
     */
    public S averageProcessingDuration() {
        return duration(100);
    }

    /**
     * Sets the processing duration used by this builder.
     *
     * @param ticks the duration to use, in ticks
     * @return the resulting value
     */
    public S duration(int ticks) {
        params.processingDuration = ticks;
        return self();
    }

    /**
     * Sets the required temperature condition for this recipe.
     *
     * @param temperatureCondition the temperature condition to use
     * @return the resulting value
     */
    public S temperatureCondition(TemperatureCondition temperatureCondition) {
        params.temperatureCondition = temperatureCondition;
        return self();
    }

    /**
     * Builds the configured value.
     *
     * @param consumer the consumer that receives each value
     */
    public void build(RecipeOutput consumer) {
        R recipe = build();
        IRecipeTypeInfo recipeType = recipe.getTypeInfo();
        ResourceLocation id = recipeId.withPrefix(recipeType.getId().getPath() + '/');
        List<String> errors = recipe.validate();
        if (!errors.isEmpty()) {
            errors.add(recipe.getClass().getSimpleName() + " with id " + id + " failed validation:");
            CreateCraftedBeginning.LOGGER.warn(Joiner.on('\n').join(errors));
        }

        consumer.accept(id, recipe, null, recipeConditions.toArray(new ICondition[0]));
    }

    /**
     * Builds the configured value.
     *
     * @return the created value
     */
    public R build() {
        return factory.create(params);
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param tag the tag to inspect or process
     * @return the resulting value
     */
    public S require(TagKey<Item> tag) {
        return require(Ingredient.of(tag));
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param ingredient the ingredient to add or inspect
     * @return the resulting value
     */
    public S require(Ingredient ingredient) {
        params.ingredients.add(ingredient);
        return self();
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param item the item to inspect or process
     * @return the resulting value
     */
    public S require(ItemLike item) {
        return require(Ingredient.of(item));
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param ingredient the ingredient to add or inspect
     * @return the resulting value
     */
    public S require(ICustomIngredient ingredient) {
        params.ingredients.add(ingredient.toVanilla());
        return self();
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param mod the mod identifier namespace
     * @param id  the identifier of the target value
     * @return the resulting value
     */
    public S require(Mods mod, String id) {
        params.ingredients.add(new SimpleDatagenIngredient(mod, id).toVanilla());
        return self();
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param fluid  the fluid to use
     * @param amount the amount to use
     * @return the resulting value
     */
    public S require(FlowingFluid fluid, int amount) {
        return require(SizedFluidIngredient.of(fluid.getSource(), amount));
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param fluidTag the fluid tag to use
     * @param amount   the amount to use
     * @return the resulting value
     */
    public S require(TagKey<Fluid> fluidTag, int amount) {
        return require(SizedFluidIngredient.of(fluidTag, amount));
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param ingredient the ingredient to add or inspect
     * @return the resulting value
     */
    public S require(SizedFluidIngredient ingredient) {
        params.fluidIngredients.add(ingredient);
        return self();
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param gasType the gas type to inspect or process
     * @param amount  the amount to use
     * @return the resulting value
     */
    public S require(Gas gasType, long amount) {
        return require(SizedGasIngredient.of(gasType, amount));
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param ingredient the ingredient to add or inspect
     * @return the resulting value
     */
    public S require(SizedGasIngredient ingredient) {
        params.gasIngredients.add(ingredient);
        return self();
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param gasTag the gas tag to use
     * @param amount the amount to use
     * @return the resulting value
     */
    public S require(TagKey<Gas> gasTag, long amount) {
        return require(SizedGasIngredient.of(gasTag, amount));
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param item the item to inspect or process
     * @return the resulting value
     */
    public S output(ItemLike item) {
        return output(item, 1);
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param item   the item to inspect or process
     * @param amount the amount to use
     * @return the resulting value
     */
    public S output(ItemLike item, int amount) {
        return output(1, item, amount);
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param chance the chance value to use
     * @param item   the item to inspect or process
     * @param amount the amount to use
     * @return the resulting value
     */
    public S output(float chance, ItemLike item, int amount) {
        return output(chance, new ItemStack(item, amount));
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param chance the chance value to use
     * @param output the output to add or process
     * @return the resulting value
     */
    public S output(float chance, ItemStack output) {
        return output(new ProcessingOutput(output, chance));
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param output the output to add or process
     * @return the resulting value
     */
    public S output(ProcessingOutput output) {
        params.results.add(output);
        return self();
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param chance the chance value to use
     * @param item   the item to inspect or process
     * @return the resulting value
     */
    public S output(float chance, ItemLike item) {
        return output(chance, item, 1);
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param output the output to add or process
     * @return the resulting value
     */
    public S output(ItemStack output) {
        return output(1, output);
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param chance the chance value to use
     * @param mod    the mod identifier namespace
     * @param id     the identifier of the target value
     * @param amount the amount to use
     * @return the resulting value
     */
    public S output(float chance, Mods mod, String id, int amount) {
        return output(new ProcessingOutput(mod.asResource(id), amount, chance));
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param id the identifier of the target value
     * @return the resulting value
     */
    public S output(ResourceLocation id) {
        return output(1, id, 1);
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param chance       the chance value to use
     * @param registryName the resource location identifying the regi
     * @param amount       the amount to use
     * @return the resulting value
     */
    public S output(float chance, ResourceLocation registryName, int amount) {
        return output(new ProcessingOutput(registryName, amount, chance));
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param mod the mod identifier namespace
     * @param id  the identifier of the target value
     * @return the resulting value
     */
    public S output(Mods mod, String id) {
        return output(1, mod.asResource(id), 1);
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param fluid  the fluid to use
     * @param amount the amount to use
     * @return the resulting value
     */
    public S output(Fluid fluid, int amount) {
        return output(new FluidStack(FluidHelper.convertToStill(fluid), amount));
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param fluidStack the fluid stack to inspect or process
     * @return the resulting value
     */
    public S output(FluidStack fluidStack) {
        params.fluidResults.add(fluidStack);
        return self();
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param gasType the gas type to inspect or process
     * @param amount  the amount to use
     * @return the resulting value
     */
    public S output(Gas gasType, long amount) {
        return output(new GasStack(gasType, amount));
    }

    /**
     * Adds the supplied output to this builder.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resulting value
     */
    public S output(GasStack gasStack) {
        params.gasResults.add(gasStack);
        return self();
    }

    /**
     * Adds a condition for when mod loaded.
     *
     * @param modId the mod identifier to test
     * @return the resulting value
     */
    public S whenModLoaded(String modId) {
        return withCondition(new ModLoadedCondition(modId));
    }

    /**
     * Configures the condition for this builder.
     *
     * @param condition the condition to use
     * @return the resulting value
     */
    public S withCondition(ICondition condition) {
        recipeConditions.add(condition);
        return self();
    }

    /**
     * Adds a condition for when mod missing.
     *
     * @param modId the mod identifier to test
     * @return the resulting value
     */
    public S whenModMissing(String modId) {
        return withCondition(new NotCondition(new ModLoadedCondition(modId)));
    }
}
