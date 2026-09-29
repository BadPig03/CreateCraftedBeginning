package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.recipe.interfaces.CreativeCoolingSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CoolingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    CoolingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.COOLING, params);
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
}
