package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.ty.createcraftedbeginning.recipe.CCBRecipeReloads;
import net.ty.createcraftedbeginning.recipe.CCBRecipeReloads.Generation;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightFractionationTowerRecipeLookup {
    private static final Map<RecipeManager, RecipeCache> RECIPE_CACHES = new WeakHashMap<>();

    private AirtightFractionationTowerRecipeLookup() {
    }

    public static synchronized void invalidateRecipeCaches() {
        RECIPE_CACHES.clear();
    }

    @Internal
    public static synchronized List<RecipeHolder<FractionationTowerRecipe>> getRecipes(RecipeManager manager) {
        return getCache(manager).recipes();
    }

    static synchronized @Nullable FractionationTowerRecipe findRecipe(RecipeManager manager, ResourceLocation id) {
        return getCache(manager).byId().get(id);
    }

    private static RecipeCache getCache(RecipeManager manager) {
        Generation generation = CCBRecipeReloads.getGeneration(manager);
        RecipeCache cached = RECIPE_CACHES.get(manager);
        if (cached != null && cached.generation() == generation) {
            return cached;
        }

        List<RecipeHolder<FractionationTowerRecipe>> candidates = manager.getAllRecipesFor(CCBRecipeTypes.FRACTIONATION_TOWER.getType());
        List<RecipeHolder<FractionationTowerRecipe>> recipes = candidates.stream().filter(holder -> holder.value().validate().isEmpty()).sorted(Comparator.comparing(holder -> holder.id().toString())).toList();
        Map<ResourceLocation, FractionationTowerRecipe> byId = new HashMap<>();
        for (RecipeHolder<FractionationTowerRecipe> holder : recipes) {
            byId.put(holder.id(), holder.value());
        }
        RecipeCache rebuilt = new RecipeCache(generation, recipes, Map.copyOf(byId));
        RECIPE_CACHES.put(manager, rebuilt);
        return rebuilt;
    }

    private record RecipeCache(Generation generation, List<RecipeHolder<FractionationTowerRecipe>> recipes, Map<ResourceLocation, FractionationTowerRecipe> byId) {}
}
