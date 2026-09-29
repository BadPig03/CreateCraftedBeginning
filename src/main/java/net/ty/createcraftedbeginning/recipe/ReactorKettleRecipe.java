package net.ty.createcraftedbeginning.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeBuilder;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureAwareRecipe;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureRecipeData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorKettleRecipe extends GasProcessingRecipe<RecipeInput, ReactorKettleRecipeParams> implements TemperatureAwareRecipe {
    private final TemperatureRecipeData temperatureRecipeData;

    public ReactorKettleRecipe(ReactorKettleRecipeParams params) {
        super(CCBRecipeTypes.REACTOR_KETTLE, params);
        temperatureRecipeData = params.temperatureRecipeData();
    }

    @Override
    protected int getMaxInputCount() {
        return 64;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 3;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 2;
    }

    @Override
    protected int getMaxGasInputCount() {
        return 3;
    }

    @Override
    protected int getMaxGasOutputCount() {
        return 2;
    }

    @Override
    protected void validateSpecial(List<String> errors) {
        if (temperatureRecipeData.isValid()) {
            return;
        }

        errors.add("Recipe cannot use compatible temperature matching with superheated or superchilled conditions.");
    }

    @Override
    public TemperatureRecipeData getTemperatureRecipeData() {
        return temperatureRecipeData;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return true;
    }

    @FunctionalInterface
    public interface Factory<R extends ReactorKettleRecipe> extends GasProcessingRecipe.Factory<ReactorKettleRecipeParams, R> {
        @Override
        R create(ReactorKettleRecipeParams params);
    }

    public static class Builder<R extends ReactorKettleRecipe> extends GasProcessingRecipeBuilder<ReactorKettleRecipeParams, R, Builder<R>> {
        public Builder(Factory<R> factory, ResourceLocation recipeId) {
            super(factory, recipeId);
        }

        @Override
        protected ReactorKettleRecipeParams createParams() {
            return new ReactorKettleRecipeParams();
        }

        @Override
        public Builder<R> self() {
            return this;
        }

        public Builder<R> temperatureCondition(TemperatureCondition condition) {
            params.temperatureCondition(condition);
            return this;
        }

        public Builder<R> temperatureMatching(TemperatureMatching matching) {
            params.temperatureMatching(matching);
            return this;
        }

    }

    public static class Serializer<R extends ReactorKettleRecipe> implements RecipeSerializer<R> {
        private final Factory<R> factory;
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(Factory<R> factory) {
            this.factory = factory;
            codec = gasRecipeCodec(factory, ReactorKettleRecipeParams.CODEC);
            streamCodec = gasRecipeStreamCodec(factory, ReactorKettleRecipeParams.STREAM_CODEC);
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
