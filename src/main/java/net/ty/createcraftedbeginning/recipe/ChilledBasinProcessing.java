package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChilledBasinProcessing {
    private ChilledBasinProcessing() {
    }

    public static boolean isChilledRecipe(Recipe<?> recipe) {
        return recipe instanceof ChilledBasinRecipe;
    }

    public static List<Recipe<?>> prioritizeChilledRecipes(List<Recipe<?>> recipes) {
        if (recipes.size() < 2) {
            return recipes;
        }

        List<Recipe<?>> chilled = new ArrayList<>();
        List<Recipe<?>> regular = new ArrayList<>();
        for (Recipe<?> recipe : recipes) {
            (isChilledRecipe(recipe) ? chilled : regular).add(recipe);
        }
        if (chilled.isEmpty() || regular.isEmpty()) {
            return recipes;
        }

        chilled.addAll(regular);
        return chilled;
    }
}
