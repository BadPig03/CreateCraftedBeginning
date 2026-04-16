package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.DissipationRecipeGen;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBDissipationRecipes extends DissipationRecipeGen {
    GeneratedRecipe NATURAL = create("natural", b -> b.require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 1).output(CCBGases.NATURAL_AIR.get(), 1));
    GeneratedRecipe ULTRAWARM = create("ultrawarm", b -> b.require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 1).output(CCBGases.ULTRAWARM_AIR.get(), 1));
    GeneratedRecipe ETHEREAL = create("ethereal", b -> b.require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 1).output(CCBGases.ETHEREAL_AIR.get(), 1));

    GeneratedRecipe PRESSURIZED_NATURAL = create("pressurized_natural", b -> b.require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 1));
    GeneratedRecipe PRESSURIZED_ULTRAWARM = create("pressurized_ultrawarm", b -> b.require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 1).output(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 1));
    GeneratedRecipe PRESSURIZED_ETHEREAL = create("pressurized_ethereal", b -> b.require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 1).output(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 1));

    public CCBDissipationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
