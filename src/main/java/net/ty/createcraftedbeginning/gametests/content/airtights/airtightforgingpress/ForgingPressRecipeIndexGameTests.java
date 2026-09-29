package net.ty.createcraftedbeginning.gametests.content.airtights.airtightforgingpress;

import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressRecipeLookup;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope.TrackingIngredient;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Serializer;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForgingPressRecipeIndexGameTests {
    private ForgingPressRecipeIndexGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void indexedSelectionMatchesLinearAcrossResourceStates(GameTestHelper helper) {
        withPress(helper, press -> {
            Serializer<ForgingPressRecipe> serializer = CCBRecipeTypes.FORGING_PRESS.getSerializer();
            ForgingPressRecipe broad = recipe("broad", Ingredient.of(Items.IRON_INGOT, Items.COPPER_INGOT));
            ForgingPressRecipe narrow = recipe("narrow", Ingredient.of(Items.IRON_INGOT));
            ForgingPressRecipe pressure = new Builder<>(serializer.factory(), CCBAPI.asResource("test/pressure")).require(Items.IRON_INGOT).require(CCBGases.NATURAL_AIR.get(), 100, PressureRequirement.atLeast(GasPressure.pascals(2))).output(Items.DIAMOND).build();
            List<ForgingPressRecipe> recipes = List.of(broad, narrow, pressure);
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), recipes)) {
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
                assertSameSelection(helper, press, recipes);
                press.setRecipeFilter(new ItemStack(Items.DIAMOND));
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).isEmpty(), "Pressure requirement was bypassed");
                GasStorageHandler gas = press.getGasCapability();
                long amount = GasPressure.amount(gas.getTankVolume(0), GasPressure.pascals(2)) + 200;
                helper.assertValueEqual(gas.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE), amount, "Initial gas fill");
                assertSameSelection(helper, press, recipes);
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'indexedSelectionMatchesLinearAcrossResourceStates'.")) == pressure, "Filter or pressure selected the wrong recipe");
                SmartInventory output = press.getOutputInventory();
                for (int slot = 0; slot < output.getSlots(); slot++) {
                    output.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
                }
                assertSameSelection(helper, press, recipes);
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).isEmpty(), "Blocked output was accepted");
                helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 4, "Input after queries");
                helper.assertValueEqual(gas.getGasInTank(0).getAmount(), amount, "Gas after queries");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void healthyMissDoesNotScanUnrelatedRecipes(GameTestHelper helper) {
        withPress(helper, press -> {
            TrackingIngredient tracked = new TrackingIngredient(Items.IRON_INGOT, false);
            Ingredient ingredient = tracked.toVanilla();
            List<ForgingPressRecipe> recipes = new ArrayList<>();
            for (int index = 0; index < 1000; index++) {
                recipes.add(recipe("unrelated_" + index, ingredient));
            }
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), recipes)) {
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.DIRT));
                tracked.resetChecks();
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).isEmpty(), "Unexpected match");
                helper.assertValueEqual(tracked.checks(), 0, "Unrelated predicates evaluated after an indexed miss");
                long linearMatches = recipes.stream().filter(recipe -> new ForgingPressCraftPlanner(press, recipe).matches()).count();
                helper.assertValueEqual(linearMatches, 0L, "Linear matches");
                helper.assertTrue(tracked.checks() >= 1000, "Linear reference did not inspect all recipes");
                CCBAPI.LOGGER.info("Airtight forging press index audit: unrelatedRecipes=1000, indexedPredicateChecks=0, linearPredicateChecks={}", tracked.checks());
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void failedIndexFallsBackUntilRecipesReload(GameTestHelper helper) {
        withPress(helper, press -> {
            TrackingIngredient broken = new TrackingIngredient(Items.IRON_INGOT, true);
            ForgingPressRecipe original = recipe("broken_index", broken.toVanilla());
            try (RecipeIndexTestScope scope = new RecipeIndexTestScope(helper.getLevel(), List.of(original))) {
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                broken.expectFailure(helper, () -> helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'failedIndexFallsBackUntilRecipesReload'.")) == original, "Failed index did not use linear matching"));
                int expansions = broken.expansions();
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'failedIndexFallsBackUntilRecipesReload'.")) == original, "Failure fallback stopped working");
                helper.assertValueEqual(broken.expansions(), expansions, "Failed index rebuilt without a reload");
                ForgingPressRecipe replacement = recipe("replacement", Ingredient.of(Items.COPPER_INGOT));
                scope.reload(List.of(replacement));
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).isEmpty(), "Reload retained the removed recipe");
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT));
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected a matching recipe in test 'failedIndexFallsBackUntilRecipesReload'.")) == replacement, "Reload did not rebuild the index");
            }
        });
    }

    private static ForgingPressRecipe recipe(String name, Ingredient ingredient) {
        Serializer<ForgingPressRecipe> serializer = CCBRecipeTypes.FORGING_PRESS.getSerializer();
        return new Builder<>(serializer.factory(), CCBAPI.asResource("test/" + name)).require(ingredient).output(Items.GOLD_INGOT).build();
    }

    private static void assertSameSelection(GameTestHelper helper, AirtightForgingPressBlockEntity press, List<ForgingPressRecipe> recipes) {
        Optional<ForgingPressRecipe> linear = recipes.stream().filter(recipe -> new ForgingPressCraftPlanner(press, recipe).matches()).findFirst();
        helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingRecipe(press).equals(linear), "Indexed and linear selections differ");
    }

    private static void withPress(GameTestHelper helper, Consumer<AirtightForgingPressBlockEntity> test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.getDefaultState());
        helper.runAfterDelay(2, () -> {
            test.accept(helper.getBlockEntity(pos));
            helper.succeed();
        });
    }
}
