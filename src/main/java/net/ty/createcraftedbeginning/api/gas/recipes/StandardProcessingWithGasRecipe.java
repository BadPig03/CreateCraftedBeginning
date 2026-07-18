package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class StandardProcessingWithGasRecipe<T extends RecipeInput> extends ProcessingWithGasRecipe<T, ProcessingWithGasRecipeParams> {
    /**
     * Creates a new {@code StandardProcessingWithGasRecipe} instance.
     *
     * @param typeInfo the type info to use
     * @param params   the parameters used to configure the operation
     */
    public StandardProcessingWithGasRecipe(IRecipeTypeInfo typeInfo, ProcessingWithGasRecipeParams params) {
        super(typeInfo, params);
    }

    @FunctionalInterface
    public interface Factory<R extends StandardProcessingWithGasRecipe<?>> extends ProcessingWithGasRecipe.Factory<ProcessingWithGasRecipeParams, R> {
        /**
         * {@inheritDoc}
         */
        @Override
        R create(ProcessingWithGasRecipeParams params);
    }

    public static class Builder<R extends StandardProcessingWithGasRecipe<?>> extends ProcessingWithGasRecipeBuilder<ProcessingWithGasRecipeParams, R, Builder<R>> {
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
        protected ProcessingWithGasRecipeParams createParams() {
            return new ProcessingWithGasRecipeParams();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder<R> self() {
            return this;
        }
    }

    public static class Serializer<R extends StandardProcessingWithGasRecipe<?>> implements RecipeSerializer<R> {
        private final Factory<R> factory;
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        /**
         * Creates a new {@code Serializer} instance.
         *
         * @param factory the factory used to create the requested value
         */
        public Serializer(Factory<R> factory) {
            this.factory = factory;
            codec = ProcessingWithGasRecipe.codec(factory, ProcessingWithGasRecipeParams.CODEC);
            streamCodec = ProcessingWithGasRecipe.streamCodec(factory, ProcessingWithGasRecipeParams.STREAM_CODEC);
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

        /**
         * Returns the factory used to create instances of this type.
         *
         * @return the resulting factory
         */
        public Factory<R> factory() {
            return factory;
        }
    }
}
