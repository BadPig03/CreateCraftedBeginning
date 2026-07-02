package net.ty.createcraftedbeginning.recipe.trie;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie.Builder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightWithGasRecipeTrieFinder {
    private static final Cache<Object, AirtightWithGasRecipeTrie<?>> CACHED_TRIES = CacheBuilder.newBuilder().build();

    public static AirtightWithGasRecipeTrie<?> get(Object cacheKey, Level level, Predicate<RecipeHolder<? extends Recipe<?>>> conditions) throws ExecutionException {
        return CACHED_TRIES.get(cacheKey, () -> {
            Builder<Recipe<?>> builder = AirtightWithGasRecipeTrie.builder();
            List<RecipeHolder<? extends Recipe<?>>> list = RecipeFinder.get(cacheKey, level, conditions);
            for (RecipeHolder<? extends Recipe<?>> recipe : list) {
                builder.insert(recipe.value());
            }
            return builder.build();
        });
    }
}
