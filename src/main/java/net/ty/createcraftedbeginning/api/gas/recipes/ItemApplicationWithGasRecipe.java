package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ItemApplicationWithGasRecipe extends ProcessingWithGasRecipe<RecipeWrapper, ItemApplicationWithGasRecipeParams> {
    private final boolean keepHeldItem;

    public ItemApplicationWithGasRecipe(CCBRecipeTypes type, ItemApplicationWithGasRecipeParams params) {
        super(type, params);
        keepHeldItem = params.keepHeldItem;
    }

    @Override
    public boolean matches(@NotNull RecipeWrapper inv, @NotNull Level level) {
        return getProcessedItem().test(inv.getItem(0)) && getRequiredHeldItem().test(inv.getItem(1));
    }

    public Ingredient getRequiredHeldItem() {
        if (ingredients.size() < 2) {
            throw new IllegalStateException("Item Application Recipe has no tool!");
        }

        return ingredients.get(1);
    }

    public Ingredient getProcessedItem() {
        if (ingredients.isEmpty()) {
            throw new IllegalStateException("Item Application Recipe has no ingredient!");
        }

        return ingredients.getFirst();
    }

    @Override
    protected int getMaxInputCount() {
        return 2;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    public boolean shouldKeepHeldItem() {
        return keepHeldItem;
    }

    @FunctionalInterface
    public interface Factory<R extends ItemApplicationWithGasRecipe> extends ProcessingWithGasRecipe.Factory<ItemApplicationWithGasRecipeParams, R> {
        @Override
        R create(ItemApplicationWithGasRecipeParams params);
    }

    public static class Builder<R extends ItemApplicationWithGasRecipe> extends ProcessingWithGasRecipeBuilder<ItemApplicationWithGasRecipeParams, R, Builder<R>> {
        public Builder(Factory<R> factory, ResourceLocation recipeId) {
            super(factory, recipeId);
        }

        @Override
        protected ItemApplicationWithGasRecipeParams createParams() {
            return new ItemApplicationWithGasRecipeParams();
        }

        @Override
        public Builder<R> self() {
            return this;
        }

        public Builder<R> toolNotConsumed() {
            params.keepHeldItem = true;
            return this;
        }
    }

    public static class Serializer<R extends ItemApplicationWithGasRecipe> implements RecipeSerializer<R> {
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(ProcessingWithGasRecipe.Factory<ItemApplicationWithGasRecipeParams, R> factory) {
            codec = ProcessingWithGasRecipe.codec(factory, ItemApplicationWithGasRecipeParams.CODEC);
            streamCodec = ProcessingWithGasRecipe.streamCodec(factory, ItemApplicationWithGasRecipeParams.STREAM_CODEC);
        }

        @Override
        public @NotNull MapCodec<R> codec() {
            return codec;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return streamCodec;
        }
    }
}
