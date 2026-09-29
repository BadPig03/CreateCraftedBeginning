package net.ty.createcraftedbeginning.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeBuilder;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureAwareRecipe;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureRecipeData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FractionationTowerRecipe extends GasProcessingRecipe<RecipeInput, FractionationTowerRecipeParams> implements TemperatureAwareRecipe {
    private static final int MAX_INPUTS = 64;
    private static final int MIN_PRODUCTS = 2;

    private final TemperatureRecipeData temperature;
    private final List<FractionationTowerOutput> outputs;

    public FractionationTowerRecipe(FractionationTowerRecipeParams params) {
        super(CCBRecipeTypes.FRACTIONATION_TOWER, params);
        temperature = params.temperature;
        outputs = List.copyOf(params.outputs);
    }

    @Override
    public TemperatureRecipeData getTemperatureRecipeData() {
        return temperature;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return true;
    }

    @Override
    protected int getMaxInputCount() {
        return MAX_INPUTS;
    }

    @Override
    protected int getMaxOutputCount() {
        return 0;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return MAX_INPUTS;
    }

    @Override
    protected int getMaxGasInputCount() {
        return MAX_INPUTS;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    protected void validateSpecial(List<String> errors) {
        if (!temperature.isValid() || temperature.condition() == TemperatureCondition.NONE) {
            errors.add("Fractionation tower temperature must specify valid heating or cooling; got " + temperature + '.');
        }
        if (getProcessingDuration() <= 0) {
            errors.add("Fractionation tower duration must be positive; got " + getProcessingDuration() + " ticks.");
        }
        if (getIngredients().isEmpty() && getFluidIngredients().isEmpty() && getGasRequirements().isEmpty()) {
            errors.add("Fractionation tower recipe requires at least one input.");
        }
        if (outputs.size() < MIN_PRODUCTS || outputs.size() > FractionationTowerOutput.MAX_LAYER_OFFSET) {
            errors.add("Fractionation tower recipe must have between 2 and 8 layer outputs; got " + outputs.size() + '.');
        }
        Set<Integer> layers = new HashSet<>();
        boolean distinctProducts = false;
        for (FractionationTowerOutput output : outputs) {
            if (!output.isValid() || !layers.add(output.layer())) {
                errors.add("Fractionation tower output requires one product and a unique layer offset in [1, 9); got offset " + output.layer() + '.');
            }
            if (output.isSameProduct(outputs.getFirst())) {
                continue;
            }

            distinctProducts = true;
        }
        if (distinctProducts) {
            return;
        }

        errors.add("Fractionation tower recipe requires at least two distinct products.");
    }

    public List<FractionationTowerOutput> getLayerOutputs() {
        return outputs;
    }

    public boolean isCondensation() {
        TemperatureCondition condition = temperature.condition();
        return condition == TemperatureCondition.CHILLED || condition == TemperatureCondition.SUPERCHILLED;
    }

    public int getRequiredHeight() {
        return outputs.stream().mapToInt(FractionationTowerOutput::layer).max().orElse(0) + 1;
    }

    public static final class Builder extends GasProcessingRecipeBuilder<FractionationTowerRecipeParams, FractionationTowerRecipe, Builder> {
        public Builder(ResourceLocation recipeId) {
            super(FractionationTowerRecipe::new, recipeId);
        }

        @Override
        protected FractionationTowerRecipeParams createParams() {
            return new FractionationTowerRecipeParams();
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder temperatureCondition(TemperatureCondition condition) {
            params.temperature = params.temperature.withCondition(condition);
            return this;
        }

        public Builder temperatureMatching(TemperatureMatching matching) {
            params.temperature = params.temperature.withMatching(matching);
            return this;
        }

        public Builder outputAtLayer(int layer, ItemStack item) {
            params.outputs.add(new FractionationTowerOutput(layer, item, FluidStack.EMPTY, GasStack.EMPTY));
            return this;
        }

        public Builder outputAtLayer(int layer, FluidStack fluid) {
            params.outputs.add(new FractionationTowerOutput(layer, ItemStack.EMPTY, fluid, GasStack.EMPTY));
            return this;
        }

        public Builder outputAtLayer(int layer, GasStack gas) {
            params.outputs.add(new FractionationTowerOutput(layer, ItemStack.EMPTY, FluidStack.EMPTY, gas));
            return this;
        }
    }

    public static final class Serializer implements RecipeSerializer<FractionationTowerRecipe> {
        private final MapCodec<FractionationTowerRecipe> codec = gasRecipeCodec(FractionationTowerRecipe::new, FractionationTowerRecipeParams.CODEC);
        private final StreamCodec<RegistryFriendlyByteBuf, FractionationTowerRecipe> streamCodec = gasRecipeStreamCodec(FractionationTowerRecipe::new, FractionationTowerRecipeParams.STREAM_CODEC);

        @Override
        public MapCodec<FractionationTowerRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FractionationTowerRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
