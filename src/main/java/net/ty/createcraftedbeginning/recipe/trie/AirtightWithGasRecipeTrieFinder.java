package net.ty.createcraftedbeginning.recipe.trie;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie.Builder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightWithGasRecipeTrieFinder {
    private static final Cache<CacheKey, AirtightWithGasRecipeTrie<?>> CACHED_TRIES = CacheBuilder.newBuilder().maximumSize(16).build();

    public static AirtightWithGasRecipeTrie<?> get(Object cacheKey, Level level, Predicate<RecipeHolder<? extends Recipe<?>>> conditions) throws ExecutionException {
        CacheKey scopedKey = new CacheKey(cacheKey, level.getRecipeManager());
        return CACHED_TRIES.get(scopedKey, () -> {
            Builder<Recipe<?>> builder = AirtightWithGasRecipeTrie.builder();
            List<RecipeHolder<? extends Recipe<?>>> recipes = RecipeFinder.get(scopedKey, level, conditions);
            for (RecipeHolder<? extends Recipe<?>> holder : recipes) {
                builder.insert(holder.value());
            }
            return builder.build();
        });
    }

    public static void invalidateCaches() {
        CACHED_TRIES.invalidateAll();
    }

    private record CacheKey(Object scope, RecipeManager recipeManager) {}
}
