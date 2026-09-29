package net.ty.createcraftedbeginning.gametests.recipe;

import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressRecipeLookup;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerRecipeLookup;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleRecipeLookup;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipeLookup;
import net.ty.createcraftedbeginning.recipe.trie.AirtightRecipeTrieFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class RecipeIndexTestScope implements AutoCloseable {
    private final ServerLevel level;
    private final RecipeManager manager;
    private final List<RecipeHolder<?>> original;

    public RecipeIndexTestScope(ServerLevel level, List<? extends Recipe<?>> recipes) {
        this.level = level;
        manager = level.getRecipeManager();
        original = List.copyOf(manager.getRecipes());
        reload(recipes);
    }

    @Override
    public void close() {
        manager.replaceRecipes(original);
        clearCaches();
    }

    public void reload(List<? extends Recipe<?>> recipes) {
        List<RecipeHolder<?>> replacements = new ArrayList<>();
        for (int index = 0; index < recipes.size(); index++) {
            replacements.add(new RecipeHolder<>(CCBAPI.asResource("test/index_scope_" + index), recipes.get(index)));
        }
        manager.replaceRecipes(replacements);
        clearCaches();
    }

    private void clearCaches() {
        RecipeFinder.LISTENER.onResourceManagerReload(level.getServer().getResourceManager());
        AirtightRecipeTrieFinder.invalidateCaches();
        AirtightReactorKettleRecipeLookup.invalidateRecipeCaches();
        AirtightForgingPressRecipeLookup.invalidateRecipeCaches();
        AirtightFractionationTowerRecipeLookup.invalidateRecipeCaches();
        GasInjectionRecipeLookup.invalidateRecipeCaches();
    }

    public static final class TrackingIngredient implements ICustomIngredient {
        private final Item item;
        private final boolean failDisplay;
        private final IllegalStateException failure = new IllegalStateException("Intentional ingredient expansion failure.");
        private int checks;
        private int expansions;
        private int failures;

        public TrackingIngredient(Item item, boolean failDisplay) {
            this.item = item;
            this.failDisplay = failDisplay;
        }

        @Override
        public boolean test(ItemStack stack) {
            checks++;
            return stack.is(item);
        }

        @Override
        public Stream<ItemStack> getItems() {
            expansions++;
            if (failDisplay) {
                failures++;
                throw failure;
            }

            return Stream.of(new ItemStack(item));
        }

        @Override
        public boolean isSimple() {
            return true;
        }

        @Override
        public IngredientType<?> getType() {
            throw new UnsupportedOperationException("Test predicate is not serialized.");
        }

        public int checks() {
            return checks;
        }

        public int expansions() {
            return expansions;
        }

        public void expectFailure(GameTestHelper helper, Runnable query) {
            int previous = failures;
            int[] reports = {0};
            String loggerName = CCBAPI.LOGGER.getName();
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            LoggerConfig logger = context.getConfiguration().getLoggerConfig(loggerName);
            AbstractFilter filter = new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    if (!loggerName.equals(event.getLoggerName())) {
                        return Result.NEUTRAL;
                    }

                    for (Throwable cause = event.getThrown(); cause != null; cause = cause.getCause()) {
                        if (cause != failure) {
                            continue;
                        }

                        reports[0]++;
                        return Result.DENY;
                    }

                    return Result.NEUTRAL;
                }
            };
            logger.addFilter(filter);
            try {
                query.run();
            }
            finally {
                logger.removeFilter(filter);
            }
            helper.assertValueEqual(failures - previous, 1, "Ingredient expansion exception count");
            helper.assertValueEqual(reports[0], 1, "Caught expansion failure report count");
        }

        public void resetChecks() {
            checks = 0;
        }
    }
}
