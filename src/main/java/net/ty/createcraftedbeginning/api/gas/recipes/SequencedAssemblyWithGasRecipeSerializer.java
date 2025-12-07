package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedWithGasRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SequencedAssemblyWithGasRecipeSerializer implements RecipeSerializer<SequencedAssemblyWithGasRecipe> {
    public final StreamCodec<RegistryFriendlyByteBuf, SequencedAssemblyWithGasRecipe> STREAM_CODEC = StreamCodec.of(this::toNetwork, this::fromNetwork);
    @SuppressWarnings({"UnstableApiUsage", "removal"})
    private final MapCodec<SequencedAssemblyWithGasRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(SequencedAssemblyWithGasRecipe::getIngredient), ProcessingOutput.CODEC.fieldOf("transitional_item").forGetter(r -> r.transitionalItem), SequencedWithGasRecipe.CODEC.listOf().fieldOf("sequence").forGetter(SequencedAssemblyWithGasRecipe::getSequence), ProcessingOutput.CODEC.listOf().fieldOf("results").forGetter(r -> r.resultPool), ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("loops").forGetter(r -> Optional.of(r.getLoops()))).apply(i, (ingredient, transitionalItem, sequence, results, loops) -> {
        SequencedAssemblyWithGasRecipe recipe = new SequencedAssemblyWithGasRecipe(this);
        recipe.ingredient = ingredient;
        recipe.transitionalItem = transitionalItem;
        recipe.sequence.addAll(sequence);
        recipe.resultPool.addAll(results);
        recipe.loops = loops.orElse(5);
        for (int j = 0; j < recipe.sequence.size(); j++) {
            sequence.get(j).initFromSequencedAssembly(recipe, j == 0);
        }
        return recipe;
    }));

    protected void toNetwork(RegistryFriendlyByteBuf buffer, @NotNull SequencedAssemblyWithGasRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
        SequencedWithGasRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.getSequence());
        ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.resultPool);
        ProcessingOutput.STREAM_CODEC.encode(buffer, recipe.transitionalItem);
        buffer.writeInt(recipe.loops);
    }

    protected SequencedAssemblyWithGasRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        SequencedAssemblyWithGasRecipe recipe = new SequencedAssemblyWithGasRecipe(this);
        recipe.ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        recipe.getSequence().addAll(SequencedWithGasRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer));
        recipe.resultPool.addAll(ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer));
        recipe.transitionalItem = ProcessingOutput.STREAM_CODEC.decode(buffer);
        recipe.loops = buffer.readInt();
        return recipe;
    }

    @Override
    public @NotNull MapCodec<SequencedAssemblyWithGasRecipe> codec() {
        return CODEC;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, SequencedAssemblyWithGasRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
