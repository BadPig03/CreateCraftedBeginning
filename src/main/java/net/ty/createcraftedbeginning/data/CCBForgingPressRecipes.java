package net.ty.createcraftedbeginning.data;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.ForgingPressRecipeGen;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBForgingPressRecipes extends ForgingPressRecipeGen {
    GeneratedRecipe DIAMOND_FROM_CHARCOAL = create("diamond_from_charcoal", builder -> builder.require(Items.CHARCOAL).require(Items.HEAVY_CORE).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 250).output(0.2f, Items.DIAMOND));

    public CCBForgingPressRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
