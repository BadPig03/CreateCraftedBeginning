package net.ty.createcraftedbeginning.recipe.trie;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.foundation.recipe.trie.IntArrayTrie;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.trie.AbstractIngredient.Universal;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractFluid;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractGas;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ReactorKettleRecipeTrie<R extends Recipe<?>> {
    private static final int MAX_CACHE_SIZE = Integer.getInteger("createcraftedbeginning.recipe_trie.max_cache_size", 512);

    private final IntArrayTrie<R> trie;
    private final Object2IntMap<AbstractVariant> variantToId;
    private final Int2ObjectMap<IntSet> variantToIngredients;
    private final int universalIngredientId;
    private final Cache<Set<AbstractVariant>, IntSet> ingredientCache = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).build();

    private ReactorKettleRecipeTrie(IntArrayTrie<R> trie, Object2IntMap<AbstractVariant> variantToId, Int2ObjectMap<IntSet> variantToIngredients, int universalIngredientId) {
        this.trie = trie;
        this.variantToId = variantToId;
        this.variantToIngredients = variantToIngredients;
        this.universalIngredientId = universalIngredientId;
    }

    public static @NotNull Set<AbstractVariant> getVariants(@Nullable IItemHandler itemStorage, @Nullable IFluidHandler fluidStorage, @Nullable IGasHandler gasStorage) {
        Set<AbstractVariant> variants = new HashSet<>();
        if (itemStorage != null) {
            for (int slot = 0; slot < itemStorage.getSlots(); slot++) {
                ItemStack item = itemStorage.getStackInSlot(slot);
                if (item.isEmpty()) {
                    continue;
                }

                variants.add(new AbstractItem(item.getItem()));
            }
        }

        if (fluidStorage != null) {
            for (int tank = 0; tank < fluidStorage.getTanks(); tank++) {
                FluidStack fluid = fluidStorage.getFluidInTank(tank);
                if (fluid.isEmpty()) {
                    continue;
                }

                variants.add(new AbstractFluid(fluid.getFluid()));
            }
        }

        if (gasStorage != null) {
            for (int tank = 0; tank < gasStorage.getTanks(); tank++) {
                GasStack gas = gasStorage.getGasInTank(tank);
                if (gas.isEmpty()) {
                    continue;
                }

                variants.add(new AbstractGas(gas.getGasType()));
            }
        }

        return variants;
    }

    @Contract(" -> new")
    public static <R extends Recipe<?>> @NotNull Builder<R> builder() {
        return new Builder<>();
    }

    private @NotNull IntSet getAvailableIngredients(@NotNull Set<AbstractVariant> pool) {
        pool.retainAll(variantToId.keySet());
        try {
            return ingredientCache.get(Set.copyOf(pool), () -> {
                IntSet ingredients = new IntOpenHashSet();
                ingredients.add(universalIngredientId);
                for (AbstractVariant variant : pool) {
                    int id = variantToId.getInt(variant);
                    if (id < 0) {
                        continue;
                    }

                    IntSet ingredientIds = variantToIngredients.get(id);
                    if (ingredientIds == null) {
                        continue;
                    }

                    ingredients.addAll(ingredientIds);
                }
                return ingredients;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull List<R> lookup(@NotNull Set<AbstractVariant> pool) {
        return trie.lookup(getAvailableIngredients(pool));
    }

    public static class Builder<R extends Recipe<?>> {
        private final IntArrayTrie<R> trie = new IntArrayTrie<>();
        private final Map<Object, AbstractVariant> variantCache = new HashMap<>();
        private final Object2IntOpenHashMap<AbstractVariant> variantToId = new Object2IntOpenHashMap<>();
        private final Object2IntMap<AbstractIngredient> ingredientToId = new Object2IntOpenHashMap<>();
        private final int universalIngredientId;
        private final Int2ObjectOpenHashMap<IntSet> variantToIngredients = new Int2ObjectOpenHashMap<>();

        private int nextVariantId;
        private int nextIngredientId;

        private Builder() {
            variantToId.defaultReturnValue(-1);
            ingredientToId.defaultReturnValue(-1);
            universalIngredientId = getOrAssignId(Universal.INSTANCE);
        }

        private int getOrAssignId(AbstractIngredient ingredient) {
            return ingredientToId.computeIfAbsent(ingredient, $ -> {
                int id = nextIngredientId++;
                for (AbstractVariant variant : ingredient.variants) {
                    variantToIngredients.computeIfAbsent(getOrAssignId(variant), $1 -> new IntOpenHashSet()).add(id);
                }
                return id;
            });
        }

        private int getOrAssignId(AbstractVariant variant) {
            return variantToId.computeIfAbsent(variant, $ -> nextVariantId++);
        }

        private AbstractVariant getOrAssignVariant(Item item) {
            AbstractVariant variant = variantCache.computeIfAbsent(item, $ -> new AbstractItem(item));
            getOrAssignId(variant);
            return variant;
        }

        private AbstractVariant getOrAssignVariant(Fluid fluid) {
            AbstractVariant variant = variantCache.computeIfAbsent(fluid, $ -> new AbstractFluid(fluid));
            getOrAssignId(variant);
            return variant;
        }

        private AbstractVariant getOrAssignVariant(Gas gasType) {
            AbstractVariant variant = variantCache.computeIfAbsent(gasType, $ -> new AbstractGas(gasType));
            getOrAssignId(variant);
            return variant;
        }

        private void insert(@NotNull AbstractRecipe<? extends R> recipe) {
            int[] key = new int[recipe.ingredients.size()];
            int i = 0;
            for (AbstractIngredient ingredient : recipe.ingredients) {
                key[i++] = getOrAssignId(ingredient);
            }
            Arrays.sort(key);
            trie.insert(key, recipe.recipe);
        }

        public <R1 extends R> void insert(R1 recipe) {
            insert(createRecipe(recipe));
        }

        @Contract("_ -> new")
        private <R1 extends R> @NotNull AbstractRecipe<R1> createRecipe(@NotNull R1 recipe) {
            Set<AbstractIngredient> ingredients = new HashSet<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    ingredients.add(Universal.INSTANCE);
                    continue;
                }

                if (!ingredient.isSimple()) {
                    ingredients.add(Universal.INSTANCE);
                    continue;
                }

                Set<AbstractVariant> variants = new HashSet<>();
                for (ItemStack stack : ingredient.getItems()) {
                    variants.add(getOrAssignVariant(stack.getItem()));
                }
                ingredients.add(new AbstractIngredient(variants));
            }

            if (recipe instanceof ReactorKettleRecipe kettleRecipe) {
                for (SizedFluidIngredient ingredient : kettleRecipe.getFluidIngredients()) {
                    if (ingredient.amount() == 0) {
                        ingredients.add(Universal.INSTANCE);
                        continue;
                    }

                    Set<AbstractVariant> variants = new HashSet<>();
                    for (FluidStack stack : ingredient.getFluids()) {
                        variants.add(getOrAssignVariant(stack.getFluid()));
                    }
                    ingredients.add(new AbstractIngredient(variants));
                }

                for (SizedGasIngredient ingredient : kettleRecipe.getGasIngredients()) {
                    if (ingredient.amount() == 0) {
                        ingredients.add(Universal.INSTANCE);
                        continue;
                    }

                    Set<AbstractVariant> variants = new HashSet<>();
                    for (GasStack stack : ingredient.getGases()) {
                        variants.add(getOrAssignVariant(stack.getGasType()));
                    }
                    ingredients.add(new AbstractIngredient(variants));
                }
            }

            return new AbstractRecipe<>(recipe, ingredients);
        }

        public ReactorKettleRecipeTrie<R> build() {
            variantToId.trim();
            variantToIngredients.trim();
            CreateCraftedBeginning.LOGGER.info("ReactorKettleRecipeTrie of depth {} with {} nodes built with {} variants, {} ingredients, and {} recipes", trie.getMaxDepth(), trie.getNodeCount(), variantToId.size(), ingredientToId.size(), trie.getValueCount());
            return new ReactorKettleRecipeTrie<>(trie, variantToId, variantToIngredients, universalIngredientId);
        }
    }
}
