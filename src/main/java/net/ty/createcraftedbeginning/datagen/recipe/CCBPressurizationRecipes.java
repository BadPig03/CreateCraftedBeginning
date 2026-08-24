package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.PressurizationRecipeGen;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBPressurizationRecipes extends PressurizationRecipeGen {
    private final GeneratedRecipe PRESSURIZED_NATURAL = create("pressurized_natural", builder -> builder.require(CCBGases.NATURAL_AIR.get(), 10).output(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 1));
    private final GeneratedRecipe PRESSURIZED_ULTRAWARM = create("pressurized_ultrawarm", builder -> builder.require(CCBGases.ULTRAWARM_AIR.get(), 10).output(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 1));
    private final GeneratedRecipe PRESSURIZED_ETHEREAL = create("pressurized_ethereal", builder -> builder.require(CCBGases.ETHEREAL_AIR.get(), 10).output(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 1));
    private final GeneratedRecipe PRESSURIZED_STEAM = create("pressurized_steam", builder -> builder.require(CCBGases.STEAM.get(), 10).output(CCBGases.PRESSURIZED_STEAM.get(), 1));

    private final GeneratedRecipe PRESSURIZED_ENERGIZED_NATURAL = create("pressurized_energized_natural", builder -> builder.require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 10).output(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1));
    private final GeneratedRecipe PRESSURIZED_ENERGIZED_ULTRAWARM = create("pressurized_energized_ultrawarm", builder -> builder.require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 10).output(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 1));
    private final GeneratedRecipe PRESSURIZED_ENERGIZED_ETHEREAL = create("pressurized_energized_ethereal", builder -> builder.require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 10).output(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 1));

    public CCBPressurizationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
