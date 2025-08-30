package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PressurizationRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public PressurizationRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.PRESSURIZATION, params);
    }



    public static FluidStack getIngredientFluidStack(@NotNull Level level, FluidStack fluidStack) {
        List<RecipeHolder<PressurizationRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.PRESSURIZATION.getType());
        for (RecipeHolder<PressurizationRecipe> holder : recipes) {
            PressurizationRecipe recipe = holder.value();

            if (recipe.isIngredientEmpty()) {
                continue;
            }

            FluidIngredient fluidIngredient = recipe.getIngredientsFluid();
            FluidStack ingredientFluidStack = fluidIngredient.getMatchingFluidStacks().getFirst();
            FluidStack resultFluidStack = recipe.getResultingFluid();

            if (Helpers.isFluidTheSame(ingredientFluidStack, fluidStack)) {
                return resultFluidStack.copyWithAmount(ingredientFluidStack.getAmount());
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput inv, @NotNull Level worldIn) {
        return true;
    }

    public boolean isIngredientEmpty() {
        return fluidIngredients.isEmpty();
    }

    public FluidIngredient getIngredientsFluid() {
        if (isIngredientEmpty()) {
            throw new IllegalStateException("Pressurization Recipe has no fluid ingredient!");
        }
        return fluidIngredients.getFirst();
    }

    public FluidStack getResultingFluid() {
        if (fluidResults.isEmpty()) {
            throw new IllegalStateException("Pressurization Recipe has no fluid result!");
        }
        return fluidResults.getFirst();
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 1;
    }
}
