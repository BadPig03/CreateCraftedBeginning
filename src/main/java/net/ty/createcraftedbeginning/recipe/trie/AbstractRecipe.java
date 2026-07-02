package net.ty.createcraftedbeginning.recipe.trie;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AbstractRecipe<R extends Recipe<?>> {
    final R recipe;
    final Set<AbstractIngredient> ingredients;

    public AbstractRecipe(R recipe, Set<AbstractIngredient> ingredients) {
        this.recipe = recipe;
        this.ingredients = ingredients;
    }
}
