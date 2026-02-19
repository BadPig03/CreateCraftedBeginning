package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WindChargingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public WindChargingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.WIND_CHARGING, params);
    }

    public static @NotNull WindChargingData getWindChargingTime(@NotNull Level level, ItemStack itemStack) {
        List<RecipeHolder<WindChargingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.WIND_CHARGING.getType());
        for (RecipeHolder<WindChargingRecipe> holder : recipes) {
            WindChargingRecipe recipe = holder.value();
            if (!recipe.getIngredient().test(itemStack)) {
                continue;
            }

            return new WindChargingData(recipe.processingDuration, 1, recipe.isBadFood(), recipe.isMilkyItem());
        }

        return new WindChargingData(0, 0, false, false);
    }

    public Ingredient getIngredient() {
        return ingredients.getFirst();
    }

    public boolean isBadFood() {
        return processingDuration < 0;
    }

    public boolean isMilkyItem() {
        return processingDuration == 0;
    }

    public boolean isCreativeIceCream() {
        return getIngredient().getItems()[0].getItem() instanceof CreativeIceCreamItem;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        return true;
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

    public record WindChargingData(int time, int amount, boolean isBadFood, boolean isMilky) {}
}
