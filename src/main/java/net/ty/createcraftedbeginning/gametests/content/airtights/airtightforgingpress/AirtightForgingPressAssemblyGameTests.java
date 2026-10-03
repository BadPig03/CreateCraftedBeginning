package net.ty.createcraftedbeginning.gametests.content.airtights.airtightforgingpress;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeBuilder;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressAutomationPlanner;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressController;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressCrafting;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressPressingRecipe;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressRecipeLookup;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPreparation.Plan;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightForgingPressAssemblyGameTests {
    private AirtightForgingPressAssemblyGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void firstPressingStepWinsOverOrdinaryPressingAndAdvancesBatch(GameTestHelper helper) {
        withAssembly(helper, (press, assembly) -> {
            press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
            AirtightForgingPressPressingRecipe recipe = AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected the first assembly pressing step."));
            helper.assertTrue(AirtightForgingPressCrafting.applyPressingRecipe(press, recipe), "First assembly step did not commit");
            ItemStack output = press.getOutputInventory().getStackInSlot(0);
            helper.assertValueEqual(output.getCount(), 8, "advanced batch size");
            helper.assertTrue(output.is(Items.PAPER), "Ordinary pressing overrode assembly pressing");
            helper.assertValueEqual(output.getOrDefault(AllDataComponents.SEQUENCED_ASSEMBLY, new SequencedAssembly(assembly.id(), 1, 0)), new SequencedAssembly(assembly.id(), 1, 0.16666667F), "first step progress");
            helper.assertTrue(press.getInputInventory().isEmpty(), "Batch input was not consumed");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void intermediateOutputsReserveSpaceForTheirNewProgress(GameTestHelper helper) {
        withAssembly(helper, (press, assembly) -> {
            SmartInventory output = press.getOutputInventory();
            for (int slot = 0; slot < output.getSlots(); slot++) {
                output.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
            }
            output.setStackInSlot(0, assemblyInput(assembly, 3, 61));
            press.getInputInventory().setStackInSlot(0, assemblyInput(assembly, 2, 8));
            AirtightForgingPressPressingRecipe recipe = AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected a partially fitting assembly batch."));
            Plan plan = AirtightForgingPressAutomationPlanner.preparePressingRecipe(press, recipe).orElseThrow(() -> new NoSuchElementException("Expected an assembly output plan."));
            helper.assertValueEqual(plan.consumption().inputAmount(), 3, "partial assembly batch size");
            helper.assertTrue(press.commitCraft(plan.consumption(), plan.output()), "Partial assembly batch failed");
            helper.assertValueEqual(output.getStackInSlot(0).getCount(), 64, "merged assembly outputs");
            helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 5, "remaining assembly inputs");
            helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).isEmpty(), "Full output accepted another batch");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void wrongStepChangedInputAndChangedRecipeAreRejected(GameTestHelper helper) {
        withAssembly(helper, (press, assembly) -> {
            SmartInventory input = press.getInputInventory();
            input.setStackInSlot(0, assemblyInput(assembly, 1, 1));
            helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).isEmpty(), "A cutting step was treated as ordinary pressing");
            input.setStackInSlot(0, assemblyInput(assembly, 2, 1));
            AirtightForgingPressPressingRecipe recipe = AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected an intermediate assembly pressing step."));
            input.setStackInSlot(0, assemblyInput(assembly, 5, 1));
            helper.assertTrue(!AirtightForgingPressCrafting.applyPressingRecipe(press, recipe), "A cached step accepted changed assembly progress");
            helper.assertValueEqual(input.getStackInSlot(0).getCount(), 1, "input after rejected progress change");
            input.setStackInSlot(0, assemblyInput(assembly, 2, 1));
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of())) {
                helper.assertTrue(!AirtightForgingPressCrafting.applyPressingRecipe(press, recipe), "A removed assembly recipe still committed");
            }
            helper.assertTrue(press.getOutputInventory().isEmpty(), "Rejected assembly produced output");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void executionRebindsSharedStepResultsToTheCurrentInput(GameTestHelper helper) {
        withAssembly(helper, (press, assembly) -> {
            press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
            AirtightForgingPressPressingRecipe recipe = AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected an initial assembly pressing step."));
            SequencedAssemblyRecipe.getRecipe(helper.getLevel(), assemblyInput(assembly, 3, 1), AllRecipeTypes.PRESSING.getType(), PressingRecipe.class).orElseThrow(() -> new NoSuchElementException("Expected a shared pressing step in a later loop."));
            helper.assertTrue(AirtightForgingPressCrafting.applyPressingRecipe(press, recipe), "Assembly failed after another machine queried the shared step");
            ItemStack output = press.getOutputInventory().getStackInSlot(0);
            helper.assertValueEqual(output.getOrDefault(AllDataComponents.SEQUENCED_ASSEMBLY, new SequencedAssembly(assembly.id(), 1, 0)), new SequencedAssembly(assembly.id(), 1, 0.16666667F), "progress after shared recipe rebinding");
            helper.assertValueEqual(output.getCount(), 2, "rebound batch size");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void finalStepPreviewsDoNotRollAndBatchResultsRollIndependently(GameTestHelper helper) {
        withAssembly(helper, (press, assembly) -> {
            SmartInventory input = press.getInputInventory();
            input.setStackInSlot(0, assemblyInput(assembly, 5, 32));
            press.setRecipeFilter(new ItemStack(Items.DIAMOND));
            RandomSource expectedRandom = RandomSource.create(481516);
            helper.getLevel().random.setSeed(481516);
            AirtightForgingPressPressingRecipe recipe = AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected the final assembly pressing step."));
            helper.assertTrue(AirtightForgingPressAutomationPlanner.canApplyPressingRecipe(press, recipe, input.getStackInSlot(0)), "Final step preview failed");
            helper.assertValueEqual(helper.getLevel().random.nextLong(), expectedRandom.nextLong(), "random state after previews");
            helper.getLevel().random.setSeed(481516);
            expectedRandom.setSeed(481516);
            int diamonds = 0;
            int gold = 0;
            for (int craft = 0; craft < 32; craft++) {
                if (expectedRandom.nextFloat() < 0.5F) {
                    diamonds++;
                    continue;
                }

                gold += 4;
            }
            Plan plan = AirtightForgingPressAutomationPlanner.preparePressingRecipe(press, recipe).orElseThrow(() -> new NoSuchElementException("Expected a final assembly batch plan."));
            helper.assertTrue(press.commitCraft(plan.consumption(), plan.output()), "Final assembly batch did not commit");
            helper.assertValueEqual(helper.getLevel().random.nextLong(), expectedRandom.nextLong(), "one final roll per input and no commit reroll");
            int actualDiamonds = 0;
            int actualGold = 0;
            SmartInventory output = press.getOutputInventory();
            for (int slot = 0; slot < output.getSlots(); slot++) {
                ItemStack stack = output.getStackInSlot(slot);
                helper.assertTrue(!stack.has(AllDataComponents.SEQUENCED_ASSEMBLY), "Finished item retained assembly progress");
                if (stack.is(Items.DIAMOND)) {
                    actualDiamonds += stack.getCount();
                }
                if (!stack.is(Items.GOLD_INGOT)) {
                    continue;
                }

                actualGold += stack.getCount();
            }
            helper.assertValueEqual(actualDiamonds, diamonds, "independent diamond results");
            helper.assertValueEqual(actualGold, gold, "independent multi-item gold results");
            helper.assertTrue(input.isEmpty(), "Final assembly inputs were not consumed");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void finalStepRequiresSpaceForAlternativeResultsAndUsesPrimaryFilter(GameTestHelper helper) {
        withAssembly(helper, (press, assembly) -> {
            SmartInventory output = press.getOutputInventory();
            for (int slot = 0; slot < output.getSlots(); slot++) {
                output.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
            }
            output.setStackInSlot(0, new ItemStack(Items.DIAMOND, 63));
            press.getInputInventory().setStackInSlot(0, assemblyInput(assembly, 5, 1));
            helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).isEmpty(), "Final step ignored a blocked alternative result");
            output.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT, 60));
            press.setRecipeFilter(new ItemStack(Items.GOLD_INGOT));
            helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).isEmpty(), "A secondary result passed the primary-result filter");
            press.setRecipeFilter(new ItemStack(Items.DIAMOND));
            helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).isPresent(), "Primary result filter rejected a safe final step");
            helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 1, "input after final-step previews");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void pressingAnimationCommitsAssemblyOnceAndRequiresEmptyToolSlots(GameTestHelper helper) {
        withAssembly(helper, (press, assembly) -> {
            press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
            press.getPressHeadInventory().setStackInSlot(0, new ItemStack(Items.HEAVY_CORE));
            helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).isEmpty(), "Assembly accepted a press head tool");
            press.getPressHeadInventory().setStackInSlot(0, ItemStack.EMPTY);
            press.getAdditionInventory().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
            helper.assertTrue(AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).isEmpty(), "Assembly accepted an addition item");
            press.getAdditionInventory().setStackInSlot(0, ItemStack.EMPTY);
            BlockPos shaftPos = new BlockPos(2, 3, 2);
            AirtightForgingPressStructuralShaftBlockEntity shaft = helper.getBlockEntity(shaftPos);
            shaft.setSpeed(SpeedLevel.FAST.getSpeedValue());
            AirtightForgingPressController controller = new AirtightForgingPressController(press);
            helper.assertTrue(controller.updateForgingPress() && controller.isOperating(), "Assembly did not start a pressing cycle");
            for (int tick = 0; tick < 14; tick++) {
                controller.tick();
            }
            helper.assertTrue(press.getOutputInventory().isEmpty(), "Assembly committed before the press impact");
            for (int tick = 0; tick < 17; tick++) {
                controller.tick();
            }
            helper.assertValueEqual(press.getOutputInventory().getStackInSlot(0).getCount(), 2, "one assembly batch per animation");
            helper.assertTrue(!controller.isOperating(), "Assembly animation did not complete");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void registeredHeavyCoreAssemblyAdvancesItsLoopAndFinishes(GameTestHelper helper) {
        withPress(helper, press -> {
            RecipeHolder<?> registered = helper.getLevel().getRecipeManager().byKey(CCBAPI.asResource("sequenced_assembly/heavy_core")).orElseThrow(() -> new NoSuchElementException("Expected the registered heavy core assembly recipe."));
            Recipe<?> registeredRecipe = registered.value();
            helper.assertTrue(registeredRecipe instanceof SequencedAssemblyRecipe, "Heavy core recipe was not a sequenced assembly");
            if (!(registeredRecipe instanceof SequencedAssemblyRecipe recipe)) {
                return;
            }

            RecipeHolder<SequencedAssemblyRecipe> assembly = new RecipeHolder<>(registered.id(), recipe);
            SmartInventory input = press.getInputInventory();
            input.setStackInSlot(0, assemblyInput(assembly, 4, 1));
            AirtightForgingPressPressingRecipe intermediate = AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected the registered heavy core pressing step."));
            helper.assertTrue(AirtightForgingPressCrafting.applyPressingRecipe(press, intermediate), "Heavy core intermediate pressing failed");
            SmartInventory output = press.getOutputInventory();
            SequencedAssembly progress = output.getStackInSlot(0).get(AllDataComponents.SEQUENCED_ASSEMBLY);
            if (progress == null) {
                throw new NullPointerException("Expected sequenced assembly progress in heavy core output slot 0.");
            }

            helper.assertValueEqual(progress, new SequencedAssembly(assembly.id(), 5, 0.25F), "heavy core loop progress");
            output.setStackInSlot(0, ItemStack.EMPTY);
            input.setStackInSlot(0, assemblyInput(assembly, 19, 1));
            AirtightForgingPressPressingRecipe last = AirtightForgingPressRecipeLookup.getMatchingPressingRecipe(press).orElseThrow(() -> new NoSuchElementException("Expected the final heavy core pressing step."));
            helper.assertTrue(AirtightForgingPressCrafting.applyPressingRecipe(press, last), "Heavy core final pressing failed");
            ItemStack result = output.getStackInSlot(0);
            helper.assertTrue(result.is(Items.HEAVY_CORE) && result.getCount() == 1 || result.is(Items.NETHERITE_INGOT) && result.getCount() == 4, "Heavy core final pressing produced an invalid result");
            helper.assertTrue(!result.has(AllDataComponents.SEQUENCED_ASSEMBLY), "Heavy core final result retained assembly progress");
            helper.assertTrue(input.isEmpty(), "Heavy core final pressing did not consume its input");
        });
    }

    private static ItemStack assemblyInput(RecipeHolder<SequencedAssemblyRecipe> assembly, int step, int count) {
        ItemStack input = assembly.value().getTransitionalItem().copyWithCount(count);
        input.set(AllDataComponents.SEQUENCED_ASSEMBLY, new SequencedAssembly(assembly.id(), step, (float) step / (assembly.value().getSequence().size() * assembly.value().getLoops())));
        return input;
    }

    private static void withAssembly(GameTestHelper helper, BiConsumer<AirtightForgingPressBlockEntity, RecipeHolder<SequencedAssemblyRecipe>> test) {
        withPress(helper, press -> {
            SequencedAssemblyRecipe assembly = new SequencedAssemblyRecipeBuilder(CCBAPI.asResource("test/forging_assembly")).require(Items.IRON_INGOT).transitionTo(Items.PAPER).addOutput(Items.DIAMOND, 1).addOutput(new ItemStack(Items.GOLD_INGOT, 4), 1).loops(2).addStep(PressingRecipe::new, builder -> builder).addStep(CuttingRecipe::new, builder -> builder.duration(20)).addStep(PressingRecipe::new, builder -> builder).build().value();
            PressingRecipe ordinary = new Builder<>(PressingRecipe::new, CCBAPI.asResource("test/ordinary_pressing")).require(Items.IRON_INGOT).output(Items.IRON_NUGGET).build();
            PressingRecipe transitional = new Builder<>(PressingRecipe::new, CCBAPI.asResource("test/transitional_pressing")).require(Items.PAPER).output(Items.IRON_NUGGET).build();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(assembly, ordinary, transitional))) {
                List<RecipeHolder<SequencedAssemblyRecipe>> assemblies = helper.getLevel().getRecipeManager().getAllRecipesFor(AllRecipeTypes.SEQUENCED_ASSEMBLY.getType());
                test.accept(press, assemblies.getFirst());
            }
        });
    }

    private static void withPress(GameTestHelper helper, Consumer<AirtightForgingPressBlockEntity> test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.getDefaultState());
        helper.runAfterDelay(2, () -> {
            AirtightForgingPressBlockEntity press = helper.getBlockEntity(pos);
            test.accept(press);
            helper.succeed();
        });
    }
}
