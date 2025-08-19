package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

public class SuperCoolingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public SuperCoolingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.SUPER_COOLING, params);
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput inv, @NotNull Level worldIn) {
        return true;
    }

    public boolean isIngredientsFluid() {
        return ingredients.isEmpty() && !fluidIngredients.isEmpty();
    }

    public FluidIngredient getIngredientsFluid() {
        return fluidIngredients.getFirst();
    }

    public Ingredient getIngredientsItem() {
        return ingredients.getFirst();
    }

    public int getResultTime() {
        return fluidResults.getFirst().getAmount();
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
