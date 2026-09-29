package net.ty.createcraftedbeginning.recipe.trie;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.recipe.trie.AirtightRecipeTrie.Builder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightRecipeTrieFinder {
    private static final Cache<CacheKey, AirtightRecipeTrie<?>> CACHED_TRIES = CacheBuilder.newBuilder().maximumSize(16).build();
    private static final Map<RecipeManager, Set<Object>> FAILED_TRIES = new WeakHashMap<>();

    private AirtightRecipeTrieFinder() {
    }

    public static AirtightRecipeTrie<?> get(Object cacheKey, Level level, Predicate<RecipeHolder<? extends Recipe<?>>> conditions) throws ExecutionException {
        return get(cacheKey, level, conditions, RecipeHolder::value);
    }

    public static AirtightRecipeTrie<?> get(Object cacheKey, Level level, Predicate<RecipeHolder<? extends Recipe<?>>> conditions, Function<RecipeHolder<? extends Recipe<?>>, Recipe<?>> converter) throws ExecutionException {
        CacheKey scopedKey = new CacheKey(cacheKey, level.getRecipeManager());
        return CACHED_TRIES.get(scopedKey, () -> {
            Builder<Recipe<?>> builder = AirtightRecipeTrie.builder();
            for (RecipeHolder<? extends Recipe<?>> recipeHolder : RecipeFinder.get(scopedKey, level, conditions)) {
                builder.insert(converter.apply(recipeHolder));
            }
            return builder.build();
        });
    }

    public static boolean hasFailed(Object cacheKey, Level level) {
        RecipeManager recipeManager = level.getRecipeManager();
        synchronized (FAILED_TRIES) {
            Set<Object> failedScopes = FAILED_TRIES.get(recipeManager);
            return failedScopes != null && failedScopes.contains(cacheKey);
        }
    }

    public static boolean recordFailure(Object cacheKey, Level level) {
        RecipeManager recipeManager = level.getRecipeManager();
        synchronized (FAILED_TRIES) {
            return FAILED_TRIES.computeIfAbsent(recipeManager, ignored -> new HashSet<>()).add(cacheKey);
        }
    }

    public static void invalidateFailures(Object cacheKey) {
        synchronized (FAILED_TRIES) {
            Iterator<Set<Object>> failedScopeIterator = FAILED_TRIES.values().iterator();
            while (failedScopeIterator.hasNext()) {
                Set<Object> failedScopes = failedScopeIterator.next();
                failedScopes.remove(cacheKey);
                if (!failedScopes.isEmpty()) {
                    continue;
                }

                failedScopeIterator.remove();
            }
        }
    }

    public static void invalidateCaches() {
        CACHED_TRIES.invalidateAll();
        synchronized (FAILED_TRIES) {
            FAILED_TRIES.clear();
        }
    }

    private record CacheKey(Object scope, RecipeManager recipeManager) {}
}
