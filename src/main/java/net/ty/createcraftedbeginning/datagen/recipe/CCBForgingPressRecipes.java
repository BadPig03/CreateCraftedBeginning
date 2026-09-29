package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.ForgingPressRecipeGen;
import net.ty.createcraftedbeginning.recipe.pressure.CCBPressureRequirements;
import net.ty.createcraftedbeginning.registry.CCBTags;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBForgingPressRecipes extends ForgingPressRecipeGen {
    private final GeneratedRecipe DIAMOND_FROM_CHARCOAL = create("diamond_from_charcoal", builder -> builder.require(Items.CHARCOAL).require(Items.HEAVY_CORE).require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 2500, CCBPressureRequirements.PRESSURIZED).output(0.2F, Items.DIAMOND));
    private final GeneratedRecipe IRON_BARS_FROM_IRON_SHEET = create("iron_bars_from_iron_sheet", builder -> builder.require(CCBTags.commonItemTag("plates/iron")).require(Items.IRON_BARS).output(Items.IRON_BARS, 4));
    private final GeneratedRecipe HEAVY_WEIGHTED_PRESSURE_PLATE = create("heavy_weighted_pressure_plate", builder -> builder.require(CCBTags.commonItemTag("plates/iron")).require(Items.IRON_TRAPDOOR).output(Items.HEAVY_WEIGHTED_PRESSURE_PLATE));
    private final GeneratedRecipe LIGHT_WEIGHTED_PRESSURE_PLATE = create("light_weighted_pressure_plate", builder -> builder.require(CCBTags.commonItemTag("plates/gold")).require(Items.IRON_TRAPDOOR).output(Items.LIGHT_WEIGHTED_PRESSURE_PLATE));
    private final GeneratedRecipe IRON_NUGGETS_FROM_IRON_SHEET = create("iron_nuggets_from_iron_sheet", builder -> builder.require(CCBTags.commonItemTag("plates/iron")).require(Items.IRON_TRAPDOOR).output(Items.IRON_NUGGET, 9));
    private final GeneratedRecipe GOLD_NUGGETS_FROM_GOLD_SHEET = create("gold_nuggets_from_gold_sheet", builder -> builder.require(CCBTags.commonItemTag("plates/gold")).require(Items.IRON_TRAPDOOR).output(Items.GOLD_NUGGET, 9));

    public CCBForgingPressRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
