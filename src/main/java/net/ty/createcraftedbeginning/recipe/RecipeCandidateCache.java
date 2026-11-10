package net.ty.createcraftedbeginning.recipe;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.ty.createcraftedbeginning.recipe.CCBRecipeReloads.Generation;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class RecipeCandidateCache {
    private static final int MAX_CACHED_SEARCHES = 16;
    private static final Cache<CacheKey, List<RecipeHolder<?>>> CACHED_SEARCHES = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SEARCHES).build();

    private RecipeCandidateCache() {
    }

    public static List<RecipeHolder<?>> get(Object scope, RecipeManager manager, Predicate<RecipeHolder<? extends Recipe<?>>> conditions) {
        CacheKey key = new CacheKey(scope, CCBRecipeReloads.getGeneration(manager));
        return CACHED_SEARCHES.asMap().computeIfAbsent(key, ignored -> manager.getRecipes().stream().filter(holder -> conditions.test(holder)).toList());
    }

    private record CacheKey(Object scope, Generation generation) {}
}
