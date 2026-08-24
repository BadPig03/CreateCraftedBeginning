package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterUtils;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleRecipeFilter {
    private AirtightReactorKettleRecipeFilter() {
    }

    public static boolean matches(AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe) {
        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        ItemStack filterItem = kettle.getRecipeFilter();
        if (GasFilterUtils.isFilter(filterItem)) {
            GasStack primaryGasResult = recipe.getPrimaryGasResult();
            return !primaryGasResult.isEmpty() && GasFilterUtils.matches(filterItem, primaryGasResult);
        }

        if (!recipe.getRollableResults().isEmpty() || recipe.getFluidResults().isEmpty()) {
            return kettle.testRecipeFilter(recipe.getResultItem(level.registryAccess()));
        }
        return kettle.testRecipeFilter(recipe.getFluidResults().getFirst());
    }
}
