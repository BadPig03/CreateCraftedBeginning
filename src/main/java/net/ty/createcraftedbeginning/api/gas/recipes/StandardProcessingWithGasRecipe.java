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
    public StandardProcessingWithGasRecipe(IRecipeTypeInfo typeInfo, ProcessingWithGasRecipeParams params) {
        super(typeInfo, params);
    }

    @FunctionalInterface
    public interface Factory<R extends StandardProcessingWithGasRecipe<?>> extends ProcessingWithGasRecipe.Factory<ProcessingWithGasRecipeParams, R> {
        @Override
        R create(ProcessingWithGasRecipeParams params);
    }

    public static class Builder<R extends StandardProcessingWithGasRecipe<?>> extends ProcessingWithGasRecipeBuilder<ProcessingWithGasRecipeParams, R, Builder<R>> {
        public Builder(Factory<R> factory, ResourceLocation recipeId) {
            super(factory, recipeId);
        }

        @Override
        protected ProcessingWithGasRecipeParams createParams() {
            return new ProcessingWithGasRecipeParams();
        }

        @Override
        public Builder<R> self() {
            return this;
        }
    }

    public static class Serializer<R extends StandardProcessingWithGasRecipe<?>> implements RecipeSerializer<R> {
        private final Factory<R> factory;
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(Factory<R> factory) {
            this.factory = factory;
            codec = ProcessingWithGasRecipe.codec(factory, ProcessingWithGasRecipeParams.CODEC);
            streamCodec = ProcessingWithGasRecipe.streamCodec(factory, ProcessingWithGasRecipeParams.STREAM_CODEC);
        }

        @Override
        public MapCodec<R> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return streamCodec;
        }

        public Factory<R> factory() {
            return factory;
        }
    }
}
