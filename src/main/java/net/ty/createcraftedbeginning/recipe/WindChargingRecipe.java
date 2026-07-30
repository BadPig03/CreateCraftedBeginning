package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.PossibleEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WindChargingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    private static final WindChargingData EMPTY = new WindChargingData(0, 0, false, false);

    public WindChargingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.WIND_CHARGING, params);
    }

    private static @Nullable WindChargingRecipe findRecipe(Level level, ItemStack itemStack) {
        List<RecipeHolder<WindChargingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.WIND_CHARGING.getType());
        for (RecipeHolder<WindChargingRecipe> holder : recipes) {
            WindChargingRecipe recipe = holder.value();
            if (recipe.getIngredient().test(itemStack)) {
                return recipe;
            }
        }
        return null;
    }

    public static WindChargingData getWindChargingTime(Level level, ItemStack itemStack) {
        WindChargingRecipe recipe = findRecipe(level, itemStack);
        if (recipe != null) {
            return new WindChargingData(recipe.processingDuration, 1, recipe.isBadFood(), recipe.isMilky());
        }
        return getAutomaticWindChargingTime(itemStack);
    }

    public static ItemStack getRecipeResult(Level level, ItemStack itemStack) {
        WindChargingRecipe recipe = findRecipe(level, itemStack);
        return recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess()).copy();
    }

    public static WindChargingData getAutomaticWindChargingTime(ItemStack stack) {
        Item item = stack.getItem();
        FoodProperties properties = item.getFoodProperties(stack, null);
        if (properties == null || CCBItemTags.WIND_CHARGING_EXCLUDED.matches(stack)) {
            return EMPTY;
        }

        double foodValue = 0.5 * properties.nutrition() + properties.saturation();
        if (foodValue <= 0) {
            return EMPTY;
        }

        long effectScore = getEffectScore(properties.effects());
        double calculatedTime = Math.pow(foodValue, 1.50504) * 100 * (Math.abs(effectScore) + 1);
        long baseTime = !Double.isFinite(calculatedTime) || calculatedTime >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (long) Math.ceil(calculatedTime);
        if (baseTime <= 0) {
            return EMPTY;
        }

        long duration = effectScore < 0 ? -Math.min(Integer.MAX_VALUE, baseTime * 2) : baseTime;
        int chargingTime = (int) duration;
        return new WindChargingData(chargingTime, 1, chargingTime < 0, false);
    }

    private static long getEffectScore(List<PossibleEffect> effects) {
        long score = 0;
        for (PossibleEffect effect : effects) {
            MobEffectCategory category = effect.effect().getEffect().value().getCategory();
            long amplifier = effect.effect().getAmplifier() + 1;
            switch (category) {
                case BENEFICIAL -> score += amplifier;
                case HARMFUL -> score -= amplifier;
                default -> {
                }
            }
        }
        return score;
    }

    public Ingredient getIngredient() {
        return ingredients.getFirst();
    }

    public boolean isBadFood() {
        return processingDuration < 0;
    }

    public boolean isMilky() {
        return processingDuration == 0;
    }

    public boolean isCreativeIceCream() {
        return getIngredient().getItems()[0].getItem() instanceof CreativeIceCreamItem;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return true;
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
    protected boolean canSpecifyDuration() {
        return true;
    }

    public record WindChargingData(int time, int amount, boolean isBadFood, boolean isMilky) {}
}
