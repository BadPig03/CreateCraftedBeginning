package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.DataComponentGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.GasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SingleGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.DissipationRecipe;
import net.ty.createcraftedbeginning.recipe.EnergizationRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BreezeChamberRecipeIndex {
    private static final Map<RecipeManager, Index> INDICES = new WeakHashMap<>();

    private BreezeChamberRecipeIndex() {
    }

    public static void rebuild(RecipeManager recipeManager) {
        Index index = Index.create(recipeManager);
        synchronized (INDICES) {
            INDICES.put(recipeManager, index);
        }
    }

    public static boolean isIndexed(RecipeManager recipeManager) {
        synchronized (INDICES) {
            return INDICES.containsKey(recipeManager);
        }
    }

    static List<GasConversion> findEnergizationCandidates(RecipeManager recipeManager, GasStack inputStack) {
        return getOrBuild(recipeManager).findEnergizationCandidates(inputStack);
    }

    static List<GasConversion> findDissipationCandidates(RecipeManager recipeManager, GasStack inputStack) {
        return getOrBuild(recipeManager).findDissipationCandidates(inputStack);
    }

    private static Index getOrBuild(RecipeManager recipeManager) {
        synchronized (INDICES) {
            return INDICES.computeIfAbsent(recipeManager, Index::create);
        }
    }

    record GasConversion(SizedGasIngredient input, GasStack output) {
        GasConversion {
            output = output.copy();
        }

        @Override
        public GasStack output() {
            return output.copy();
        }

        boolean matches(GasStack inputStack) {
            return input.ingredient().test(inputStack);
        }

        boolean matchesIngredient(GasStack inputStack) {
            return matches(inputStack);
        }

        boolean hasRequiredInput(GasStack inputStack) {
            return input.test(inputStack);
        }

        int specificity(GasStack inputStack) {
            GasIngredient ingredient = input.ingredient();
            if (!ingredient.test(inputStack)) {
                return 0;
            }

            if (ingredient instanceof DataComponentGasIngredient) {
                return 3;
            }

            if (ingredient instanceof SingleGasIngredient) {
                return 2;
            }
            return 1;
        }
    }

    private record IndexedConversion(String recipeId, GasConversion conversion) {}

    private record Index(ConversionLookup energization, ConversionLookup dissipation) {
        private static Index create(RecipeManager recipeManager) {
            ConversionLookup energization = new ConversionLookup();
            for (RecipeHolder<?> holder : recipeManager.getAllRecipesFor(CCBRecipeTypes.ENERGIZATION.getType())) {
                if (!(holder.value() instanceof EnergizationRecipe recipe)) {
                    continue;
                }

                try {
                    addRecipe(energization, holder.id().toString(), recipe.getGasIngredient(), recipe.getGasResult());
                } catch (RuntimeException exception) {
                    CCBAPI.LOGGER.error("Failed to index breeze chamber energization recipe {}", holder.id(), exception);
                }
            }

            ConversionLookup dissipation = new ConversionLookup();
            for (RecipeHolder<?> holder : recipeManager.getAllRecipesFor(CCBRecipeTypes.DISSIPATION.getType())) {
                if (!(holder.value() instanceof DissipationRecipe recipe)) {
                    continue;
                }

                try {
                    addRecipe(dissipation, holder.id().toString(), recipe.getGasIngredient(), recipe.getGasResult());
                } catch (RuntimeException exception) {
                    CCBAPI.LOGGER.error("Failed to index breeze chamber dissipation recipe {}", holder.id(), exception);
                }
            }

            energization.freeze();
            dissipation.freeze();
            return new Index(energization, dissipation);
        }

        private static void addRecipe(ConversionLookup lookup, String recipeId, SizedGasIngredient input, GasStack output) {
            if (output.isEmpty()) {
                CCBAPI.LOGGER.warn("Ignoring breeze chamber recipe {} because its gas output is empty", recipeId);
                return;
            }

            lookup.add(recipeId, new GasConversion(input, output));
        }

        private List<GasConversion> findEnergizationCandidates(GasStack input) {
            return energization.findCandidates(input);
        }

        private List<GasConversion> findDissipationCandidates(GasStack input) {
            return dissipation.findCandidates(input);
        }
    }

    private static final class ConversionLookup {
        private Map<Gas, List<IndexedConversion>> byGas = new IdentityHashMap<>();
        private List<IndexedConversion> fallback = new ArrayList<>();

        private static void collectMatches(List<IndexedConversion> candidates, GasStack inputStack, Set<IndexedConversion> seen, List<IndexedConversion> matches) {
            for (IndexedConversion indexedConversion : candidates) {
                if (!seen.add(indexedConversion) || !indexedConversion.conversion().matchesIngredient(inputStack)) {
                    continue;
                }

                matches.add(indexedConversion);
            }
        }

        private void add(String recipeId, GasConversion conversion) {
            IndexedConversion indexedConversion = new IndexedConversion(recipeId, conversion);
            Set<Gas> indexedGases = Collections.newSetFromMap(new IdentityHashMap<>());
            for (GasStack candidateStack : conversion.input().getGases()) {
                if (candidateStack.isEmpty()) {
                    continue;
                }

                Gas candidateGas = candidateStack.getGasType();
                if (!indexedGases.add(candidateGas)) {
                    continue;
                }

                byGas.computeIfAbsent(candidateGas, ignored -> new ArrayList<>()).add(indexedConversion);
            }

            if (!indexedGases.isEmpty() && conversion.input().ingredient().isSimple()) {
                return;
            }

            fallback.add(indexedConversion);
        }

        private void freeze() {
            byGas.replaceAll((gas, conversions) -> List.copyOf(conversions));
            byGas = Collections.unmodifiableMap(byGas);
            fallback = List.copyOf(fallback);
        }

        private List<GasConversion> findCandidates(GasStack inputStack) {
            if (inputStack.isEmpty()) {
                return List.of();
            }

            List<IndexedConversion> matches = new ArrayList<>();
            Set<IndexedConversion> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            List<IndexedConversion> indexedCandidates = byGas.get(inputStack.getGasType());
            if (indexedCandidates != null) {
                collectMatches(indexedCandidates, inputStack, seen, matches);
            }
            collectMatches(fallback, inputStack, seen, matches);
            matches.sort(Comparator.comparingInt((IndexedConversion indexedConversion) -> indexedConversion.conversion().specificity(inputStack)).reversed().thenComparingLong(indexedConversion -> indexedConversion.conversion().input().amount()).thenComparing(IndexedConversion::recipeId));
            return matches.stream().map(IndexedConversion::conversion).toList();
        }
    }
}
