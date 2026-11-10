package net.ty.createcraftedbeginning.compat.jei.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe.Builder;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class ReactorKettleRecipeConversion {
    private ReactorKettleRecipeConversion() {
    }

    public static RecipeHolder<ReactorKettleRecipe> convertToReactorKettleRecipe(RecipeHolder<?> sourceRecipe) {
        ResourceLocation id = sourceRecipe.id();
        Builder<ReactorKettleRecipe> recipeBuilder = new Builder<>(ReactorKettleRecipe::new, id);
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return new RecipeHolder<>(id, recipeBuilder.build());
        }

        Recipe<?> source = sourceRecipe.value();
        ReactorKettleRecipe convertedRecipe = recipeBuilder.withItemIngredients(source.getIngredients()).withSingleItemOutput(source.getResultItem(level.registryAccess())).build();
        return new RecipeHolder<>(id, convertedRecipe);
    }
}
