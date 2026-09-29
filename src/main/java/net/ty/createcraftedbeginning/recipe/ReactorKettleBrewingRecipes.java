package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.mixin.common.accessor.PotionMixingRecipesAccessor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ReactorKettleBrewingRecipes {
    private static final Map<Level, Catalog> CATALOGS = new WeakHashMap<>();

    private ReactorKettleBrewingRecipes() {
    }

    public static synchronized void invalidateCaches() {
        CATALOGS.clear();
    }

    public static List<RecipeHolder<ReactorKettleRecipe>> getRecipes(Level level) {
        return getCatalog(level).recipes();
    }

    public static List<ReactorKettleRecipe> getCandidates(Level level, IItemHandler items) {
        Catalog catalog = getCatalog(level);
        Set<ReactorKettleRecipe> candidates = new LinkedHashSet<>(catalog.unindexed());
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            candidates.addAll(catalog.byItem().getOrDefault(stack.getItem(), List.of()));
        }
        return List.copyOf(candidates);
    }

    private static synchronized Catalog getCatalog(Level level) {
        PotionBrewing brewing = level.potionBrewing();
        RegistryAccess registries = level.registryAccess();
        RecipeManager recipeManager = level.getRecipeManager();
        Catalog cached = CATALOGS.get(level);
        if (cached != null && cached.brewing() == brewing && cached.registries() == registries && cached.recipeManager() == recipeManager) {
            return cached;
        }

        List<RecipeHolder<ReactorKettleRecipe>> recipes = PotionMixingRecipesAccessor.ccb$createRecipes(level).stream().filter(ReactorKettleMixingRecipe::isSupported).map(ReactorKettleMixingRecipe::convertBrewing).toList();
        Map<Item, List<ReactorKettleRecipe>> byItem = new HashMap<>();
        List<ReactorKettleRecipe> unindexed = new ArrayList<>();
        for (RecipeHolder<ReactorKettleRecipe> holder : recipes) {
            ReactorKettleRecipe recipe = holder.value();
            if (recipe.getIngredients().isEmpty() || recipe.getIngredients().stream().anyMatch(ingredient -> !ingredient.isSimple())) {
                unindexed.add(recipe);
                continue;
            }

            Set<Item> ingredientItems = new LinkedHashSet<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) {
                    ingredientItems.add(stack.getItem());
                }
            }
            for (Item item : ingredientItems) {
                byItem.computeIfAbsent(item, ignored -> new ArrayList<>()).add(recipe);
            }
        }
        byItem.replaceAll((item, candidates) -> List.copyOf(candidates));
        Catalog catalog = new Catalog(brewing, registries, recipeManager, recipes, Map.copyOf(byItem), List.copyOf(unindexed));
        CATALOGS.put(level, catalog);
        return catalog;
    }

    private record Catalog(PotionBrewing brewing, RegistryAccess registries, RecipeManager recipeManager, List<RecipeHolder<ReactorKettleRecipe>> recipes, Map<Item, List<ReactorKettleRecipe>> byItem, List<ReactorKettleRecipe> unindexed) {}
}
