package net.ty.createcraftedbeginning.api.gas.recipes;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.ty.createcraftedbeginning.api.gas.recipes.ItemApplicationWithGasRecipe.Builder;
import net.ty.createcraftedbeginning.api.gas.recipes.ItemApplicationWithGasRecipe.Factory;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class SequencedAssemblyWithGasRecipeBuilder {
    private final ResourceLocation id;
    private final SequencedAssemblyWithGasRecipe recipe;
    protected List<ICondition> recipeConditions;

    public SequencedAssemblyWithGasRecipeBuilder(ResourceLocation id) {
        this.id = id;
        recipeConditions = new ArrayList<>();
        recipe = new SequencedAssemblyWithGasRecipe(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getSerializer());
    }

    public <R extends StandardProcessingWithGasRecipe<?>> SequencedAssemblyWithGasRecipeBuilder addStep(StandardProcessingWithGasRecipe.Factory<R> factory, UnaryOperator<StandardProcessingWithGasRecipe.Builder<R>> builder) {
        return addStep((Function<ResourceLocation, StandardProcessingWithGasRecipe.Builder<R>>) id -> new StandardProcessingWithGasRecipe.Builder<>(factory, id), builder);
    }

    public <B extends ProcessingWithGasRecipeBuilder<?, ?, B>> SequencedAssemblyWithGasRecipeBuilder addStep(@NotNull Function<ResourceLocation, B> factory, @NotNull UnaryOperator<B> builder) {
        B recipeBuilder = factory.apply(ResourceLocation.withDefaultNamespace("dummy"));
        Item placeHolder = recipe.getTransitionalItem().getItem();
        recipe.getSequence().add(new SequencedWithGasRecipe<>(builder.apply(recipeBuilder.require(placeHolder).output(placeHolder)).build()));
        return this;
    }

    public <R extends ItemApplicationWithGasRecipe> SequencedAssemblyWithGasRecipeBuilder addStep(Factory<R> factory, UnaryOperator<Builder<R>> builder) {
        return addStep((Function<ResourceLocation, Builder<R>>) id -> new Builder<>(factory, id), builder);
    }

    public SequencedAssemblyWithGasRecipeBuilder require(ItemLike ingredient) {
        return require(Ingredient.of(ingredient));
    }

    public SequencedAssemblyWithGasRecipeBuilder require(Ingredient ingredient) {
        recipe.ingredient = ingredient;
        return this;
    }

    public SequencedAssemblyWithGasRecipeBuilder require(TagKey<Item> tag) {
        return require(Ingredient.of(tag));
    }

    public SequencedAssemblyWithGasRecipeBuilder transitionTo(@NotNull ItemLike item) {
        recipe.transitionalItem = new ProcessingOutput(item.asItem(), 1, 1);
        return this;
    }

    public SequencedAssemblyWithGasRecipeBuilder loops(int loops) {
        recipe.loops = loops;
        return this;
    }

    public SequencedAssemblyWithGasRecipeBuilder addOutput(ItemLike item, float weight) {
        return addOutput(new ItemStack(item), weight);
    }

    public SequencedAssemblyWithGasRecipeBuilder addOutput(ItemLike item, int count, float weight) {
        return addOutput(new ItemStack(item), count, weight);
    }

    public SequencedAssemblyWithGasRecipeBuilder addOutput(@NotNull ItemStack item, float weight) {
        recipe.resultPool.add(new ProcessingOutput(item.getItem(), item.getCount(), item.getComponentsPatch(), weight));
        return this;
    }

    public SequencedAssemblyWithGasRecipeBuilder addOutput(@NotNull ItemStack item, int count, float weight) {
        recipe.resultPool.add(new ProcessingOutput(item.getItem(), count, item.getComponentsPatch(), weight));
        return this;
    }

    public void build(@NotNull RecipeOutput consumer) {
        RecipeHolder<SequencedAssemblyWithGasRecipe> holder = build();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(), CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getId().getPath() + '/' + holder.id().getPath());
        consumer.accept(id, holder.value(), null, recipeConditions.toArray(new ICondition[0]));
    }

    public RecipeHolder<SequencedAssemblyWithGasRecipe> build() {
        return new RecipeHolder<>(id, recipe);
    }
}
