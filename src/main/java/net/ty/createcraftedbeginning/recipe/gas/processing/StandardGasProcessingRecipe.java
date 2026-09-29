package net.ty.createcraftedbeginning.recipe.gas.processing;

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
public abstract class StandardGasProcessingRecipe<T extends RecipeInput> extends GasProcessingRecipe<T, GasProcessingRecipeParams> {
    public StandardGasProcessingRecipe(IRecipeTypeInfo typeInfo, GasProcessingRecipeParams params) {
        super(typeInfo, params);
    }

    @FunctionalInterface
    public interface Factory<R extends StandardGasProcessingRecipe<?>> extends GasProcessingRecipe.Factory<GasProcessingRecipeParams, R> {
        @Override
        R create(GasProcessingRecipeParams params);
    }

    public static class Builder<R extends StandardGasProcessingRecipe<?>> extends GasProcessingRecipeBuilder<GasProcessingRecipeParams, R, Builder<R>> {
        public Builder(Factory<R> factory, ResourceLocation recipeId) {
            super(factory, recipeId);
        }

        @Override
        protected GasProcessingRecipeParams createParams() {
            return new GasProcessingRecipeParams();
        }

        @Override
        public Builder<R> self() {
            return this;
        }
    }

    public static class Serializer<R extends StandardGasProcessingRecipe<?>> implements RecipeSerializer<R> {
        private final Factory<R> factory;
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(Factory<R> factory) {
            this.factory = factory;
            codec = gasRecipeCodec(factory, GasProcessingRecipeParams.CODEC);
            streamCodec = gasRecipeStreamCodec(factory, GasProcessingRecipeParams.STREAM_CODEC);
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
