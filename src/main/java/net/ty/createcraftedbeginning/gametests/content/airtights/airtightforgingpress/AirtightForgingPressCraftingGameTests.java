package net.ty.createcraftedbeginning.gametests.content.airtights.airtightforgingpress;

import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressAutomationPlanner;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressCrafting;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressPressingRecipe;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPreparation;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPreparation.Plan;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Serializer;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightForgingPressCraftingGameTests {
    private AirtightForgingPressCraftingGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void nativeBatchStopsAtAvailableOutputSpace(GameTestHelper helper) {
        withPress(helper, press -> {
            SmartInventory input = press.getInputInventory();
            input.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
            SmartInventory output = press.getOutputInventory();
            for (int slot = 0; slot < output.getSlots(); slot++) {
                output.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
            }
            output.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 61));
            Serializer<ForgingPressRecipe> serializer = CCBRecipeTypes.FORGING_PRESS.getSerializer();
            ForgingPressRecipe recipe = new Builder<>(serializer.factory(), CCBAPI.asResource("test/native_batch")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            ForgingPressCraftPlanner planner = new ForgingPressCraftPlanner(press, recipe);

            helper.assertTrue(planner.matches(), "A partial native batch was not accepted");
            Plan plan = ForgingPressCraftPreparation.prepare(press, recipe).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'nativeBatchStopsAtAvailableOutputSpace'."));
            helper.assertValueEqual(plan.consumption().inputAmount(), 3, "planned native batch size");
            helper.assertValueEqual(input.getStackInSlot(0).getCount(), 8, "input after planning");
            helper.assertTrue(AirtightForgingPressCrafting.applyRecipe(press, recipe), "Native batch did not commit");
            helper.assertValueEqual(input.getStackInSlot(0).getCount(), 5, "input after native craft");
            helper.assertValueEqual(output.getStackInSlot(0).getCount(), 64, "output after native craft");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void automaticPressingReservesRemaindersAndRejectsChangedOutput(GameTestHelper helper) {
        withPress(helper, press -> {
            SmartInventory output = press.getOutputInventory();
            SmartInventory input = press.getInputInventory();
            input.setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
            PressingRecipe recipe = new StandardProcessingRecipe.Builder<>(PressingRecipe::new, CCBAPI.asResource("test/pressing_remainder")).require(Items.WATER_BUCKET).output(Items.GOLD_INGOT).build();
            AirtightForgingPressPressingRecipe pressingRecipe = new AirtightForgingPressPressingRecipe(recipe, input.getStackInSlot(0), null);
            Plan plan = AirtightForgingPressAutomationPlanner.preparePressingRecipe(press, pressingRecipe).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'automaticPressingReservesRemaindersAndRejectsChangedOutput'."));
            output.setStackInSlot(0, new ItemStack(Items.DIRT));

            helper.assertTrue(!press.commitCraft(plan.consumption(), plan.output()), "A stale output snapshot committed");
            helper.assertTrue(input.getStackInSlot(0).is(Items.WATER_BUCKET), "Rejected output plan consumed the input");
            output.setStackInSlot(0, ItemStack.EMPTY);
            helper.assertTrue(AirtightForgingPressCrafting.applyPressingRecipe(press, pressingRecipe), "Automatic pressing failed");
            helper.assertTrue(output.getStackInSlot(0).is(Items.GOLD_INGOT) && output.getStackInSlot(1).is(Items.BUCKET), "Automatic pressing lost its result or container remainder");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void smithingKeepsTheTemplateAndInputComponents(GameTestHelper helper) {
        withPress(helper, press -> {
            SmartInventory addition = press.getAdditionInventory();
            SmartInventory head = press.getPressHeadInventory();
            SmartInventory input = press.getInputInventory();
            head.setStackInSlot(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            ItemStack base = new ItemStack(Items.DIAMOND_SWORD);
            base.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved name"));
            input.setStackInSlot(0, base);
            addition.setStackInSlot(0, new ItemStack(Items.NETHERITE_INGOT));
            SmithingTransformRecipe recipe = new SmithingTransformRecipe(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(Items.DIAMOND_SWORD), Ingredient.of(Items.NETHERITE_INGOT), new ItemStack(Items.NETHERITE_SWORD));

            helper.assertTrue(AirtightForgingPressCrafting.applySmithingRecipe(press, recipe), "Automatic smithing failed");
            helper.assertTrue(head.getStackInSlot(0).is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), "Smithing consumed its reusable template");
            helper.assertTrue(input.isEmpty() && addition.isEmpty(), "Smithing did not consume its base and addition");
            ItemStack result = press.getOutputInventory().getStackInSlot(0);
            helper.assertTrue(result.is(Items.NETHERITE_SWORD) && Component.literal("Preserved name").equals(result.get(DataComponents.CUSTOM_NAME)), "Smithing lost input components");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void nativeBatchLeavesTheRequiredGasPressure(GameTestHelper helper) {
        withPress(helper, press -> {
            GasStorageHandler gas = press.getGasCapability();
            press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
            long volume = gas.getTankVolume(0);
            long minimum = GasPressure.amount(volume, GasPressure.pascals(2));
            long remaining = minimum + 350;
            helper.assertValueEqual(gas.fill(new GasStack(CCBGases.NATURAL_AIR.get(), remaining), GasAction.EXECUTE), remaining, "initial gas fill");
            Serializer<ForgingPressRecipe> serializer = CCBRecipeTypes.FORGING_PRESS.getSerializer();
            ForgingPressRecipe recipe = new Builder<>(serializer.factory(), CCBAPI.asResource("test/pressure_limited_batch")).require(Items.IRON_INGOT).require(CCBGases.NATURAL_AIR.get(), 100, PressureRequirement.atLeast(GasPressure.pascals(2))).output(Items.GOLD_INGOT).build();
            Plan plan = ForgingPressCraftPreparation.prepare(press, recipe).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'nativeBatchLeavesTheRequiredGasPressure'."));

            helper.assertValueEqual(plan.consumption().inputAmount(), 3, "pressure limited batch size");
            helper.assertValueEqual(gas.getGasInTank(0).getAmount(), remaining, "gas after planning");
            helper.assertTrue(AirtightForgingPressCrafting.applyRecipe(press, recipe), "Pressure limited batch did not commit");
            helper.assertValueEqual(gas.getGasInTank(0).getAmount(), minimum + 50, "gas after batch");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void previewDoesNotRollAndCommitUsesPreparedOutputs(GameTestHelper helper) {
        withPress(helper, press -> {
            SmartInventory input = press.getInputInventory();
            input.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));
            Serializer<ForgingPressRecipe> serializer = CCBRecipeTypes.FORGING_PRESS.getSerializer();
            ForgingPressRecipe recipe = new Builder<>(serializer.factory(), CCBAPI.asResource("test/roll_boundary")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            int[] rolls = {0};
            recipe.getRollableResults().set(0, new ProcessingOutput(new ItemStack(Items.GOLD_INGOT), 1) {
                @Override
                public ItemStack rollOutput(RandomSource random) {
                    rolls[0]++;
                    if (rolls[0] == 2) {
                        return ItemStack.EMPTY;
                    }

                    return new ItemStack(Items.GOLD_INGOT);
                }
            });
            ForgingPressCraftPlanner planner = new ForgingPressCraftPlanner(press, recipe);
            helper.assertTrue(planner.matches(), "Preview rejected a valid batch");
            planner.getPressureSpeedMultiplier();
            helper.assertValueEqual(rolls[0], 0, "rolls during recipe and speed planning");
            Plan plan = ForgingPressCraftPreparation.prepare(press, recipe).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'rollOutput'."));
            helper.assertValueEqual(rolls[0], 3, "one roll per prepared craft");
            helper.assertValueEqual(input.getStackInSlot(0).getCount(), 3, "input before commit");
            helper.assertTrue(press.commitCraft(plan.consumption(), plan.output()), "Prepared batch did not commit");
            helper.assertValueEqual(rolls[0], 3, "commit must not roll again");
            helper.assertTrue(input.isEmpty(), "Batch input was not consumed");
            helper.assertValueEqual(press.getOutputInventory().getStackInSlot(0).getCount(), 2, "committed prepared results");
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
