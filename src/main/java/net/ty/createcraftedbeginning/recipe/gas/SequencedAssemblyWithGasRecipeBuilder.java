package net.ty.createcraftedbeginning.recipe.gas;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeBuilder;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.ItemApplicationWithGasRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.gas.ItemApplicationWithGasRecipe.Factory;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SequencedAssemblyWithGasRecipeBuilder {
    private final ResourceLocation id;
    private final SequencedAssemblyWithGasRecipe recipe;
    protected List<ICondition> recipeConditions;

    /**
     * Creates a new {@code SequencedAssemblyWithGasRecipeBuilder} instance.
     *
     * @param id the identifier of the target value
     */
    public SequencedAssemblyWithGasRecipeBuilder(ResourceLocation id) {
        this.id = id;
        recipeConditions = new ArrayList<>();
        recipe = new SequencedAssemblyWithGasRecipe(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getSerializer());
    }

    /**
     * Adds the supplied step.
     *
     * @param <R>     the value type constrained by {@code extends StandardProcessingWithGasRecipe<?>}
     * @param factory the factory used to create the requested value
     * @param builder the builder to configure
     * @return this builder for chaining
     */
    public <R extends StandardProcessingWithGasRecipe<?>> SequencedAssemblyWithGasRecipeBuilder addStep(StandardProcessingWithGasRecipe.Factory<R> factory, UnaryOperator<StandardProcessingWithGasRecipe.Builder<R>> builder) {
        return addStep((Function<ResourceLocation, StandardProcessingWithGasRecipe.Builder<R>>) stepId -> new StandardProcessingWithGasRecipe.Builder<>(factory, stepId), builder);
    }

    /**
     * Adds the supplied step.
     *
     * @param <B>     the value type constrained by {@code extends ProcessingWithGasRecipeBuilder<?, ?, B>}
     * @param factory the factory used to create the requested value
     * @param builder the builder to configure
     * @return this builder for chaining
     */
    public <B extends ProcessingWithGasRecipeBuilder<?, ?, B>> SequencedAssemblyWithGasRecipeBuilder addStep(Function<ResourceLocation, B> factory, UnaryOperator<B> builder) {
        B recipeBuilder = factory.apply(ResourceLocation.withDefaultNamespace("dummy"));
        Item placeholder = recipe.getTransitionalItem().getItem();
        B configuredBuilder = recipeBuilder.require(placeholder).output(placeholder);
        recipe.getSequence().add(new SequencedWithGasRecipe<>(builder.apply(configuredBuilder).build()));
        return this;
    }

    /**
     * Adds the supplied step.
     *
     * @param <R>     the value type constrained by {@code extends ItemApplicationWithGasRecipe}
     * @param factory the factory used to create the requested value
     * @param builder the builder to configure
     * @return this builder for chaining
     */
    public <R extends ItemApplicationWithGasRecipe> SequencedAssemblyWithGasRecipeBuilder addStep(Factory<R> factory, UnaryOperator<Builder<R>> builder) {
        return addStep((Function<ResourceLocation, Builder<R>>) stepId -> new Builder<>(factory, stepId), builder);
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param ingredient the ingredient to add or inspect
     * @return this builder for chaining
     */
    public SequencedAssemblyWithGasRecipeBuilder require(ItemLike ingredient) {
        return require(Ingredient.of(ingredient));
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param ingredient the ingredient to add or inspect
     * @return this builder for chaining
     */
    public SequencedAssemblyWithGasRecipeBuilder require(Ingredient ingredient) {
        recipe.ingredient = ingredient;
        return this;
    }

    /**
     * Adds the supplied requirement to this builder.
     *
     * @param tag the tag to inspect or process
     * @return this builder for chaining
     */
    public SequencedAssemblyWithGasRecipeBuilder require(TagKey<Item> tag) {
        return require(Ingredient.of(tag));
    }

    /**
     * Transitions this object to the to state.
     *
     * @param item the item to inspect or process
     * @return this instance
     */
    public SequencedAssemblyWithGasRecipeBuilder transitionTo(ItemLike item) {
        recipe.transitionalItem = new ProcessingOutput(item.asItem(), 1, 1);
        return this;
    }

    /**
     * Sets the number of sequenced-assembly loops.
     *
     * @param loops the loops value to use
     * @return this instance
     */
    public SequencedAssemblyWithGasRecipeBuilder loops(int loops) {
        recipe.loops = loops;
        return this;
    }

    /**
     * Adds the supplied output.
     *
     * @param item   the item to inspect or process
     * @param weight the weight value to use
     * @return this builder for chaining
     */
    public SequencedAssemblyWithGasRecipeBuilder addOutput(ItemLike item, float weight) {
        return addOutput(new ItemStack(item), weight);
    }

    /**
     * Adds the supplied output.
     *
     * @param item   the item to inspect or process
     * @param count  the count value to use
     * @param weight the weight value to use
     * @return this builder for chaining
     */
    public SequencedAssemblyWithGasRecipeBuilder addOutput(ItemLike item, int count, float weight) {
        return addOutput(new ItemStack(item), count, weight);
    }

    /**
     * Adds the supplied output.
     *
     * @param item   the item to inspect or process
     * @param weight the weight value to use
     * @return this builder for chaining
     */
    public SequencedAssemblyWithGasRecipeBuilder addOutput(ItemStack item, float weight) {
        recipe.resultPool.add(new ProcessingOutput(item.getItem(), item.getCount(), item.getComponentsPatch(), weight));
        return this;
    }

    /**
     * Adds the supplied output.
     *
     * @param item   the item to inspect or process
     * @param count  the count value to use
     * @param weight the weight value to use
     * @return this builder for chaining
     */
    public SequencedAssemblyWithGasRecipeBuilder addOutput(ItemStack item, int count, float weight) {
        recipe.resultPool.add(new ProcessingOutput(item.getItem(), count, item.getComponentsPatch(), weight));
        return this;
    }

    /**
     * Builds the configured value.
     *
     * @param consumer the consumer that receives each value
     */
    public void build(RecipeOutput consumer) {
        RecipeHolder<SequencedAssemblyWithGasRecipe> holder = build();
        ResourceLocation holderId = holder.id();
        String path = CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getId().getPath() + '/' + holderId.getPath();
        ResourceLocation outputId = ResourceLocation.fromNamespaceAndPath(holderId.getNamespace(), path);
        consumer.accept(outputId, holder.value(), null, recipeConditions.toArray(new ICondition[0]));
    }

    /**
     * Builds the configured value.
     *
     * @return the created value
     */
    public RecipeHolder<SequencedAssemblyWithGasRecipe> build() {
        return new RecipeHolder<>(id, recipe);
    }
}
