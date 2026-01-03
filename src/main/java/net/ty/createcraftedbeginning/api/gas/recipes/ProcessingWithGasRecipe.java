package net.ty.createcraftedbeginning.api.gas.recipes;

import com.google.common.base.Joiner;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
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
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.reactorkettle.TemperatureCondition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
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

    public ProcessingWithGasRecipe(@NotNull IRecipeTypeInfo typeInfo, @NotNull P params) {
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

    public static <P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>> MapCodec<R> codec(@NotNull Factory<P, R> factory, @NotNull MapCodec<P> paramsCodec) {
        return paramsCodec.xmap(factory::create, recipe -> recipe.getParams()).validate(recipe -> {
            List<String> errors = recipe.validate();
            if (errors.isEmpty()) {
                return DataResult.success(recipe);
            }
            errors.add(recipe.getClass().getSimpleName() + " failed validation:");
            return DataResult.error(() -> Joiner.on('\n').join(errors), recipe);
        });
    }

    public static <P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>> @NotNull StreamCodec<RegistryFriendlyByteBuf, R> streamCodec(@NotNull Factory<P, R> factory, @NotNull StreamCodec<RegistryFriendlyByteBuf, P> streamCodec) {
        return streamCodec.map(factory::create, ProcessingWithGasRecipe::getParams);
    }

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

        if (processingDuration > 0 && !canSpecifyDuration()) {
            errors.add("Recipe specified a duration. Durations have no impact on this type of recipe.");
        }

        if (temperatureCondition != TemperatureCondition.NONE && !requireTemperatureCondition()) {
            errors.add("Recipe specified a temperature condition. Temperature conditions have no impact on this type of recipe.");
        }
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

    public P getParams() {
        return params;
    }

    public NonNullList<SizedFluidIngredient> getFluidIngredients() {
        return fluidIngredients;
    }

    public NonNullList<SizedGasIngredient> getGasIngredients() {
        return gasIngredients;
    }

    public NonNullList<FluidStack> getFluidResults() {
        return fluidResults;
    }

    public NonNullList<GasStack> getGasResults() {
        return gasResults;
    }

    public List<ItemStack> getRollableResultsAsItemStacks() {
        return getRollableResults().stream().map(ProcessingOutput::getStack).collect(Collectors.toList());
    }

    public List<ProcessingOutput> getRollableResults() {
        return results;
    }

    public void enforceNextResult(Supplier<ItemStack> stack) {
        forcedResult = stack;
    }

    public List<ItemStack> rollResults(RandomSource randomSource) {
        return rollResults(getRollableResults(), randomSource);
    }

    public List<ItemStack> rollResults(@NotNull List<ProcessingOutput> rollableResults, RandomSource randomSource) {
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

    public int getProcessingDuration() {
        return processingDuration;
    }

    public TemperatureCondition getTemperatureCondition() {
        return temperatureCondition;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull I t, @NotNull Provider provider) {
        return getResultItem(provider);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull Provider provider) {
        if (forcedResult != null) {
            return forcedResult.get();
        }
        return getRollableResults().isEmpty() ? ItemStack.EMPTY : getRollableResults().getFirst().getStack();
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull String getGroup() {
        return "processing";
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return type;
    }

    public IRecipeTypeInfo getTypeInfo() {
        return typeInfo;
    }

    @FunctionalInterface
    public interface Factory<P extends ProcessingWithGasRecipeParams, R extends ProcessingWithGasRecipe<?, P>> {
        R create(P params);
    }
}
