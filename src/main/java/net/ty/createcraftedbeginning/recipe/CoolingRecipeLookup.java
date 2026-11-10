package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CoolingRecipeLookup {
    private CoolingRecipeLookup() {
    }

    public static CoolingData findCoolingData(Level level, @Nullable ItemStack itemStack, @Nullable FluidStack fluidStack) {
        for (RecipeHolder<CoolingRecipe> recipeHolder : level.getRecipeManager().<SingleRecipeInput, CoolingRecipe>getAllRecipesFor(CCBRecipeTypes.COOLING.getType())) {
            CoolingRecipe recipe = recipeHolder.value();
            boolean usesFluid = recipe.isFluidIngredients();
            if (usesFluid && (itemStack != null || fluidStack == null)) {
                continue;
            }

            if (!usesFluid && (itemStack == null || fluidStack != null)) {
                continue;
            }

            if (!usesFluid) {
                if (!recipe.getIngredient().test(itemStack)) {
                    continue;
                }

                return new CoolingData(recipe.getProcessingDuration(), 1);
            }

            SizedFluidIngredient ingredient = recipe.getFluidIngredient();
            if (!ingredient.ingredient().test(fluidStack)) {
                continue;
            }

            return new CoolingData(recipe.getProcessingDuration(), ingredient.amount());
        }

        return new CoolingData(0, 0);
    }

    public record CoolingData(int time, int amount) {
        public static final CoolingData EMPTY = new CoolingData(0, 0);
    }
}
