package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferResult;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner.Plan;
import net.ty.createcraftedbeginning.recipe.ResidueRecipeLookup.ResidueOutput;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipeLookup.WindChargingData;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeData;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.ConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.OutputPlan;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StateOwnershipGameTests {
    private StateOwnershipGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void transferResultKeepsCustodyAccountingStable(GameTestHelper helper) {
        GasStack remainder = new GasStack(CCBGases.NATURAL_AIR.get(), 20);
        GasPressureTransferResult result = new GasPressureTransferResult(100, 100, 100, 70, 10, remainder, true);
        remainder.setAmount(1);
        result.unrecoveredGas().setAmount(2);
        helper.assertValueEqual(result.unrecoveredAmount(), 20L, "owned transfer remainder");
        helper.assertTrue(result.custodyAccountedFor(), "Mutating a returned gas stack changed custody accounting");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void recipeGasResultsDoNotShareMutableStacks(GameTestHelper helper) {
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        List<GasStack> inputs = new ArrayList<>(List.of(gas));
        GasRecipeData data = new GasRecipeData(List.of(), inputs);
        gas.setAmount(1);
        inputs.clear();
        data.results().getFirst().setAmount(2);
        data.resultStacks().getFirst().setAmount(3);
        helper.assertValueEqual(data.results().getFirst().getAmount(), 100L, "recipe gas result");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void residueOutputsKeepTheirNormalizedTemplate(GameTestHelper helper) {
        ItemStack item = new ItemStack(Items.IRON_INGOT, 5);
        FluidStack fluid = new FluidStack(Fluids.WATER, 100);
        ResidueOutput itemOutput = new ResidueOutput(item, FluidStack.EMPTY);
        ResidueOutput fluidOutput = new ResidueOutput(ItemStack.EMPTY, fluid);
        item.setCount(2);
        fluid.setAmount(2);
        itemOutput.itemStack().setCount(8);
        fluidOutput.fluidStack().setAmount(8);
        helper.assertValueEqual(itemOutput.itemStack().getCount(), 1, "normalized residue item");
        helper.assertValueEqual(fluidOutput.fluidStack().getAmount(), 1, "normalized residue fluid");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void windChargingResultOwnsItsItem(GameTestHelper helper) {
        ItemStack item = new ItemStack(Items.IRON_INGOT, 3);
        WindChargingData data = new WindChargingData(WindChargingAction.CHARGE, 20, 1, item);
        item.setCount(1);
        data.recipeResult().setCount(2);
        helper.assertValueEqual(data.recipeResult().getCount(), 3, "wind charging result");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void reactorPlanOwnsAllocationAndOutputSnapshots(GameTestHelper helper) {
        int[] itemAmounts = {2};
        int[] fluidAmounts = {100};
        ItemStack item = new ItemStack(Items.IRON_INGOT, 3);
        FluidStack fluid = new FluidStack(Fluids.WATER, 200);
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 300);
        List<ItemStack> items = new ArrayList<>(List.of(item));
        Plan plan = new Plan(itemAmounts, fluidAmounts, GasConsumptionPlan.empty(0), items, List.of(fluid), List.of(gas));
        itemAmounts[0] = 0;
        fluidAmounts[0] = 0;
        item.setCount(1);
        fluid.setAmount(1);
        gas.setAmount(1);
        items.clear();
        plan.itemAmounts()[0] = 0;
        plan.fluidAmounts()[0] = 0;
        plan.outputItems().getFirst().setCount(1);
        plan.outputFluids().getFirst().setAmount(1);
        plan.outputGases().getFirst().setAmount(1);
        helper.assertValueEqual(plan.itemAmounts()[0], 2, "planned item amount");
        helper.assertValueEqual(plan.fluidAmounts()[0], 100, "planned fluid amount");
        helper.assertValueEqual(plan.outputItems().getFirst().getCount(), 3, "planned item output");
        helper.assertValueEqual(plan.outputFluids().getFirst().getAmount(), 200, "planned fluid output");
        helper.assertValueEqual(plan.outputGases().getFirst().getAmount(), 300L, "planned gas output");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void forgingPlansProtectValidationAndOutputSnapshots(GameTestHelper helper) {
        ItemStack item = new ItemStack(Items.IRON_INGOT, 3);
        FluidStack fluid = new FluidStack(Fluids.WATER, 100);
        ConsumptionPlan consumption = new ConsumptionPlan(item, item, 1, item, 1, fluid, 50, GasConsumptionPlan.empty(0));
        OutputPlan output = new OutputPlan(List.of(item), List.of(item));
        item.setCount(1);
        fluid.setAmount(1);
        consumption.expectedPressHeadStack().setCount(1);
        consumption.expectedProcessingStack().setCount(1);
        consumption.expectedInputStack().setCount(1);
        consumption.expectedFluid().setAmount(1);
        output.expectedSlots().getFirst().setCount(1);
        output.finalSlots().getFirst().setCount(1);
        helper.assertValueEqual(consumption.expectedPressHeadStack().getCount(), 3, "press head snapshot");
        helper.assertValueEqual(consumption.expectedProcessingStack().getCount(), 3, "processing snapshot");
        helper.assertValueEqual(consumption.expectedInputStack().getCount(), 3, "input snapshot");
        helper.assertValueEqual(consumption.expectedFluid().getAmount(), 100, "fluid snapshot");
        helper.assertValueEqual(output.expectedSlots().getFirst().getCount(), 3, "expected output snapshot");
        helper.assertValueEqual(output.finalSlots().getFirst().getCount(), 3, "final output snapshot");
        helper.succeed();
    }
}
