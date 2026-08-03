package net.ty.createcraftedbeginning.api.gas.recipes;

import com.google.common.base.Joiner;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.TemperatureCondition;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ProcessingWithGasRecipe<I extends RecipeInput, P extends ProcessingWithGasRecipeParams> implements Recipe<I> {
    private final RecipeType<?> type;
    private final RecipeSerializer<?> serializer;
    private final IRecipeTypeInfo typeInfo;
    protected P params;
    protected NonNullList<Ingredient> ingredients;
    protected NonNullList<ProcessingOutput> results;
    protected NonNullList<SizedFluidIngredient> fluidIngredients;
    protected NonNullList<FluidStack> fluidResults;
    protected NonNullList<SizedGasIngredient> gasIngredients;
    protected NonNullList<GasStack> gasResults;
    protected int processingDuration;
    protected TemperatureCondition temperatureCondition;
    private Supplier<ItemStack> forcedResult;

    /**
     * Creates a new {@code ProcessingWithGasRecipe} instance.
     *
     * @param typeInfo the type info to use
     * @param params   the parameters used to configure the operation
     */
    public ProcessingWithGasRecipe(IRecipeTypeInfo typeInfo, P params) {
        this.params = params;
        this.typeInfo = typeInfo;
        ingredients = params.ingredients;
        fluidIngredients = params.fluidIngredients;
        gasIngredients = params.gasIngredients;
        results = params.results;
        fluidResults = params.fluidResults;
        gasResults = params.gasResults;
        processingDuration = params.processingDuration;
        temperatureCondition = params.temperatureCondition;
        type = typeInfo.getType();
        serializer = typeInfo.getSerializer();
        forcedResult = null;
    }

    /**
     * Creates a codec for serializing and deserializing values of this type.
     *
     * @param <P>         the value type constrained by {@code extends ProcessingWithGasRecipeParams}
     * @param <R>         the value type constrained by {@code extends ProcessingWithGasRecipe<?, P>}
     * @param factory     the factory used to create the requested value
     * @param paramsCodec the params codec to use
     * @return the configured codec
     */
    public static <P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>> MapCodec<R> codec(Factory<P, R> factory, MapCodec<P> paramsCodec) {
        return paramsCodec.xmap(factory::create, recipe -> recipe.getParams()).validate(recipe -> {
            List<String> errors = recipe.validate();
            if (errors.isEmpty()) {
                return DataResult.success(recipe);
            }

            errors.addFirst(recipe.getClass().getSimpleName() + " failed validation:");
            return DataResult.error(() -> Joiner.on('\n').join(errors), recipe);
        });
    }

    /**
     * Creates a stream codec for network serialization of values of this type.
     *
     * @param <P>         the value type constrained by {@code extends ProcessingWithGasRecipeParams}
     * @param <R>         the value type constrained by {@code extends ProcessingWithGasRecipe<?, P>}
     * @param factory     the factory used to create the requested value
     * @param paramsCodec the params codec to use
     * @return the configured codec
     */
    public static <P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>> @NotNull StreamCodec<RegistryFriendlyByteBuf, R> streamCodec(Factory<P, R> factory, StreamCodec<RegistryFriendlyByteBuf, P> paramsCodec) {
        return paramsCodec.map(factory::create, ProcessingWithGasRecipe::getParams);
    }

    /**
     * Validates the supplied state and reports invalid values.
     *
     * @return the resulting values
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        int ingredientCount = ingredients.size();
        int outputCount = results.size();
        if (ingredientCount > getMaxInputCount()) {
            errors.add("Recipe has more item inputs (" + ingredientCount + ") than supported (" + getMaxInputCount() + ").");
        }
        if (outputCount > getMaxOutputCount()) {
            errors.add("Recipe has more item outputs (" + outputCount + ") than supported (" + getMaxOutputCount() + ").");
        }

        ingredientCount = fluidIngredients.size();
        outputCount = fluidResults.size();
        if (ingredientCount > getMaxFluidInputCount()) {
            errors.add("Recipe has more fluid inputs (" + ingredientCount + ") than supported (" + getMaxFluidInputCount() + ").");
        }
        if (outputCount > getMaxFluidOutputCount()) {
            errors.add("Recipe has more fluid outputs (" + outputCount + ") than supported (" + getMaxFluidOutputCount() + ").");
        }

        ingredientCount = gasIngredients.size();
        outputCount = gasResults.size();
        if (ingredientCount > getMaxGasInputCount()) {
            errors.add("Recipe has more gas inputs (" + ingredientCount + ") than supported (" + getMaxGasInputCount() + ").");
        }
        if (outputCount > getMaxGasOutputCount()) {
            errors.add("Recipe has more gas outputs (" + outputCount + ") than supported (" + getMaxGasOutputCount() + ").");
        }

        validateSpecial(errors);
        if (processingDuration > 0 && !canSpecifyDuration()) {
            errors.add("Recipe specified a duration. Durations have no impact on this type of recipe.");
        }
        if (temperatureCondition == TemperatureCondition.NONE || requireTemperatureCondition()) {
            return errors;
        }

        errors.add("Recipe specified a temperature condition. Temperature conditions have no impact on this type of recipe.");
        return errors;
    }

    protected abstract int getMaxInputCount();

    protected abstract int getMaxOutputCount();

    protected boolean requireTemperatureCondition() {
        return false;
    }

    protected boolean canSpecifyDuration() {
        return false;
    }

    protected int getMaxFluidInputCount() {
        return 0;
    }

    protected int getMaxFluidOutputCount() {
        return 0;
    }

    protected int getMaxGasInputCount() {
        return 0;
    }

    protected int getMaxGasOutputCount() {
        return 0;
    }

    protected void validateSpecial(List<String> errors) {
    }

    /**
     * Returns the params.
     *
     * @return the params
     */
    public P getParams() {
        return params;
    }

    /**
     * Returns the fluid ingredients.
     *
     * @return the fluid ingredients
     */
    public NonNullList<SizedFluidIngredient> getFluidIngredients() {
        return fluidIngredients;
    }

    /**
     * Returns the gas ingredients.
     *
     * @return the gas ingredients
     */
    public NonNullList<SizedGasIngredient> getGasIngredients() {
        return gasIngredients;
    }

    /**
     * Returns the fluid results.
     *
     * @return the fluid results
     */
    public NonNullList<FluidStack> getFluidResults() {
        return fluidResults;
    }

    /**
     * Returns the gas results.
     *
     * @return the gas results
     */
    public NonNullList<GasStack> getGasResults() {
        return gasResults;
    }

    /**
     * Returns the rollable results.
     *
     * @return the rollable results
     */
    public List<ProcessingOutput> getRollableResults() {
        return results;
    }

    /**
     * Forces the next generated result to use the supplied stack.
     *
     * @param stack the stack to inspect or process
     */
    public void enforceNextResult(Supplier<ItemStack> stack) {
        forcedResult = stack;
    }

    /**
     * Rolls the results using the supplied random source.
     *
     * @param randomSource the random source used by the operation
     * @return the resulting values
     */
    public List<ItemStack> rollResults(RandomSource randomSource) {
        return rollResults(getRollableResults(), randomSource);
    }

    /**
     * Rolls the results using the supplied random source.
     *
     * @param rollableResults the rollable results to inspect or process
     * @param randomSource    the random source used by the operation
     * @return the resulting values
     */
    public List<ItemStack> rollResults(List<ProcessingOutput> rollableResults, RandomSource randomSource) {
        List<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < rollableResults.size(); i++) {
            ProcessingOutput output = rollableResults.get(i);
            ItemStack stack = i == 0 && forcedResult != null ? forcedResult.get() : output.rollOutput(randomSource);
            if (!stack.isEmpty()) {
                results.add(stack);
            }
        }
        return results;
    }

    /**
     * Returns the processing duration.
     *
     * @return the processing duration
     */
    public int getProcessingDuration() {
        return processingDuration;
    }

    /**
     * Returns the temperature condition.
     *
     * @return the temperature condition
     */
    public TemperatureCondition getTemperatureCondition() {
        return temperatureCondition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack assemble(I input, Provider provider) {
        return getResultItem(provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack getResultItem(Provider provider) {
        if (forcedResult != null) {
            return forcedResult.get();
        }
        return getRollableResults().isEmpty() ? ItemStack.EMPTY : getRollableResults().getFirst().getStack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGroup() {
        return "processing";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecipeType<?> getType() {
        return type;
    }

    /**
     * Returns the type info.
     *
     * @return the type info
     */
    public IRecipeTypeInfo getTypeInfo() {
        return typeInfo;
    }

    @FunctionalInterface
    public interface Factory<P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>> {
        /**
         * Creates a new value from the supplied arguments.
         *
         * @param params the parameters used to configure the operation
         * @return the created value
         */
        R create(P params);
    }
}
