package net.ty.createcraftedbeginning.gametests.content.airtights.airtightreactorkettle;

import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleRecipeLookup;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope.TrackingIngredient;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;

import static net.ty.createcraftedbeginning.registry.gas.CCBGases.NATURAL_AIR;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReactorKettleRecipeIndexGameTests {
    private ReactorKettleRecipeIndexGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void indexedSelectionKeepsTemperatureAndPressurePriority(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            ReactorKettleRecipe compatible = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/compatible")).require(Items.IRON_INGOT).temperatureMatching(TemperatureMatching.COMPATIBLE).output(Items.GOLD_INGOT).build();
            ReactorKettleRecipe heated = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/heated")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.HEATED).output(Items.GOLD_INGOT).build();
            ReactorKettleRecipe pressurized = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/pressurized")).require(Items.IRON_INGOT).require(NATURAL_AIR.get(), 100, PressureRequirement.atLeast(GasPressure.pascals(2))).temperatureCondition(TemperatureCondition.HEATED).output(Items.DIAMOND).build();
            List<ReactorKettleRecipe> recipes = List.of(compatible, heated, pressurized);
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), recipes)) {
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
                assertSameSelection(helper, kettle, recipes);
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'indexedSelectionKeepsTemperatureAndPressurePriority'.")) == compatible, "Ambient temperature selected a heated recipe");
                temperature[0] = 2;
                assertSameSelection(helper, kettle, recipes);
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'indexedSelectionKeepsTemperatureAndPressurePriority'.")) == heated, "Exact temperature did not win over compatible temperature");
                GasStorageHandler gas = kettle.getInputGasTank().getCapability();
                long amount = GasPressure.amount(gas.getTankVolume(0), GasPressure.pascals(2)) + 200;
                helper.assertValueEqual(gas.fill(new GasStack(NATURAL_AIR.get(), amount), GasAction.EXECUTE), amount, "Initial gas fill");
                assertSameSelection(helper, kettle, recipes);
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'indexedSelectionKeepsTemperatureAndPressurePriority'.")) == pressurized, "Higher minimum pressure lost priority");
                kettle.setRecipeFilter(new ItemStack(Items.GOLD_INGOT));
                assertSameSelection(helper, kettle, recipes);
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'indexedSelectionKeepsTemperatureAndPressurePriority'.")) == heated, "Filter was ignored");
                SmartInventory output = kettle.getOutputInventory();
                for (int slot = 0; slot < output.getSlots(); slot++) {
                    output.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
                }
                assertSameSelection(helper, kettle, recipes);
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).isEmpty(), "Blocked output was accepted");
                helper.assertValueEqual(kettle.getInputInventory().getStackInSlot(0).getCount(), 4, "Input after queries");
                helper.assertValueEqual(gas.getGasInTank(0).getAmount(), amount, "Gas after queries");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void healthyMissDoesNotScanUnrelatedRecipes(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            TrackingIngredient tracked = new TrackingIngredient(Items.IRON_INGOT, false);
            Ingredient ingredient = tracked.toVanilla();
            List<ReactorKettleRecipe> recipes = new ArrayList<>();
            for (int index = 0; index < 1000; index++) {
                recipes.add(new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/unrelated_" + index)).require(ingredient).output(Items.GOLD_INGOT).build());
            }
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), recipes)) {
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.DIRT));
                tracked.resetChecks();
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).isEmpty(), "Unexpected match");
                helper.assertValueEqual(tracked.checks(), 0, "Unrelated predicates evaluated after an indexed miss");
                long linearMatches = recipes.stream().filter(recipe -> new ReactorKettleCraftPlanner(kettle, recipe).matches()).count();
                helper.assertValueEqual(linearMatches, 0L, "Linear matches");
                helper.assertTrue(tracked.checks() >= 1000, "Linear reference did not inspect all recipes");
                CCBAPI.LOGGER.info("Airtight reactor kettle index audit: unrelatedRecipes=1000, indexedPredicateChecks=0, linearPredicateChecks={}", tracked.checks());
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void failedIndexFallsBackUntilRecipesReload(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            TrackingIngredient broken = new TrackingIngredient(Items.IRON_INGOT, true);
            ReactorKettleRecipe original = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/broken_index")).require(broken.toVanilla()).output(Items.GOLD_INGOT).build();
            try (RecipeIndexTestScope scope = new RecipeIndexTestScope(helper.getLevel(), List.of(original))) {
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                broken.expectFailure(helper, () -> helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'failedIndexFallsBackUntilRecipesReload'.")) == original, "Failed index did not use linear matching"));
                int expansions = broken.expansions();
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'failedIndexFallsBackUntilRecipesReload'.")) == original, "Failure fallback stopped working");
                helper.assertValueEqual(broken.expansions(), expansions, "Failed index rebuilt without a reload");
                ReactorKettleRecipe replacement = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/replacement")).require(Items.COPPER_INGOT).output(Items.GOLD_INGOT).build();
                scope.reload(List.of(replacement));
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).isEmpty(), "Reload retained the removed recipe");
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT));
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'failedIndexFallsBackUntilRecipesReload'.")) == replacement, "Reload did not rebuild the index");
            }
        });
    }

    private static void assertSameSelection(GameTestHelper helper, AirtightReactorKettleBlockEntity kettle, List<ReactorKettleRecipe> recipes) {
        Comparator<ReactorKettleRecipe> priority = Comparator.comparingInt((ReactorKettleRecipe recipe) -> TemperatureMatching.getMatchPriority(recipe.getTemperatureMatching(), recipe.getTemperatureCondition(), kettle.getRecipeTemperature())).thenComparingLong(recipe -> recipe.getGasRequirements().stream().mapToLong(requirement -> requirement.pressure().minimumPressurePaOrVacuum()).max().orElse(0));
        Optional<ReactorKettleRecipe> linear = recipes.stream().filter(recipe -> new ReactorKettleCraftPlanner(kettle, recipe).matches()).max(priority);
        helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingRecipe(kettle).equals(linear), "Indexed and linear selections differ");
    }

    private static void withKettle(GameTestHelper helper, BiConsumer<AirtightReactorKettleBlockEntity, float[]> test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.getDefaultState());
        BlockEntity original = helper.getBlockEntity(pos);
        float[] temperature = {0};
        AirtightReactorKettleBlockEntity kettle = new AirtightReactorKettleBlockEntity(original.getType(), helper.absolutePos(pos), original.getBlockState()) {
            @Override
            public float getRecipeTemperature() {
                return temperature[0];
            }
        };
        helper.getLevel().setBlockEntity(kettle);
        helper.runAfterDelay(2, () -> {
            test.accept(kettle, temperature);
            helper.succeed();
        });
    }
}
