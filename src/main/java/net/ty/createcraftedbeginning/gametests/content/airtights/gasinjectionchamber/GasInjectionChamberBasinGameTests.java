package net.ty.createcraftedbeginning.gametests.content.airtights.gasinjectionchamber;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinPlanner;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinPlanner.BasinPlan;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinPlanner.ItemDrain;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinProcessor;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBeltOutputs;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberFilterState;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationPlanner;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationPlanner.BeltPlan;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Serializer;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasInjectionChamberBasinGameTests {
    private GasInjectionChamberBasinGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void batchPlanningKeepsResourcesUntilCommit(GameTestHelper helper) {
        withBasin(helper, (chamber, basin) -> {
            GasInjectionRecipe recipe = recipe();
            GasTank gas = chamber.getGasTankBehaviour().getPrimaryHandler();
            gas.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 250));
            basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 5));
            BasinPlan plan = new GasInjectionChamberBasinPlanner(chamber).createPlan(basin, recipe).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'batchPlanningKeepsResourcesUntilCommit'."));

            helper.assertValueEqual(plan.itemInputs().stream().mapToInt(ItemDrain::count).sum(), 2, "Gas limited basin batch");
            helper.assertValueEqual(gas.getStoredAmount(), 250L, "Gas after planning");
            helper.assertValueEqual(basin.getInputInventory().getStackInSlot(0).getCount(), 5, "Items after planning");
            GasInjectionChamberOperationState operation = new GasInjectionChamberOperationState();
            operation.startProcessing(OperationType.BASIN_RECIPE, 1, recipe);
            helper.assertTrue(new GasInjectionChamberBasinProcessor(chamber, operation).executeCurrentState(), "Basin craft did not commit");
            helper.assertValueEqual(gas.getStoredAmount(), 50L, "Gas after commit");
            helper.assertValueEqual(basin.getInputInventory().getStackInSlot(0).getCount(), 3, "Items after commit");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void changedInputRejectsExecutionWithoutGasLoss(GameTestHelper helper) {
        withBasin(helper, (chamber, basin) -> {
            GasInjectionRecipe recipe = recipe();
            GasTank gas = chamber.getGasTankBehaviour().getPrimaryHandler();
            gas.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 250));
            basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
            helper.assertTrue(new GasInjectionChamberBasinPlanner(chamber).createPlan(basin, recipe).isPresent(), "Initial basin plan missing");
            GasInjectionChamberOperationState operation = new GasInjectionChamberOperationState();
            operation.startProcessing(OperationType.BASIN_RECIPE, 1, recipe);
            basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.DIRT, 2));

            helper.assertTrue(!new GasInjectionChamberBasinProcessor(chamber, operation).executeCurrentState(), "Changed input committed");
            helper.assertValueEqual(gas.getStoredAmount(), 250L, "Gas after rejected execution");
            helper.assertTrue(basin.getInputInventory().getStackInSlot(0).is(Items.DIRT), "Rejected execution changed input identity");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void basinRollsOnlyDuringExecution(GameTestHelper helper) {
        withBasin(helper, (chamber, basin) -> {
            GasInjectionRecipe recipe = recipe();
            int[] rolls = {0};
            recipe.getRollableResults().set(0, new ProcessingOutput(new ItemStack(Items.GOLD_INGOT), 0) {
                @Override
                public ItemStack rollOutput(RandomSource random) {
                    rolls[0]++;
                    return ItemStack.EMPTY;
                }
            });
            GasTank gas = chamber.getGasTankBehaviour().getPrimaryHandler();
            gas.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 250));
            basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
            GasInjectionChamberBasinPlanner planner = new GasInjectionChamberBasinPlanner(chamber);
            helper.assertTrue(planner.createPlan(basin, recipe).isPresent(), "Potential output preview was rejected");
            helper.assertTrue(planner.createPlan(basin, recipe).isPresent(), "Repeated preview was rejected");
            helper.assertValueEqual(rolls[0], 0, "basin preview rolls");
            GasInjectionChamberOperationState operation = new GasInjectionChamberOperationState();
            operation.startProcessing(OperationType.BASIN_RECIPE, 1, recipe);
            helper.assertTrue(new GasInjectionChamberBasinProcessor(chamber, operation).executeCurrentState(), "Empty random output must still complete its craft");
            helper.assertValueEqual(rolls[0], 2, "basin execution rolls");
            helper.assertValueEqual(gas.getStoredAmount(), 50L, "gas for completed empty-output crafts");
            helper.assertTrue(basin.getInputInventory().isEmpty(), "Empty-output crafts did not consume their input");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void beltPlanningLeavesOutputGenerationToExecution(GameTestHelper helper) {
        withBasin(helper, (chamber, basin) -> {
            GasInjectionRecipe recipe = recipe();
            int[] rolls = {0};
            recipe.getRollableResults().set(0, new ProcessingOutput(new ItemStack(Items.GOLD_INGOT), 1) {
                @Override
                public ItemStack rollOutput(RandomSource random) {
                    rolls[0]++;
                    return new ItemStack(Items.GOLD_INGOT);
                }
            });
            GasTank gas = chamber.getGasTankBehaviour().getPrimaryHandler();
            gas.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 250));
            GasInjectionChamberFilterState filter = new GasInjectionChamberFilterState();
            GasInjectionChamberOperationPlanner planner = new GasInjectionChamberOperationPlanner(chamber, filter);
            BeltPlan plan = planner.createRecipePlan(new ItemStack(Items.IRON_INGOT, 3), recipe).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'rollOutput'."));
            helper.assertValueEqual(rolls[0], 0, "belt planning rolls");
            List<ItemStack> outputs = new GasInjectionChamberBeltOutputs(chamber, filter).createResults(plan).orElseThrow(() -> new NoSuchElementException("Expected recipe outputs in test 'rollOutput'."));
            helper.assertValueEqual(rolls[0], 2, "one belt roll per craft");
            helper.assertValueEqual(outputs.getFirst().getCount(), 2, "merged belt outputs");
            helper.assertValueEqual(gas.getStoredAmount(), 250L, "output generation must not commit fuel");
        });
    }

    private static GasInjectionRecipe recipe() {
        Serializer<GasInjectionRecipe> serializer = CCBRecipeTypes.GAS_INJECTION.getSerializer();
        return new Builder<>(serializer.factory(), CCBAPI.asResource("test/basin_injection")).require(Items.IRON_INGOT).require(CCBGases.NATURAL_AIR.get(), 100).output(Items.GOLD_INGOT).build();
    }

    private static void withBasin(GameTestHelper helper, BiConsumer<GasInjectionChamberBlockEntity, BasinBlockEntity> test) {
        BlockPos basinPos = new BlockPos(2, 1, 2);
        helper.setBlock(basinPos, AllBlocks.BASIN.getDefaultState());
        helper.setBlock(basinPos.above(2), CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.getDefaultState());
        helper.runAfterDelay(2, () -> {
            GasInjectionChamberBlockEntity chamber = helper.getBlockEntity(basinPos.above(2));
            BasinBlockEntity basin = helper.getBlockEntity(basinPos);
            test.accept(chamber, basin);
            helper.succeed();
        });
    }
}
