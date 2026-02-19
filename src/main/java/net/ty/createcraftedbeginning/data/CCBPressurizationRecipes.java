package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.PressurizationRecipeGen;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBPressurizationRecipes extends PressurizationRecipeGen {
    GeneratedRecipe PRESSURIZED_NATURAL = create("pressurized_natural", b -> b.require(CCBGases.NATURAL_AIR.get(), 10).output(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 1));
    GeneratedRecipe PRESSURIZED_ULTRAWARM = create("pressurized_ultrawarm", b -> b.require(CCBGases.ULTRAWARM_AIR.get(), 10).output(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 1));
    GeneratedRecipe PRESSURIZED_ETHEREAL = create("pressurized_ethereal", b -> b.require(CCBGases.ETHEREAL_AIR.get(), 10).output(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 1));

    GeneratedRecipe PRESSURIZED_ENERGIZED_NATURAL = create("pressurized_energized_natural", b -> b.require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 10).output(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1));
    GeneratedRecipe PRESSURIZED_ENERGIZED_ULTRAWARM = create("pressurized_energized_ultrawarm", b -> b.require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 10).output(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 1));
    GeneratedRecipe PRESSURIZED_ENERGIZED_ETHEREAL = create("pressurized_energized_ethereal", b -> b.require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 10).output(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 1));

    public CCBPressurizationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
