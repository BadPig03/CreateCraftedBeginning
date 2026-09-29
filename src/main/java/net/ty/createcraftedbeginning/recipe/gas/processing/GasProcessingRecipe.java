package net.ty.createcraftedbeginning.recipe.gas.processing;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeInput;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.gas.GasAwareRecipe;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeData;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.SizedGasIngredient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GasProcessingRecipe<I extends RecipeInput, P extends GasProcessingRecipeParams> extends ProcessingRecipe<I, P> implements GasAwareRecipe {
    protected final GasRecipeData gasRecipeData;
    protected final NonNullList<SizedGasIngredient> gasIngredients;
    protected final NonNullList<GasStack> gasResults;

    public GasProcessingRecipe(IRecipeTypeInfo typeInfo, P params) {
        super(typeInfo, params);
        gasRecipeData = params.gasRecipeData();
        gasIngredients = gasRecipeData.sizedIngredients();
        gasResults = gasRecipeData.resultStacks();
    }

    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        int ingredientCount = gasIngredients.size();
        int outputCount = gasResults.size();
        if (ingredientCount > getMaxGasInputCount()) {
            errors.add("Recipe has more gas inputs (" + ingredientCount + ") than supported (" + getMaxGasInputCount() + ").");
        }
        if (outputCount > getMaxGasOutputCount()) {
            errors.add("Recipe has more gas outputs (" + outputCount + ") than supported (" + getMaxGasOutputCount() + ").");
        }

        validateSpecial(errors);
        return errors;
    }

    @Override
    public GasRecipeData getGasRecipeData() {
        return gasRecipeData;
    }

    public static <P extends GasProcessingRecipeParams, R extends GasProcessingRecipe<?, P>> MapCodec<R> gasRecipeCodec(Factory<P, R> factory, MapCodec<P> paramsCodec) {
        return codec(factory, paramsCodec);
    }

    public static <P extends GasProcessingRecipeParams, R extends GasProcessingRecipe<?, P>> StreamCodec<RegistryFriendlyByteBuf, R> gasRecipeStreamCodec(Factory<P, R> factory, StreamCodec<RegistryFriendlyByteBuf, P> paramsCodec) {
        return streamCodec(factory, paramsCodec);
    }

    public NonNullList<SizedGasIngredient> getGasIngredients() {
        return gasIngredients;
    }

    public NonNullList<GasStack> getGasResults() {
        return gasResults;
    }

    public GasStack getPrimaryGasResult() {
        if (gasResults.isEmpty()) {
            return GasStack.EMPTY;
        }

        return gasResults.getFirst();
    }

    protected int getMaxGasInputCount() {
        return 0;
    }

    protected int getMaxGasOutputCount() {
        return 0;
    }

    protected void validateSpecial(List<String> errors) {
    }

    @FunctionalInterface
    public interface Factory<P extends GasProcessingRecipeParams, R extends GasProcessingRecipe<?, P>> extends ProcessingRecipe.Factory<P, R> {
        @Override
        R create(P params);
    }
}
