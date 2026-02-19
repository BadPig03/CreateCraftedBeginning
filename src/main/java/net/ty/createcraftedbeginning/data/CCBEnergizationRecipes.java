package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.EnergizationRecipeGen;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBEnergizationRecipes extends EnergizationRecipeGen {
    GeneratedRecipe ENERGIZED_NATURAL = create("energized_natural", b -> b.require(CCBGases.NATURAL_AIR.get(), 1).output(CCBGases.ENERGIZED_NATURAL_AIR.get(), 1));
    GeneratedRecipe ENERGIZED_ULTRAWARM = create("energized_ultrawarm", b -> b.require(CCBGases.ULTRAWARM_AIR.get(), 1).output(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 1));
    GeneratedRecipe ENERGIZED_ETHEREAL = create("energized_ethereal", b -> b.require(CCBGases.ETHEREAL_AIR.get(), 1).output(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 1));

    GeneratedRecipe PRESSURIZED_ENERGIZED_NATURAL = create("pressurized_energized_natural", b -> b.require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 1).output(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1));
    GeneratedRecipe PRESSURIZED_ENERGIZED_ULTRAWARM = create("pressurized_energized_ultrawarm", b -> b.require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 1).output(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 1));
    GeneratedRecipe PRESSURIZED_ENERGIZED_ETHEREAL = create("pressurized_energized_ethereal", b -> b.require(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 1).output(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 1));

    public CCBEnergizationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
