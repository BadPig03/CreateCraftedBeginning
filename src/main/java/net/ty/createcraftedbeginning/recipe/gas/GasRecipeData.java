package net.ty.createcraftedbeginning.recipe.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.SizedGasIngredient;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasRecipeData(List<GasRecipeRequirement> requirements, List<GasStack> results) {
    public static final MapCodec<GasRecipeData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(GasRecipeRequirement.CODEC.listOf().optionalFieldOf("gas_ingredients", List.of()).forGetter(GasRecipeData::requirements), GasStack.CODEC.listOf().optionalFieldOf("gas_results", List.of()).forGetter(GasRecipeData::results)).apply(instance, GasRecipeData::new));
    public static final Codec<GasRecipeData> CODEC = MAP_CODEC.codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, GasRecipeData> STREAM_CODEC = StreamCodec.of((buffer, data) -> {
        CatnipStreamCodecBuilders.nonNullList(GasRecipeRequirement.STREAM_CODEC).encode(buffer, data.requirementList());
        CatnipStreamCodecBuilders.nonNullList(GasStack.STREAM_CODEC).encode(buffer, data.resultStacks());
    }, buffer -> new GasRecipeData(CatnipStreamCodecBuilders.nonNullList(GasRecipeRequirement.STREAM_CODEC).decode(buffer), CatnipStreamCodecBuilders.nonNullList(GasStack.STREAM_CODEC).decode(buffer)));

    public GasRecipeData {
        requirements = List.copyOf(requirements);
        results = results.stream().map(GasStack::copy).toList();
    }

    @Override
    public @Unmodifiable List<GasStack> results() {
        return results.stream().map(GasStack::copy).toList();
    }

    public NonNullList<GasRecipeRequirement> requirementList() {
        NonNullList<GasRecipeRequirement> requirementList = NonNullList.create();
        requirementList.addAll(requirements);
        return requirementList;
    }

    public NonNullList<SizedGasIngredient> sizedIngredients() {
        NonNullList<SizedGasIngredient> sizedIngredients = NonNullList.create();
        requirements.forEach(requirement -> sizedIngredients.add(requirement.sizedIngredient()));
        return sizedIngredients;
    }

    public NonNullList<GasStack> resultStacks() {
        NonNullList<GasStack> resultStacks = NonNullList.create();
        results.forEach(gas -> resultStacks.add(gas.copy()));
        return resultStacks;
    }
}
