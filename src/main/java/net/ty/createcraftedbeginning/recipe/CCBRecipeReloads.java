package net.ty.createcraftedbeginning.recipe;

import com.google.common.collect.MapMaker;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.RecipeManager;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressRecipeLookup;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerRecipeLookup;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleRecipeLookup;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex;
import net.ty.createcraftedbeginning.recipe.trie.AirtightRecipeTrieFinder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ConcurrentMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBRecipeReloads {
    private static final ConcurrentMap<RecipeManager, Generation> GENERATIONS = new MapMaker().weakKeys().makeMap();

    private CCBRecipeReloads() {
    }

    public static Generation getGeneration(RecipeManager manager) {
        return GENERATIONS.computeIfAbsent(manager, ignored -> new Generation());
    }

    public static void onDatapackSync(RecipeManager manager, boolean playerSync) {
        if (playerSync) {
            if (!BreezeChamberRecipeIndex.isIndexed(manager)) {
                BreezeChamberRecipeIndex.rebuild(manager);
            }
            return;
        }

        BreezeChamberRecipeIndex.rebuild(manager);
        ResidueRecipeLookup.invalidateCaches();
        GasInjectionRecipeLookup.invalidateRecipeCaches();
        AirtightRecipeTrieFinder.invalidateCaches();
        AirtightReactorKettleRecipeLookup.invalidateRecipeCaches();
        AirtightForgingPressRecipeLookup.invalidateRecipeCaches();
        AirtightFractionationTowerRecipeLookup.invalidateRecipeCaches();
        GENERATIONS.put(manager, new Generation());
    }

    public static void onClientRecipesUpdated(RecipeManager manager) {
        BreezeChamberRecipeIndex.rebuild(manager);
        ReactorKettleBrewingRecipes.invalidateCaches(manager);
        GENERATIONS.put(manager, new Generation());
    }

    public static final class Generation {
        private Generation() {
        }
    }
}
