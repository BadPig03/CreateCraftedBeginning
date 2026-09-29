package net.ty.createcraftedbeginning.compat.jei.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
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
        Builder<ReactorKettleRecipe> recipeBuilder = new Builder<>(ReactorKettleRecipe::new, sourceRecipe.id());
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return new RecipeHolder<>(sourceRecipe.id(), recipeBuilder.build());
        }

        ReactorKettleRecipe convertedRecipe = recipeBuilder.withItemIngredients(sourceRecipe.value().getIngredients()).withSingleItemOutput(sourceRecipe.value().getResultItem(level.registryAccess())).build();
        return new RecipeHolder<>(sourceRecipe.id(), convertedRecipe);
    }
}
