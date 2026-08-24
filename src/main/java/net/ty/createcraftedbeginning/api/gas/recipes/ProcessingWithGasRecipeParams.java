package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.codec.CreateCodecs;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ProcessingWithGasRecipeParams {
    public static MapCodec<ProcessingWithGasRecipeParams> CODEC = codec(ProcessingWithGasRecipeParams::new);
    public static StreamCodec<RegistryFriendlyByteBuf, ProcessingWithGasRecipeParams> STREAM_CODEC = streamCodec(ProcessingWithGasRecipeParams::new);

    protected NonNullList<Ingredient> ingredients;
    protected NonNullList<ProcessingOutput> results;
    protected NonNullList<SizedFluidIngredient> fluidIngredients;
    protected NonNullList<FluidStack> fluidResults;
    protected NonNullList<SizedGasIngredient> gasIngredients;
    protected NonNullList<GasStack> gasResults;
    protected int processingDuration;
    protected TemperatureCondition temperatureCondition;
    protected TemperatureMatching temperatureMatching;

    protected ProcessingWithGasRecipeParams() {
        ingredients = NonNullList.create();
        results = NonNullList.create();
        fluidIngredients = NonNullList.create();
        fluidResults = NonNullList.create();
        gasIngredients = NonNullList.create();
        gasResults = NonNullList.create();
        processingDuration = 0;
        temperatureCondition = TemperatureCondition.NONE;
        temperatureMatching = TemperatureMatching.EXACT;
    }

    @SuppressWarnings({"removal", "UnstableApiUsage"})
    @Contract("_ -> new")
    protected static <P extends ProcessingWithGasRecipeParams> @NotNull MapCodec<P> codec(Supplier<P> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.either(Codec.either(CreateCodecs.SIZED_FLUID_INGREDIENT, SizedGasIngredient.CODEC), Ingredient.CODEC).listOf().fieldOf("ingredients").forGetter(ProcessingWithGasRecipeParams::ingredients), Codec.either(Codec.either(FluidStack.CODEC, GasStack.CODEC), ProcessingOutput.CODEC).listOf().fieldOf("results").forGetter(ProcessingWithGasRecipeParams::results), Codec.INT.optionalFieldOf("processing_time", 0).forGetter(ProcessingWithGasRecipeParams::processingDuration), TemperatureCondition.CODEC.optionalFieldOf("temperature", TemperatureCondition.NONE).forGetter(ProcessingWithGasRecipeParams::temperatureCondition), TemperatureMatching.CODEC.optionalFieldOf("temperature_matching", TemperatureMatching.EXACT).forGetter(ProcessingWithGasRecipeParams::temperatureMatching)).apply(instance, (ingredients, results, duration, temperature, temperatureMatching) -> {
            P params = factory.get();
            ingredients.forEach(ingredient -> ingredient.ifRight(params.ingredients::add).ifLeft(fluidOrGas -> fluidOrGas.ifLeft(params.fluidIngredients::add).ifRight(params.gasIngredients::add)));
            results.forEach(result -> result.ifRight(params.results::add).ifLeft(fluidOrGas -> fluidOrGas.ifLeft(params.fluidResults::add).ifRight(params.gasResults::add)));
            params.processingDuration = duration;
            params.temperatureCondition = temperature;
            params.temperatureMatching = temperatureMatching;
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

    protected final List<Either<Either<SizedFluidIngredient, SizedGasIngredient>, Ingredient>> ingredients() {
        int size = ingredients.size() + fluidIngredients.size() + gasIngredients.size();
        List<Either<Either<SizedFluidIngredient, SizedGasIngredient>, Ingredient>> combined = new ArrayList<>(size);
        ingredients.forEach(ingredient -> combined.add(Either.right(ingredient)));
        fluidIngredients.forEach(ingredient -> combined.add(Either.left(Either.left(ingredient))));
        gasIngredients.forEach(ingredient -> combined.add(Either.left(Either.right(ingredient))));
        return combined;
    }

    protected final List<Either<Either<FluidStack, GasStack>, ProcessingOutput>> results() {
        int size = results.size() + fluidResults.size() + gasResults.size();
        List<Either<Either<FluidStack, GasStack>, ProcessingOutput>> combined = new ArrayList<>(size);
        results.forEach(result -> combined.add(Either.right(result)));
        fluidResults.forEach(result -> combined.add(Either.left(Either.left(result))));
        gasResults.forEach(result -> combined.add(Either.left(Either.right(result))));
        return combined;
    }

    protected final int processingDuration() {
        return processingDuration;
    }

    protected final TemperatureCondition temperatureCondition() {
        return temperatureCondition;
    }

    protected final TemperatureMatching temperatureMatching() {
        return temperatureMatching;
    }

    protected void encode(RegistryFriendlyByteBuf buffer) {
        CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).encode(buffer, ingredients);
        CatnipStreamCodecBuilders.nonNullList(SizedGasIngredient.STREAM_CODEC).encode(buffer, gasIngredients);
        CatnipStreamCodecBuilders.nonNullList(SizedFluidIngredient.STREAM_CODEC).encode(buffer, fluidIngredients);
        CatnipStreamCodecBuilders.nonNullList(ProcessingOutput.STREAM_CODEC).encode(buffer, results);
        CatnipStreamCodecBuilders.nonNullList(GasStack.STREAM_CODEC).encode(buffer, gasResults);
        CatnipStreamCodecBuilders.nonNullList(FluidStack.STREAM_CODEC).encode(buffer, fluidResults);
        ByteBufCodecs.VAR_INT.encode(buffer, processingDuration);
        TemperatureCondition.STREAM_CODEC.encode(buffer, temperatureCondition);
        TemperatureMatching.STREAM_CODEC.encode(buffer, temperatureMatching);
    }

    protected void decode(RegistryFriendlyByteBuf buffer) {
        ingredients = CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).decode(buffer);
        gasIngredients = CatnipStreamCodecBuilders.nonNullList(SizedGasIngredient.STREAM_CODEC).decode(buffer);
        fluidIngredients = CatnipStreamCodecBuilders.nonNullList(SizedFluidIngredient.STREAM_CODEC).decode(buffer);
        results = CatnipStreamCodecBuilders.nonNullList(ProcessingOutput.STREAM_CODEC).decode(buffer);
        gasResults = CatnipStreamCodecBuilders.nonNullList(GasStack.STREAM_CODEC).decode(buffer);
        fluidResults = CatnipStreamCodecBuilders.nonNullList(FluidStack.STREAM_CODEC).decode(buffer);
        processingDuration = ByteBufCodecs.VAR_INT.decode(buffer);
        temperatureCondition = TemperatureCondition.STREAM_CODEC.decode(buffer);
        temperatureMatching = TemperatureMatching.STREAM_CODEC.decode(buffer);
    }
}