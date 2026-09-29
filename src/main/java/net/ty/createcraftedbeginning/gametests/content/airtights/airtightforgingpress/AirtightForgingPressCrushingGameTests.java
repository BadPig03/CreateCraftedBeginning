package net.ty.createcraftedbeginning.gametests.content.airtights.airtightforgingpress;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import com.simibubi.create.foundation.item.SmartInventory;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressController;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressCrushing;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressRecipeLookup;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPreparation.Plan;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Serializer;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightForgingPressCrushingGameTests {
    private AirtightForgingPressCrushingGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void unmatchedItemsAndMillingOnlyInputsAreNotDestroyed(GameTestHelper helper) {
        withPress(helper, press -> {
            CrushingRecipe crushing = new Builder<>(CrushingRecipe::new, CCBAPI.asResource("test/crushing_iron")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            MillingRecipe milling = new Builder<>(MillingRecipe::new, CCBAPI.asResource("test/milling_only")).require(Items.DIRT).output(Items.SAND).build();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(crushing, milling))) {
                ItemStack input = new ItemStack(Items.DIRT, 12);
                input.set(DataComponents.CUSTOM_NAME, Component.literal("Keep unmatched input"));
                press.getInputInventory().setStackInSlot(0, input.copy());
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).isEmpty(), "Milling-only input matched automatic crushing");
                AirtightForgingPressController controller = new AirtightForgingPressController(press);
                controller.updateForgingPress();
                for (int tick = 0; tick < 60; tick++) {
                    controller.tick();
                }
                helper.assertTrue(!controller.isOperating(), "Unmatched input started crushing");
                helper.assertTrue(ItemStack.matches(press.getInputInventory().getStackInSlot(0), input), "Unmatched input was consumed or changed");
                helper.assertTrue(press.getOutputInventory().isEmpty(), "Unmatched input produced output");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void crushingPreservesTheCoreAndRollsEachInputOnlyWhenPrepared(GameTestHelper helper) {
        withPress(helper, press -> {
            CrushingRecipe recipe = new Builder<>(CrushingRecipe::new, CCBAPI.asResource("test/crushing_rolls")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT, 2).output(0.5F, Items.DIAMOND).build();
            int[] rolls = {0};
            recipe.getRollableResults().set(1, new ProcessingOutput(new ItemStack(Items.DIAMOND), 0.5F) {
                @Override
                public ItemStack rollOutput(RandomSource random) {
                    rolls[0]++;
                    if (rolls[0] % 2 == 0) {
                        return ItemStack.EMPTY;
                    }

                    return new ItemStack(Items.DIAMOND);
                }
            });
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(recipe))) {
                ItemStack input = new ItemStack(Items.IRON_INGOT, 4);
                input.set(DataComponents.CUSTOM_NAME, Component.literal("Input-only component"));
                press.getInputInventory().setStackInSlot(0, input);
                ItemStack core = press.getPressHeadInventory().getStackInSlot(0).copy();
                RecipeHolder<CrushingRecipe> match = AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected a matching crushing recipe."));
                helper.assertTrue(AirtightForgingPressCrushing.canApply(press, match), "Crushing preview rejected valid input");
                helper.assertValueEqual(rolls[0], 0, "rolls during crushing previews");
                Plan plan = AirtightForgingPressCrushing.prepare(press, match).orElseThrow(() -> new NoSuchElementException("Expected a crushing batch plan."));
                helper.assertValueEqual(rolls[0], 4, "one probability roll per input");
                helper.assertTrue(press.commitCraft(plan.consumption(), plan.output()), "Crushing batch failed to commit");
                helper.assertValueEqual(rolls[0], 4, "commit did not reroll outputs");
                helper.assertTrue(ItemStack.matches(core, press.getPressHeadInventory().getStackInSlot(0)), "Crushing consumed or changed its core");
                helper.assertTrue(press.getInputInventory().isEmpty(), "Crushing did not consume its inputs");
                helper.assertValueEqual(press.getOutputInventory().getStackInSlot(0).getCount(), 8, "guaranteed output count");
                helper.assertValueEqual(press.getOutputInventory().getStackInSlot(1).getCount(), 2, "probabilistic output count");
                helper.assertTrue(!press.getOutputInventory().getStackInSlot(0).has(DataComponents.CUSTOM_NAME), "Crushing copied input components to its result");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void crushingLimitsBatchesAndRejectsChangedCoreOrOutput(GameTestHelper helper) {
        withPress(helper, press -> {
            CrushingRecipe recipe = new Builder<>(CrushingRecipe::new, CCBAPI.asResource("test/crushing_capacity")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(recipe))) {
                SmartInventory output = press.getOutputInventory();
                for (int slot = 0; slot < output.getSlots(); slot++) {
                    output.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
                }
                output.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 61));
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
                RecipeHolder<CrushingRecipe> match = AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected a partially fitting crushing recipe."));
                Plan plan = AirtightForgingPressCrushing.prepare(press, match).orElseThrow(() -> new NoSuchElementException("Expected a partial crushing batch."));
                helper.assertValueEqual(plan.consumption().inputAmount(), 3, "output-limited crushing batch");
                press.getPressHeadInventory().setStackInSlot(0, ItemStack.EMPTY);
                helper.assertTrue(!press.commitCraft(plan.consumption(), plan.output()), "Changed core allowed a stale crushing commit");
                press.getPressHeadInventory().setStackInSlot(0, new ItemStack(Items.HEAVY_CORE));
                output.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 62));
                helper.assertTrue(!press.commitCraft(plan.consumption(), plan.output()), "Changed output allowed a stale crushing commit");
                helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 8, "input after rejected commits");
                output.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 61));
                helper.assertTrue(press.commitCraft(plan.consumption(), plan.output()), "Valid crushing plan failed to commit");
                helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 5, "remaining batch input");
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).isEmpty(), "Full output accepted another crushing batch");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void crushingRequiresTheCoreEmptyAdditionAndMatchingPrimaryFilter(GameTestHelper helper) {
        withPress(helper, press -> {
            CrushingRecipe recipe = new Builder<>(CrushingRecipe::new, CCBAPI.asResource("test/crushing_requirements")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).output(0.5F, Items.DIAMOND).build();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(recipe))) {
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                press.getPressHeadInventory().setStackInSlot(0, ItemStack.EMPTY);
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).isEmpty(), "Crushing accepted an empty press head");
                press.getPressHeadInventory().setStackInSlot(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).isEmpty(), "Crushing accepted a different press head");
                press.getPressHeadInventory().setStackInSlot(0, new ItemStack(Items.HEAVY_CORE));
                press.getAdditionInventory().setStackInSlot(0, new ItemStack(Items.DIRT));
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).isEmpty(), "Crushing accepted an addition item");
                press.getAdditionInventory().setStackInSlot(0, ItemStack.EMPTY);
                press.setRecipeFilter(new ItemStack(Items.DIAMOND));
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).isEmpty(), "Secondary output passed the primary filter");
                press.setRecipeFilter(new ItemStack(Items.GOLD_INGOT));
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).isPresent(), "Valid core and primary filter did not match");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void crushingSwitchStopsAnUncommittedCycleAndCanBeReenabled(GameTestHelper helper) {
        withPress(helper, press -> {
            CrushingRecipe recipe = new Builder<>(CrushingRecipe::new, CCBAPI.asResource("test/crushing_switch")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            ConfigBool enabled = CCBConfig.server().machines.airtightForgingPress.enableAutomaticCrushingRecipes;
            boolean previous = enabled.get();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(recipe))) {
                enabled.set(true);
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
                AirtightForgingPressController controller = new AirtightForgingPressController(press);
                controller.updateForgingPress();
                helper.assertTrue(controller.isOperating(), "Crushing cycle did not start");
                enabled.set(false);
                controller.tick();
                controller.lazyTick();
                helper.assertTrue(!controller.isOperating(), "Disabled crushing cycle did not stop");
                helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).isEmpty(), "Disabled crushing still matched recipes");
                helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 4, "input after disabling crushing");
                enabled.set(true);
                controller.lazyTick();
                controller.updateForgingPress();
                for (int tick = 0; tick < 31; tick++) {
                    controller.tick();
                }
                helper.assertValueEqual(press.getOutputInventory().getStackInSlot(0).getCount(), 4, "one batch after re-enabling crushing");
                helper.assertTrue(!controller.isOperating(), "Completed crushing cycle remained active");
            }
            finally {
                enabled.set(previous);
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void nativeForgingWinsAndRemainsAvailableWhenCrushingIsDisabled(GameTestHelper helper) {
        withPress(helper, press -> {
            Serializer<ForgingPressRecipe> serializer = CCBRecipeTypes.FORGING_PRESS.getSerializer();
            ForgingPressRecipe nativeRecipe = new StandardGasProcessingRecipe.Builder<>(serializer.factory(), CCBAPI.asResource("test/native_before_crushing")).require(Items.IRON_INGOT).require(Items.HEAVY_CORE).output(Items.DIAMOND).build();
            CrushingRecipe crushing = new Builder<>(CrushingRecipe::new, CCBAPI.asResource("test/crushing_after_native")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            ConfigBool enabled = CCBConfig.server().machines.airtightForgingPress.enableAutomaticCrushingRecipes;
            boolean previous = enabled.get();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(nativeRecipe, crushing))) {
                for (boolean crushingEnabled : List.of(true, false)) {
                    enabled.set(crushingEnabled);
                    press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                    press.getOutputInventory().setStackInSlot(0, ItemStack.EMPTY);
                    AirtightForgingPressController controller = new AirtightForgingPressController(press);
                    controller.updateForgingPress();
                    for (int tick = 0; tick < 15; tick++) {
                        controller.tick();
                    }
                    helper.assertTrue(press.getOutputInventory().getStackInSlot(0).is(Items.DIAMOND), "Native forging lost priority or was disabled by the crushing switch");
                }
            }
            finally {
                enabled.set(previous);
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void removedCrushingRecipeAndCoreCannotConsumeInput(GameTestHelper helper) {
        withPress(helper, press -> {
            CrushingRecipe recipe = new Builder<>(CrushingRecipe::new, CCBAPI.asResource("test/crushing_reload")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            try (RecipeIndexTestScope scope = new RecipeIndexTestScope(helper.getLevel(), List.of(recipe))) {
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
                AirtightForgingPressController controller = new AirtightForgingPressController(press);
                controller.updateForgingPress();
                scope.reload(List.of());
                for (int tick = 0; tick < 15; tick++) {
                    controller.tick();
                }
                helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 4, "input after recipe removal before impact");
                helper.assertTrue(press.getOutputInventory().isEmpty(), "Removed recipe produced output");
                scope.reload(List.of(recipe));
                controller.lazyTick();
                controller.updateForgingPress();
                press.getPressHeadInventory().setStackInSlot(0, ItemStack.EMPTY);
                for (int tick = 0; tick < 15; tick++) {
                    controller.tick();
                }
                helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 4, "input after core removal before impact");
                helper.assertTrue(press.getOutputInventory().isEmpty(), "Crushing without a core produced output");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void matchedProbabilisticRecipeMayProduceNoOutput(GameTestHelper helper) {
        withPress(helper, press -> {
            CrushingRecipe recipe = new Builder<>(CrushingRecipe::new, CCBAPI.asResource("test/crushing_empty_roll")).require(Items.IRON_INGOT).output(0.5F, Items.DIAMOND).build();
            recipe.getRollableResults().set(0, new ProcessingOutput(new ItemStack(Items.DIAMOND), 0.5F) {
                @Override
                public ItemStack rollOutput(RandomSource random) {
                    return ItemStack.EMPTY;
                }
            });
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(recipe))) {
                press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                RecipeHolder<CrushingRecipe> match = AirtightForgingPressRecipeLookup.getMatchingCrushingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected a probabilistic crushing recipe."));
                Plan plan = AirtightForgingPressCrushing.prepare(press, match).orElseThrow(() -> new NoSuchElementException("Expected a valid empty crushing roll."));
                helper.assertTrue(press.commitCraft(plan.consumption(), plan.output()), "Valid empty probability roll did not commit");
                helper.assertTrue(press.getInputInventory().isEmpty() && press.getOutputInventory().isEmpty(), "Empty probability roll changed recipe semantics");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void registeredCreateCrushingRecipeCanBeProcessed(GameTestHelper helper) {
        withPress(helper, press -> {
            List<RecipeHolder<CrushingRecipe>> recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(AllRecipeTypes.CRUSHING.getType());
            RecipeHolder<CrushingRecipe> registered = recipes.stream().filter(holder -> "create:crushing/gravel".equals(holder.id().toString())).findFirst().orElseThrow(() -> new NoSuchElementException("Expected the registered Create gravel crushing recipe."));
            press.getInputInventory().setStackInSlot(0, new ItemStack(Items.GRAVEL));
            helper.assertTrue(AirtightForgingPressCrushing.canApply(press, registered), "Registered Create crushing recipe was not accepted");
            Plan plan = AirtightForgingPressCrushing.prepare(press, registered).orElseThrow(() -> new NoSuchElementException("Expected a registered crushing recipe plan."));
            helper.assertTrue(press.commitCraft(plan.consumption(), plan.output()), "Registered Create crushing recipe failed to commit");
            helper.assertTrue(press.getInputInventory().isEmpty(), "Registered crushing recipe did not consume gravel");
            helper.assertTrue(press.getPressHeadInventory().getStackInSlot(0).is(Items.HEAVY_CORE), "Registered crushing recipe consumed its core");
        });
    }

    private static void withPress(GameTestHelper helper, Consumer<AirtightForgingPressBlockEntity> test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.getDefaultState());
        helper.runAfterDelay(2, () -> {
            AirtightForgingPressBlockEntity press = helper.getBlockEntity(pos);
            BlockPos shaftPos = pos.above();
            AirtightForgingPressStructuralShaftBlockEntity shaft = helper.getBlockEntity(shaftPos);
            shaft.setSpeed(SpeedLevel.FAST.getSpeedValue());
            press.getPressHeadInventory().setStackInSlot(0, new ItemStack(Items.HEAVY_CORE));
            test.accept(press);
            helper.succeed();
        });
    }
}
