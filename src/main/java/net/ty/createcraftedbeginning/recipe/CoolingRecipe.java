package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CoolingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public CoolingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.COOLING, params);
    }

    public static CoolingData getCoolingTime(Level level, @Nullable ItemStack itemStack, @Nullable FluidStack fluidStack) {
        for (RecipeHolder<CoolingRecipe> recipeHolder : level.getRecipeManager().<SingleRecipeInput, CoolingRecipe>getAllRecipesFor(CCBRecipeTypes.COOLING.getType())) {
            CoolingRecipe recipe = recipeHolder.value();
            boolean usesFluid = recipe.isFluidIngredients();
            if (usesFluid && (itemStack != null || fluidStack == null)) {
                continue;
            }

            if (!usesFluid && (itemStack == null || fluidStack != null)) {
                continue;
            }

            if (usesFluid && !recipe.getFluidIngredient().ingredient().test(fluidStack)) {
                continue;
            }

            if (!usesFluid && !recipe.getIngredient().test(itemStack)) {
                continue;
            }

            return new CoolingData(recipe.processingDuration, usesFluid ? recipe.fluidIngredients.getFirst().amount() : 1);
        }
        return new CoolingData(0, 0);
    }

    public SizedFluidIngredient getFluidIngredient() {
        return fluidIngredients.getFirst();
    }

    public Ingredient getIngredient() {
        return ingredients.getFirst();
    }

    public boolean isFluidIngredients() {
        return ingredients.isEmpty() && !fluidIngredients.isEmpty();
    }

    public boolean isCreativeIceCream() {
        if (ingredients.isEmpty()) {
            return false;
        }

        ItemStack[] ingredientStacks = getIngredient().getItems();
        return ingredientStacks.length > 0 && Arrays.stream(ingredientStacks).allMatch(itemStack -> itemStack.getItem() instanceof CreativeCoolingSource);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !isFluidIngredients() && !input.isEmpty() && !ingredients.isEmpty() && ingredients.getFirst().test(input.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 0;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 1;
    }

    public record CoolingData(int time, int amount) {
        public static final CoolingData EMPTY = new CoolingData(0, 0);
    }
}
