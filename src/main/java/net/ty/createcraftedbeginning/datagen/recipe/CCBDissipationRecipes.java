package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.DissipationRecipeGen;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBDissipationRecipes extends DissipationRecipeGen {
    private final GeneratedRecipe NATURAL = create("natural", builder -> builder.require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 1).output(CCBGases.NATURAL_AIR.get(), 1));
    private final GeneratedRecipe ULTRAWARM = create("ultrawarm", builder -> builder.require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 1).output(CCBGases.ULTRAWARM_AIR.get(), 1));
    private final GeneratedRecipe ETHEREAL = create("ethereal", builder -> builder.require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 1).output(CCBGases.ETHEREAL_AIR.get(), 1));

    public CCBDissipationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
