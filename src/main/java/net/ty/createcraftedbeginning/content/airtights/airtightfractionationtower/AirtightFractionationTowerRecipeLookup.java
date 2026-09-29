package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
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
        return RECIPE_CACHES.computeIfAbsent(manager, recipeManager -> {
            List<RecipeHolder<FractionationTowerRecipe>> candidates = recipeManager.getAllRecipesFor(CCBRecipeTypes.FRACTIONATION_TOWER.getType());
            List<RecipeHolder<FractionationTowerRecipe>> recipes = candidates.stream().filter(holder -> holder.value().validate().isEmpty()).sorted(Comparator.comparing(holder -> holder.id().toString())).toList();
            Map<ResourceLocation, FractionationTowerRecipe> byId = new HashMap<>();
            for (RecipeHolder<FractionationTowerRecipe> holder : recipes) {
                byId.put(holder.id(), holder.value());
            }
            return new RecipeCache(recipes, Map.copyOf(byId));
        });
    }

    private record RecipeCache(List<RecipeHolder<FractionationTowerRecipe>> recipes, Map<ResourceLocation, FractionationTowerRecipe> byId) {}
}
