package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.GasInjectionRecipeGen;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBGasInjectionRecipes extends GasInjectionRecipeGen {
    GeneratedRecipe WIND_CHARGE = create("wind_charge", b -> b.require(Items.BLAZE_POWDER).require(CCBGases.NATURAL_AIR.get(), 250).output(Items.WIND_CHARGE, 2));
    GeneratedRecipe BREEZE_ROD = create("breeze_rod", b -> b.require(Items.BLAZE_ROD).require(CCBGases.NATURAL_AIR.get(), 500).output(Items.BREEZE_ROD));

    public CCBGasInjectionRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
