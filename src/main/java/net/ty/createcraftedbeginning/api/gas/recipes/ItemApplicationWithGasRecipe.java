package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemApplicationWithGasRecipe extends ProcessingWithGasRecipe<RecipeWrapper, ItemApplicationWithGasRecipeParams> {
    private final boolean keepHeldItem;

    /**
     * Creates a new {@code ItemApplicationWithGasRecipe} instance.
     *
     * @param type   the type to use
     * @param params the parameters used to configure the operation
     */
    public ItemApplicationWithGasRecipe(CCBRecipeTypes type, ItemApplicationWithGasRecipeParams params) {
        super(type, params);
        keepHeldItem = params.keepHeldItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(RecipeWrapper input, Level level) {
        return getProcessedItem().test(input.getItem(0)) && getRequiredHeldItem().test(input.getItem(1));
    }

    /**
     * Returns the required held item.
     *
     * @return the required held item
     */
    public Ingredient getRequiredHeldItem() {
        if (ingredients.size() < 2) {
            throw new IllegalStateException("Item Application Recipe has no tool!");
        }

        return ingredients.get(1);
    }

    /**
     * Returns the processed item.
     *
     * @return the processed item
     */
    public Ingredient getProcessedItem() {
        if (ingredients.isEmpty()) {
            throw new IllegalStateException("Item Application Recipe has no ingredient!");
        }

        return ingredients.getFirst();
    }

    @Override
    protected int getMaxInputCount() {
        return 2;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    /**
     * Checks whether the caller should keep held item.
     *
     * @return {@code true} if the caller should keep held item; otherwise {@code false}
     */
    public boolean shouldKeepHeldItem() {
        return keepHeldItem;
    }

    @FunctionalInterface
    public interface Factory<R extends ItemApplicationWithGasRecipe> extends ProcessingWithGasRecipe.Factory<ItemApplicationWithGasRecipeParams, R> {
        /**
         * {@inheritDoc}
         */
        @Override
        R create(ItemApplicationWithGasRecipeParams params);
    }

    public static class Builder<R extends ItemApplicationWithGasRecipe> extends ProcessingWithGasRecipeBuilder<ItemApplicationWithGasRecipeParams, R, Builder<R>> {
        /**
         * Creates a new {@code Builder} instance.
         *
         * @param factory  the factory used to create the requested value
         * @param recipeId the resource location identifying the recipe
         */
        public Builder(Factory<R> factory, ResourceLocation recipeId) {
            super(factory, recipeId);
        }

        @Override
        protected ItemApplicationWithGasRecipeParams createParams() {
            return new ItemApplicationWithGasRecipeParams();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder<R> self() {
            return this;
        }

        /**
         * Converts this value to an ol not consumed representation.
         *
         * @return the converted value
         */
        public Builder<R> toolNotConsumed() {
            params.keepHeldItem = true;
            return this;
        }
    }

    public static class Serializer<R extends ItemApplicationWithGasRecipe> implements RecipeSerializer<R> {
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        /**
         * Creates a new {@code Serializer} instance.
         *
         * @param factory the factory used to create the requested value
         */
        public Serializer(ProcessingWithGasRecipe.Factory<ItemApplicationWithGasRecipeParams, R> factory) {
            codec = ProcessingWithGasRecipe.codec(factory, ItemApplicationWithGasRecipeParams.CODEC);
            streamCodec = ProcessingWithGasRecipe.streamCodec(factory, ItemApplicationWithGasRecipeParams.STREAM_CODEC);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MapCodec<R> codec() {
            return codec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return streamCodec;
        }
    }
}
