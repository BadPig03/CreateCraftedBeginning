package net.ty.createcraftedbeginning.platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.ty.createcraftedbeginning.platform.access.SmithingRecipeAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SmithingRecipeBridge {
    private SmithingRecipeBridge() {
    }

    public static @Nullable Ingredients getIngredients(Recipe<?> recipe) {
        if (!(recipe instanceof SmithingTransformRecipe || recipe instanceof SmithingTrimRecipe) || !(recipe instanceof SmithingRecipeAccess access)) {
            return null;
        }

        return new Ingredients(access.ccb$getTemplate(), access.ccb$getBase(), access.ccb$getAddition());
    }

    public record Ingredients(Ingredient template, Ingredient base, Ingredient addition) {}
}
