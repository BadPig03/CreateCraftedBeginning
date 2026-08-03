package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.recipe.DissipationRecipe;
import net.ty.createcraftedbeginning.recipe.EnergizationRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public static Optional<GasConversion> findEnergization(RecipeManager recipeManager, GasStack input) {
        return getOrBuild(recipeManager).findEnergization(input);
    }

    public static Optional<GasConversion> findDissipation(RecipeManager recipeManager, GasStack input) {
        return getOrBuild(recipeManager).findDissipation(input);
    }

    private static Index getOrBuild(RecipeManager recipeManager) {
        synchronized (INDICES) {
            return INDICES.computeIfAbsent(recipeManager, Index::create);
        }
    }

    public record GasConversion(SizedGasIngredient input, GasStack output) {
        public GasConversion {
            output = output.copy();
        }

        @Override
        public GasStack output() {
            return output.copy();
        }

        public boolean matches(GasStack inputStack) {
            return input.ingredient().test(inputStack);
        }

        public boolean matches(Gas inputGas) {
            return input.test(inputGas);
        }
    }

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
                    CreateCraftedBeginning.LOGGER.error("Failed to index breeze chamber energization recipe {}", holder.id(), exception);
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
                    CreateCraftedBeginning.LOGGER.error("Failed to index breeze chamber dissipation recipe {}", holder.id(), exception);
                }
            }

            energization.freeze();
            dissipation.freeze();
            return new Index(energization, dissipation);
        }

        private static void addRecipe(ConversionLookup lookup, String recipeId, SizedGasIngredient input, GasStack output) {
            if (output.isEmpty()) {
                CreateCraftedBeginning.LOGGER.warn("Ignoring breeze chamber recipe {} because its gas output is empty", recipeId);
                return;
            }

            lookup.add(new GasConversion(input, output));
        }

        private Optional<GasConversion> findEnergization(GasStack input) {
            return energization.find(input);
        }

        private Optional<GasConversion> findDissipation(GasStack input) {
            return dissipation.find(input);
        }

        private Optional<GasConversion> findEnergization(Gas input) {
            return energization.find(input);
        }

        private Optional<GasConversion> findDissipation(Gas input) {
            return dissipation.find(input);
        }
    }

    private static final class ConversionLookup {
        private Map<Gas, List<GasConversion>> byGas = new IdentityHashMap<>();
        private List<GasConversion> fallback = new ArrayList<>();

        private void add(GasConversion conversion) {
            Set<Gas> indexedGases = Collections.newSetFromMap(new IdentityHashMap<>());
            for (GasStack candidate : conversion.input().getGases()) {
                if (candidate.isEmpty()) {
                    continue;
                }

                Gas gas = candidate.getGasType();
                if (!indexedGases.add(gas)) {
                    continue;
                }

                byGas.computeIfAbsent(gas, ignored -> new ArrayList<>()).add(conversion);
            }

            if (!indexedGases.isEmpty() && conversion.input().ingredient().isSimple()) {
                return;
            }

            fallback.add(conversion);
        }

        private void freeze() {
            byGas.replaceAll((gas, conversions) -> List.copyOf(conversions));
            byGas = Collections.unmodifiableMap(byGas);
            fallback = List.copyOf(fallback);
        }

        private Optional<GasConversion> find(GasStack input) {
            if (input.isEmpty()) {
                return Optional.empty();
            }

            List<GasConversion> candidates = byGas.get(input.getGasType());
            if (candidates != null) {
                for (GasConversion conversion : candidates) {
                    if (conversion.matches(input)) {
                        return Optional.of(conversion);
                    }
                }
            }

            for (GasConversion conversion : fallback) {
                if (conversion.matches(input)) {
                    return Optional.of(conversion);
                }
            }
            return Optional.empty();
        }

        private Optional<GasConversion> find(Gas input) {
            if (input.isEmpty()) {
                return Optional.empty();
            }

            List<GasConversion> candidates = byGas.get(input);
            if (candidates != null) {
                for (GasConversion conversion : candidates) {
                    if (conversion.matches(input)) {
                        return Optional.of(conversion);
                    }
                }
            }

            for (GasConversion conversion : fallback) {
                if (conversion.matches(input)) {
                    return Optional.of(conversion);
                }
            }
            return Optional.empty();
        }
    }
}
