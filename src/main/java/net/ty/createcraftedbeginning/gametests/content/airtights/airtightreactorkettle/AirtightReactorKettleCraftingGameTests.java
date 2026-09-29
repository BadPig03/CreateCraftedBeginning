package net.ty.createcraftedbeginning.gametests.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity.CraftPlan;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleCrafting;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleMixingPlanner;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner.Plan;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPreparation;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightReactorKettleCraftingGameTests {
    private AirtightReactorKettleCraftingGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void overlappingIngredientsReserveTheSpecificItem(GameTestHelper helper) {
        withKettle(helper, kettle -> {
            SmartInventory input = kettle.getInputInventory();
            input.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            input.setStackInSlot(1, new ItemStack(Items.COPPER_INGOT));
            ReactorKettleRecipe recipe = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/overlapping_inputs")).require(Ingredient.of(Items.IRON_INGOT, Items.COPPER_INGOT)).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            ReactorKettleCraftPlanner planner = new ReactorKettleCraftPlanner(kettle, recipe);

            helper.assertTrue(planner.matches(), "The broad ingredient must leave iron for the specific ingredient");
            helper.assertTrue(input.getStackInSlot(0).is(Items.IRON_INGOT) && input.getStackInSlot(1).is(Items.COPPER_INGOT), "Matching consumed input items");
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "The planned recipe did not commit");
            helper.assertTrue(input.isEmpty(), "The recipe did not consume exactly both ingredients");
            helper.assertTrue(kettle.getOutputInventory().getStackInSlot(0).is(Items.GOLD_INGOT), "The result was not inserted");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void consumedOutputSlotCanHoldTheNextResult(GameTestHelper helper) {
        withKettle(helper, kettle -> {
            SmartInventory output = kettle.getOutputInventory();
            for (int slot = 0; slot < output.getSlots(); slot++) {
                output.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
            }
            output.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            ReactorKettleRecipe recipe = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/shared_output")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();

            helper.assertTrue(new ReactorKettleCraftPlanner(kettle, recipe).matches(), "Planning ignored the output slot freed by consumption");
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Shared input/output craft failed");
            helper.assertTrue(output.getStackInSlot(0).is(Items.GOLD_INGOT), "The freed output slot did not receive the result");
            helper.assertValueEqual(output.getStackInSlot(1).getCount(), 64, "unrelated output count");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void matchingReservesPotentialOutputsWithoutRolling(GameTestHelper helper) {
        withKettle(helper, kettle -> {
            SmartInventory input = kettle.getInputInventory();
            input.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            ReactorKettleRecipe recipe = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/chance_output")).require(Items.IRON_INGOT).output(0.0F, Items.DIAMOND).build();
            int[] rolls = {0};
            recipe.getRollableResults().set(0, new ProcessingOutput(new ItemStack(Items.DIAMOND), 0) {
                @Override
                public ItemStack rollOutput(RandomSource random) {
                    rolls[0]++;
                    return super.rollOutput(random);
                }
            });
            ReactorKettleCraftPlanner planner = new ReactorKettleCraftPlanner(kettle, recipe);

            helper.assertValueEqual(planner.plan().orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'rollOutput'.")).outputItems().size(), 1, "potential output count");
            helper.assertTrue(planner.matches(), "Preview rejected a valid recipe");
            helper.assertValueEqual(rolls[0], 0, "rolls after preview and matching");
            helper.assertTrue(ReactorKettleCraftPreparation.prepare(kettle, recipe).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'rollOutput'.")).outputItems().isEmpty(), "Execution planning did not roll the zero-chance output");
            helper.assertValueEqual(rolls[0], 1, "rolls after preparing one craft");
            helper.assertValueEqual(input.getStackInSlot(0).getCount(), 1, "input after both kinds of planning");
            kettle.setRecipeFilter(new ItemStack(Items.GOLD_INGOT));
            helper.assertTrue(!planner.matches(), "The changed filter was ignored");
            kettle.setRecipeFilter(new ItemStack(Items.DIAMOND));
            helper.assertTrue(planner.matches(), "The matching filter did not restore recipe eligibility");
            helper.assertValueEqual(rolls[0], 1, "rolls after repeated filter matching");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void automaticMixingReturnsContainersAndRejectsStalePlans(GameTestHelper helper) {
        withKettle(helper, kettle -> {
            SmartInventory output = kettle.getOutputInventory();
            SmartInventory input = kettle.getInputInventory();
            input.setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
            input.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
            ShapelessRecipe recipe = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.GOLD_INGOT), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.WATER_BUCKET), Ingredient.of(Items.IRON_INGOT)));
            CraftPlan plan = AirtightReactorKettleMixingPlanner.planCraftingRecipe(kettle, recipe).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'automaticMixingReturnsContainersAndRejectsStalePlans'."));
            input.setStackInSlot(1, new ItemStack(Items.COPPER_INGOT));

            helper.assertTrue(!kettle.commitCraft(plan), "A plan committed after its input changed");
            helper.assertTrue(input.getStackInSlot(0).is(Items.WATER_BUCKET) && output.isEmpty(), "Rejected plan changed resources");
            input.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
            helper.assertTrue(AirtightReactorKettleCrafting.applyCraftingRecipe(kettle, recipe), "Automatic mixing failed");
            helper.assertTrue(output.getStackInSlot(0).is(Items.GOLD_INGOT) && output.getStackInSlot(1).is(Items.BUCKET), "Automatic mixing lost its result or container remainder");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void fluidOutputFailureRollsBackEarlierItemChanges(GameTestHelper helper) {
        withKettle(helper, kettle -> {
            SmartInventory input = kettle.getInputInventory();
            input.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            int[] items = new int[kettle.getAvailableItems().getSlots()];
            items[0] = 1;
            int[] fluids = new int[kettle.getAvailableFluids().getTanks()];
            GasConsumptionPlan gases = GasConsumptionPlan.empty(kettle.getAvailableGases().getTanks());
            int capacity = AirtightReactorKettleBlockEntity.getFluidCapacity();
            CraftPlan plan = kettle.createCraftPlan(items, fluids, gases, List.of(new ItemStack(Items.GOLD_INGOT)), List.of(new FluidStack(Fluids.WATER, capacity * 3)), List.of());

            helper.assertTrue(!kettle.commitCraft(plan), "An oversized fluid output unexpectedly committed");
            helper.assertTrue(input.getStackInSlot(0).is(Items.IRON_INGOT), "Rollback lost the consumed input");
            helper.assertTrue(kettle.getOutputInventory().isEmpty(), "Rollback retained an item result");
            helper.assertTrue(kettle.getOutputFluidTank().getPrimaryHandler().isEmpty(), "Rollback retained partially inserted fluid");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void overlappingFluidsReserveTheSpecificTank(GameTestHelper helper) {
        withKettle(helper, kettle -> {
            IFluidHandler fluids = kettle.getAvailableFluids();
            IFluidHandler input = kettle.getInputFluidTank().getCapability();
            helper.assertValueEqual(input.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), 1000, "initial water fill");
            helper.assertValueEqual(input.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), 1000, "initial lava fill");
            ReactorKettleRecipe recipe = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/overlapping_fluids")).require(new SizedFluidIngredient(FluidIngredient.of(Fluids.WATER, Fluids.LAVA), 1000)).require(Fluids.WATER, 1000).output(Items.OBSIDIAN).build();
            Plan plan = new ReactorKettleCraftPlanner(kettle, recipe).plan().orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'overlappingFluidsReserveTheSpecificTank'."));

            helper.assertValueEqual(plan.fluidAmounts()[0], 1000, "specific water allocation");
            helper.assertValueEqual(plan.fluidAmounts()[1], 1000, "broad fluid allocation");
            helper.assertValueEqual(fluids.getFluidInTank(0).getAmount(), 1000, "water after simulation");
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Overlapping fluid recipe did not commit");
            helper.assertTrue(fluids.getFluidInTank(0).isEmpty() && fluids.getFluidInTank(1).isEmpty(), "Fluid consumption left unexpected contents");
        });
    }

    private static void withKettle(GameTestHelper helper, Consumer<AirtightReactorKettleBlockEntity> test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.getDefaultState());
        helper.runAfterDelay(2, () -> {
            AirtightReactorKettleBlockEntity kettle = helper.getBlockEntity(pos);
            test.accept(kettle);
            helper.succeed();
        });
    }
}
