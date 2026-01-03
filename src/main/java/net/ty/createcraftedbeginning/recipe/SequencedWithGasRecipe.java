package net.ty.createcraftedbeginning.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.IAssemblyRecipeWithGas;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe;

public class SequencedWithGasRecipe<T extends ProcessingWithGasRecipe<?, ?>> {
    public static final Codec<SequencedWithGasRecipe<?>> CODEC = Recipe.CODEC.comapFlatMap(recipe -> recipe instanceof ProcessingWithGasRecipe<?, ?> processing && recipe instanceof IAssemblyRecipeWithGas ? DataResult.success(new SequencedWithGasRecipe<>(processing)) : DataResult.error(() -> recipe.getClass().getSimpleName() + " is not supported in Sequenced Assembly with Gas"), SequencedWithGasRecipe::getRecipe);

    public static final StreamCodec<RegistryFriendlyByteBuf, SequencedWithGasRecipe<?>> STREAM_CODEC = Recipe.STREAM_CODEC.map(recipe -> {
        if (recipe instanceof ProcessingWithGasRecipe<?, ?> processing && recipe instanceof IAssemblyRecipeWithGas) {
            return new SequencedWithGasRecipe<>(processing);
        }
        throw new DecoderException("Unexpected " + recipe.getClass().getSimpleName() + " not supported in Sequenced Assembly with Gas");
    }, SequencedWithGasRecipe::getRecipe);

    private final T wrapped;

    public SequencedWithGasRecipe(T wrapped) {
        this.wrapped = wrapped;
    }

    public T getRecipe() {
        return wrapped;
    }

    public void initFromSequencedAssembly(SequencedAssemblyWithGasRecipe parent, boolean isFirst) {
        if (getAsAssemblyRecipe().supportsAssembly()) {
            Ingredient transit = Ingredient.of(parent.getTransitionalItem());
            wrapped.getIngredients().set(0, isFirst ? CompoundIngredient.of(transit, parent.getIngredient()) : transit);
        }
    }

    public IAssemblyRecipeWithGas getAsAssemblyRecipe() {
        return (IAssemblyRecipeWithGas) wrapped;
    }
}
