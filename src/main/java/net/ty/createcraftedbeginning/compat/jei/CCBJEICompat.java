package net.ty.createcraftedbeginning.compat.jei;

import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureRecipeData;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBJEICompat {
    private static final int CHILLED_TEXT_COLOR = 0xFF20566B;

    private CCBJEICompat() {
    }

    public static int getTemperatureTextColor(TemperatureCondition condition) {
        return condition == TemperatureCondition.CHILLED ? CHILLED_TEXT_COLOR : condition.getColor();
    }

    public static Component getTemperatureLabel(TemperatureRecipeData temperature) {
        TemperatureCondition condition = temperature.condition();
        if (temperature.matching().effectiveFor(condition) == TemperatureMatching.EXACT) {
            return CCBLang.translateDirect(condition.getTranslationKey());
        }

        return CCBLang.translateDirect("recipe.temperature_matching.compatible." + condition.getSerializedName());
    }

    public static List<Component> getTemperatureTooltip(TemperatureRecipeData temperature) {
        TemperatureCondition condition = temperature.condition();
        TemperatureMatching matching = temperature.matching();
        String description = "recipe.temperature_matching.compatible.description";
        if (matching.effectiveFor(condition) == TemperatureMatching.EXACT) {
            description = "recipe.temperature_matching.exact.description";
        }
        else if (condition == TemperatureCondition.NONE) {
            description = "recipe.temperature_matching.compatible.none.description";
        }

        return List.of(CCBLang.translateDirect("recipe.temperature_matching." + matching.getSerializedName()), CCBLang.translateDirect(description).withStyle(ChatFormatting.GRAY));
    }

    public static void registerMysteriousItemConversions() {
        MysteriousItemConversionCategory.RECIPES.add(ConversionRecipe.create(new ItemStack(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK), new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK)));
    }
}
