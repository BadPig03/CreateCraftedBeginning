package net.ty.createcraftedbeginning.recipe.gas;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedWithGasRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SequencedAssemblyWithGasRecipeSerializer implements RecipeSerializer<SequencedAssemblyWithGasRecipe> {
    private final StreamCodec<RegistryFriendlyByteBuf, SequencedAssemblyWithGasRecipe> STREAM_CODEC = StreamCodec.of(SequencedAssemblyWithGasRecipeSerializer::toNetwork, this::fromNetwork);
    private final MapCodec<SequencedAssemblyWithGasRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(SequencedAssemblyWithGasRecipe::getIngredient), ProcessingOutput.CODEC_NEW.fieldOf("transitional_item").forGetter(recipe -> recipe.transitionalItem), SequencedWithGasRecipe.CODEC.listOf().fieldOf("sequence").forGetter(SequencedAssemblyWithGasRecipe::getSequence), ProcessingOutput.CODEC_NEW.listOf().fieldOf("results").forGetter(recipe -> recipe.resultPool), ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("loops").forGetter(recipe -> Optional.of(recipe.getLoops()))).apply(instance, (ingredient, transitionalItem, sequence, results, loops) -> {
        SequencedAssemblyWithGasRecipe recipe = new SequencedAssemblyWithGasRecipe(this);
        recipe.ingredient = ingredient;
        recipe.transitionalItem = transitionalItem;
        recipe.sequence.addAll(sequence);
        recipe.resultPool.addAll(results);
        recipe.loops = loops.orElse(5);
        for (int index = 0; index < recipe.sequence.size(); index++) {
            sequence.get(index).initFromSequencedAssembly(recipe, index == 0);
        }
        return recipe;
    }));

    private static void toNetwork(RegistryFriendlyByteBuf buffer, SequencedAssemblyWithGasRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        SequencedWithGasRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.getSequence());
        ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.resultPool);
        ProcessingOutput.STREAM_CODEC.encode(buffer, recipe.transitionalItem);
        buffer.writeInt(recipe.loops);
    }

    @Override
    public MapCodec<SequencedAssemblyWithGasRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SequencedAssemblyWithGasRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private SequencedAssemblyWithGasRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        SequencedAssemblyWithGasRecipe recipe = new SequencedAssemblyWithGasRecipe(this);
        recipe.ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        recipe.getSequence().addAll(SequencedWithGasRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer));
        recipe.resultPool.addAll(ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer));
        recipe.transitionalItem = ProcessingOutput.STREAM_CODEC.decode(buffer);
        recipe.loops = buffer.readInt();
        for (int stepIndex = 0; stepIndex < recipe.getSequence().size(); stepIndex++) {
            recipe.getSequence().get(stepIndex).initFromSequencedAssembly(recipe, stepIndex == 0);
        }
        return recipe;
    }
}
