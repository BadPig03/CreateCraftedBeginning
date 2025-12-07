package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.codec.CreateCodecs;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ProcessingWithGasRecipeParams {
    public static MapCodec<ProcessingWithGasRecipeParams> CODEC = codec(ProcessingWithGasRecipeParams::new);
    public static StreamCodec<RegistryFriendlyByteBuf, ProcessingWithGasRecipeParams> STREAM_CODEC = streamCodec(ProcessingWithGasRecipeParams::new);

    protected NonNullList<Ingredient> ingredients;
    protected NonNullList<ProcessingOutput> results;
    protected NonNullList<SizedFluidIngredient> fluidIngredients;
    protected NonNullList<FluidStack> fluidResults;
    protected NonNullList<GasIngredient> gasIngredients;
    protected NonNullList<GasStack> gasResults;
    protected int processingDuration;
    protected HeatCondition requiredHeat;

    protected ProcessingWithGasRecipeParams() {
        ingredients = NonNullList.create();
        results = NonNullList.create();
        fluidIngredients = NonNullList.create();
        fluidResults = NonNullList.create();
        gasIngredients = NonNullList.create();
        gasResults = NonNullList.create();
        processingDuration = 0;
        requiredHeat = HeatCondition.NONE;
    }

    @SuppressWarnings({"removal", "UnstableApiUsage"})
    @Contract("_ -> new")
    protected static <P extends ProcessingWithGasRecipeParams> @NotNull MapCodec<P> codec(Supplier<P> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.either(Codec.either(CreateCodecs.SIZED_FLUID_INGREDIENT, GasIngredient.CODEC), Ingredient.CODEC).listOf().fieldOf("ingredients").forGetter(ProcessingWithGasRecipeParams::ingredients), Codec.either(Codec.either(FluidStack.CODEC, GasStack.CODEC), ProcessingOutput.CODEC).listOf().fieldOf("results").forGetter(ProcessingWithGasRecipeParams::results), Codec.INT.optionalFieldOf("processing_time", 0).forGetter(ProcessingWithGasRecipeParams::processingDuration), HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE).forGetter(ProcessingWithGasRecipeParams::requiredHeat)).apply(instance, (ingredients, results, processingDuration, requiredHeat) -> {
            P params = factory.get();
            ingredients.forEach(either -> either.ifRight(params.ingredients::add).ifLeft(innerEither -> innerEither.ifLeft(params.fluidIngredients::add).ifRight(params.gasIngredients::add)));
            results.forEach(either -> either.ifRight(params.results::add).ifLeft(innerEither -> innerEither.ifLeft(params.fluidResults::add).ifRight(params.gasResults::add)));
            params.processingDuration = processingDuration;
            params.requiredHeat = requiredHeat;
            return params;
        }));
    }

    @Contract(value = "_ -> new", pure = true)
    protected static <P extends ProcessingWithGasRecipeParams> @NotNull StreamCodec<RegistryFriendlyByteBuf, P> streamCodec(Supplier<P> factory) {
        return StreamCodec.of((buffer, params) -> params.encode(buffer), buffer -> {
            P params = factory.get();
            params.decode(buffer);
            return params;
        });
    }

    protected final @NotNull List<Either<Either<SizedFluidIngredient, GasIngredient>, Ingredient>> ingredients() {
        List<Either<Either<SizedFluidIngredient, GasIngredient>, Ingredient>> ingredients = new ArrayList<>(this.ingredients.size() + gasIngredients.size() + fluidIngredients.size());
        this.ingredients.forEach(ingredient -> ingredients.add(Either.right(ingredient)));
        fluidIngredients.forEach(ingredient -> ingredients.add(Either.left(Either.left(ingredient))));
        gasIngredients.forEach(ingredient -> ingredients.add(Either.left(Either.right(ingredient))));
        return ingredients;
    }

    protected final @NotNull List<Either<Either<FluidStack, GasStack>, ProcessingOutput>> results() {
        List<Either<Either<FluidStack, GasStack>, ProcessingOutput>> results = new ArrayList<>(this.results.size() + gasResults.size() + fluidResults.size());
        this.results.forEach(result -> results.add(Either.right(result)));
        fluidResults.forEach(result -> results.add(Either.left(Either.left(result))));
        gasResults.forEach(result -> results.add(Either.left(Either.right(result))));
        return results;
    }

    protected final int processingDuration() {
        return processingDuration;
    }

    protected final HeatCondition requiredHeat() {
        return requiredHeat;
    }

    protected void encode(RegistryFriendlyByteBuf buffer) {
        CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).encode(buffer, ingredients);
        CatnipStreamCodecBuilders.nonNullList(GasIngredient.STREAM_CODEC).encode(buffer, gasIngredients);
        CatnipStreamCodecBuilders.nonNullList(SizedFluidIngredient.STREAM_CODEC).encode(buffer, fluidIngredients);
        CatnipStreamCodecBuilders.nonNullList(ProcessingOutput.STREAM_CODEC).encode(buffer, results);
        CatnipStreamCodecBuilders.nonNullList(GasStack.STREAM_CODEC).encode(buffer, gasResults);
        CatnipStreamCodecBuilders.nonNullList(FluidStack.STREAM_CODEC).encode(buffer, fluidResults);
        ByteBufCodecs.VAR_INT.encode(buffer, processingDuration);
        HeatCondition.STREAM_CODEC.encode(buffer, requiredHeat);
    }

    protected void decode(RegistryFriendlyByteBuf buffer) {
        ingredients = CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).decode(buffer);
        gasIngredients = CatnipStreamCodecBuilders.nonNullList(GasIngredient.STREAM_CODEC).decode(buffer);
        fluidIngredients = CatnipStreamCodecBuilders.nonNullList(SizedFluidIngredient.STREAM_CODEC).decode(buffer);
        results = CatnipStreamCodecBuilders.nonNullList(ProcessingOutput.STREAM_CODEC).decode(buffer);
        gasResults = CatnipStreamCodecBuilders.nonNullList(GasStack.STREAM_CODEC).decode(buffer);
        fluidResults = CatnipStreamCodecBuilders.nonNullList(FluidStack.STREAM_CODEC).decode(buffer);
        processingDuration = ByteBufCodecs.VAR_INT.decode(buffer);
        requiredHeat = HeatCondition.STREAM_CODEC.decode(buffer);
    }
}