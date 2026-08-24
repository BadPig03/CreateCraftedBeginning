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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.recipe.trie.AbstractIngredient.Universal;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractFluid;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractGas;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightWithGasRecipeTrie<R extends Recipe<?>> {
    private static final int MAX_CACHE_SIZE = Integer.getInteger("createcraftedbeginning.recipe_trie.max_cache_size", 512);

    private final IntArrayTrie<R> trie;
    private final Object2IntMap<AbstractVariant> variantToId;
    private final Int2ObjectMap<IntSet> variantToIngredients;
    private final int universalIngredientId;
    private final Cache<Set<AbstractVariant>, IntSet> ingredientCache = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).build();

    private AirtightWithGasRecipeTrie(IntArrayTrie<R> trie, Object2IntMap<AbstractVariant> variantToId, Int2ObjectMap<IntSet> variantToIngredients, int universalIngredientId) {
        this.trie = trie;
        this.variantToId = variantToId;
        this.variantToIngredients = variantToIngredients;
        this.universalIngredientId = universalIngredientId;
    }

    public static Set<AbstractVariant> getVariants(@Nullable IItemHandler itemStorage, @Nullable IFluidHandler fluidStorage, @Nullable IGasHandler gasStorage) {
        Set<AbstractVariant> variants = new HashSet<>();
        addItemVariants(variants, itemStorage);
        addFluidVariants(variants, fluidStorage);
        addGasVariants(variants, gasStorage);
        return variants;
    }

    @Contract(" -> new")
    static <R extends Recipe<?>> @NotNull Builder<R> builder() {
        return new Builder<>();
    }

    private static void addItemVariants(Set<AbstractVariant> variants, @Nullable IItemHandler itemStorage) {
        if (itemStorage == null) {
            return;
        }

        for (int slot = 0; slot < itemStorage.getSlots(); slot++) {
            ItemStack itemStack = itemStorage.getStackInSlot(slot);
            if (!itemStack.isEmpty()) {
                variants.add(new AbstractItem(itemStack.getItem()));
            }
        }
    }

    private static void addFluidVariants(Set<AbstractVariant> variants, @Nullable IFluidHandler fluidStorage) {
        if (fluidStorage == null) {
            return;
        }

        for (int tankIndex = 0; tankIndex < fluidStorage.getTanks(); tankIndex++) {
            FluidStack fluidStack = fluidStorage.getFluidInTank(tankIndex);
            if (!fluidStack.isEmpty()) {
                variants.add(new AbstractFluid(fluidStack.getFluid()));
            }
        }
    }

    private static void addGasVariants(Set<AbstractVariant> variants, @Nullable IGasHandler gasStorage) {
        if (gasStorage == null) {
            return;
        }

        for (int tankIndex = 0; tankIndex < gasStorage.getTanks(); tankIndex++) {
            GasStack gasStack = gasStorage.getGasInTank(tankIndex);
            if (!gasStack.isEmpty()) {
                variants.add(new AbstractGas(gasStack.getGasType()));
            }
        }
    }

    private IntSet getAvailableIngredients(Set<AbstractVariant> variants) {
        variants.retainAll(variantToId.keySet());
        try {
            return ingredientCache.get(Set.copyOf(variants), () -> {
                IntSet availableIngredientIds = new IntOpenHashSet();
                availableIngredientIds.add(universalIngredientId);
                for (AbstractVariant variant : variants) {
                    int variantId = variantToId.getInt(variant);
                    if (variantId < 0) {
                        continue;
                    }

                    IntSet ingredientIds = variantToIngredients.get(variantId);
                    if (ingredientIds == null) {
                        continue;
                    }

                    availableIngredientIds.addAll(ingredientIds);
                }
                return availableIngredientIds;
            });
        } catch (ExecutionException exception) {
            throw new RuntimeException(exception);
        }
    }

    public List<R> lookup(Set<AbstractVariant> availableVariants) {
        return trie.lookup(getAvailableIngredients(availableVariants));
    }

    static class Builder<R extends Recipe<?>> {
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
            return ingredientToId.computeIfAbsent(ingredient, ignored -> {
                int ingredientId = nextIngredientId++;
                for (AbstractVariant variant : ingredient.variants) {
                    variantToIngredients.computeIfAbsent(getOrAssignId(variant), ignoredId -> new IntOpenHashSet()).add(ingredientId);
                }
                return ingredientId;
            });
        }

        private int getOrAssignId(AbstractVariant variant) {
            return variantToId.computeIfAbsent(variant, ignored -> nextVariantId++);
        }

        private AbstractVariant getOrAssignVariant(Item item) {
            AbstractVariant variant = variantCache.computeIfAbsent(item, ignored -> new AbstractItem(item));
            getOrAssignId(variant);
            return variant;
        }

        private AbstractVariant getOrAssignVariant(Fluid fluid) {
            AbstractVariant variant = variantCache.computeIfAbsent(fluid, ignored -> new AbstractFluid(fluid));
            getOrAssignId(variant);
            return variant;
        }

        private AbstractVariant getOrAssignVariant(Gas gasType) {
            AbstractVariant variant = variantCache.computeIfAbsent(gasType, ignored -> new AbstractGas(gasType));
            getOrAssignId(variant);
            return variant;
        }

        private void insert(AbstractRecipe<? extends R> recipe) {
            int[] ingredientIds = new int[recipe.ingredients.size()];
            int ingredientIndex = 0;
            for (AbstractIngredient ingredient : recipe.ingredients) {
                ingredientIds[ingredientIndex++] = getOrAssignId(ingredient);
            }
            Arrays.sort(ingredientIds);
            trie.insert(ingredientIds, recipe.recipe);
        }

        <R1 extends R> void insert(R1 recipe) {
            insert(createRecipe(recipe));
        }

        @Contract("_ -> new")
        private <R1 extends R> @NotNull AbstractRecipe<R1> createRecipe(R1 recipe) {
            Set<AbstractIngredient> ingredients = new HashSet<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredients.add(createIngredient(ingredient));
            }

            if (!(recipe instanceof IAirtightWithGasRecipe withGasRecipe)) {
                return new AbstractRecipe<>(recipe, ingredients);
            }

            for (SizedFluidIngredient ingredient : withGasRecipe.getFluidIngredients()) {
                ingredients.add(createIngredient(ingredient));
            }
            for (SizedGasIngredient ingredient : withGasRecipe.getGasIngredients()) {
                ingredients.add(createIngredient(ingredient));
            }
            return new AbstractRecipe<>(recipe, ingredients);
        }

        private AbstractIngredient createIngredient(Ingredient ingredient) {
            if (ingredient.isEmpty() || !ingredient.isSimple()) {
                return Universal.INSTANCE;
            }

            Set<AbstractVariant> variants = new HashSet<>();
            for (ItemStack itemStack : ingredient.getItems()) {
                variants.add(getOrAssignVariant(itemStack.getItem()));
            }
            return new AbstractIngredient(variants);
        }

        private AbstractIngredient createIngredient(SizedFluidIngredient ingredient) {
            if (ingredient.amount() == 0) {
                return Universal.INSTANCE;
            }

            Set<AbstractVariant> variants = new HashSet<>();
            for (FluidStack fluidStack : ingredient.getFluids()) {
                variants.add(getOrAssignVariant(fluidStack.getFluid()));
            }
            return new AbstractIngredient(variants);
        }

        private AbstractIngredient createIngredient(SizedGasIngredient ingredient) {
            if (ingredient.amount() == 0) {
                return Universal.INSTANCE;
            }

            Set<AbstractVariant> variants = new HashSet<>();
            for (GasStack gasStack : ingredient.getGases()) {
                variants.add(getOrAssignVariant(gasStack.getGasType()));
            }
            return new AbstractIngredient(variants);
        }

        AirtightWithGasRecipeTrie<R> build() {
            variantToId.trim();
            variantToIngredients.trim();
            CCBAPI.LOGGER.info("AirtightWithGasRecipeTrie of depth {} with {} nodes built with {} variants, {} ingredients, and {} recipes", trie.getMaxDepth(), trie.getNodeCount(), variantToId.size(), ingredientToId.size(), trie.getValueCount());
            return new AirtightWithGasRecipeTrie<>(trie, variantToId, variantToIngredients, universalIngredientId);
        }
    }
}
