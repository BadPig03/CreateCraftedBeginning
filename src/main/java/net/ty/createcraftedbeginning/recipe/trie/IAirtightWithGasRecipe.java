package net.ty.createcraftedbeginning.recipe.trie;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IAirtightWithGasRecipe {
    NonNullList<SizedFluidIngredient> getFluidIngredients();

    NonNullList<SizedGasIngredient> getGasIngredients();
}
