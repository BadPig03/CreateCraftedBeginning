package net.ty.createcraftedbeginning.gametests.content.airtights.airtightreactorkettle;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleAnimationState;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleController;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleCrafting;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleRecipeLookup;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogBlockEntity;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope.TrackingIngredient;
import net.ty.createcraftedbeginning.recipe.ChilledMixingRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ReactorKettleMixingRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReactorKettleMixingGameTests {
    private ReactorKettleMixingGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void unknownMixingSubclassesAreExcludedFromAutomaticRecipes(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            MixingRecipe special = new Builder<>(params -> new MixingRecipe(params) {}, CCBAPI.asResource("test/special_mixing")).require(Fluids.WATER, 250).output(Items.SNOW_BLOCK).build();
            ChilledMixingRecipe specialChilled = new Builder<>(params -> new ChilledMixingRecipe(params) {}, CCBAPI.asResource("test/special_chilled_mixing")).require(Fluids.WATER, 500).output(Items.ICE).build();
            RecipeHolder<MixingRecipe> specialHolder = new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("fluidlogistics", "test/special_mixing"), special);
            helper.assertTrue(!ReactorKettleMixingRecipe.isSupported(specialHolder), "Unknown mixing subclass was included in automatic recipes");
            helper.assertTrue(!ReactorKettleMixingRecipe.isSupported(new RecipeHolder<>(CCBAPI.asResource("test/special_chilled_mixing"), specialChilled)), "Unknown chilled mixing subclass was included in automatic recipes");
            boolean conversionRejected = false;
            try {
                ReactorKettleMixingRecipe.convert(specialHolder);
            }
            catch (IllegalArgumentException exception) {
                conversionRejected = true;
            }
            helper.assertTrue(conversionRejected, "Direct conversion accepted an unsupported mixing recipe");
            try (RecipeIndexTestScope scope = new RecipeIndexTestScope(helper.getLevel(), List.of(special, specialChilled))) {
                kettle.getInputFluidTank().getCapability().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
                for (int value : new int[]{-3, -1, 0, 1, 3}) {
                    temperature[0] = value;
                    helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).isEmpty(), "Unknown cooling recipe remained selectable at temperature " + value);
                }
                helper.assertValueEqual(kettle.getInputFluidTank().getCapability().getFluidInTank(0).getAmount(), 1000, "water after unsupported recipe queries");
                helper.assertTrue(kettle.getOutputInventory().isEmpty(), "Unsupported cooling recipe produced output");
                MixingRecipe standard = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/standard_mixing")).require(Fluids.WATER, 500).output(Items.CLAY_BALL).build();
                RecipeHolder<MixingRecipe> standardHolder = new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("fluidlogistics", "test/standard_mixing"), standard);
                helper.assertTrue(ReactorKettleMixingRecipe.isSupported(standardHolder), "Standard mixing was rejected because of its namespace");
                scope.reload(List.of(special, specialChilled, standard));
                ReactorKettleRecipe match = AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a supported mixing recipe after excluding special recipes."));
                helper.assertTrue(match.getResultItem(helper.getLevel().registryAccess()).is(Items.CLAY_BALL), "Unsupported recipe displaced standard mixing");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void mixingMapsHeatAndColdWithoutKeepingSourceDuration(GameTestHelper helper) {
        MixingRecipe ambient = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/ambient_mixing")).require(Items.IRON_INGOT).duration(400).output(Items.GOLD_INGOT).build();
        MixingRecipe heated = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/heated_mixing")).require(Items.IRON_INGOT).requiresHeat(HeatCondition.HEATED).output(Items.GOLD_INGOT).build();
        MixingRecipe superheated = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/superheated_mixing")).require(Items.IRON_INGOT).requiresHeat(HeatCondition.SUPERHEATED).output(Items.GOLD_INGOT).build();
        ChilledMixingRecipe chilled = new Builder<>(ChilledMixingRecipe::new, CCBAPI.asResource("test/chilled_mixing")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
        ReactorKettleRecipe ambientAdapter = ReactorKettleMixingRecipe.convert(new RecipeHolder<>(CCBAPI.asResource("test/ambient_mixing"), ambient)).value();
        ReactorKettleRecipe heatedAdapter = ReactorKettleMixingRecipe.convert(new RecipeHolder<>(CCBAPI.asResource("test/heated_mixing"), heated)).value();
        ReactorKettleRecipe superheatedAdapter = ReactorKettleMixingRecipe.convert(new RecipeHolder<>(CCBAPI.asResource("test/superheated_mixing"), superheated)).value();
        ReactorKettleRecipe chilledAdapter = ReactorKettleMixingRecipe.convert(new RecipeHolder<>(CCBAPI.asResource("test/chilled_mixing"), chilled)).value();
        for (int temperature : new int[]{-4, -3, -1, 0, 1, 2, 3, 4}) {
            helper.assertTrue(ambientAdapter.getTemperatureRecipeData().test(temperature), "Unheated mixing rejected a temperature");
            helper.assertTrue(heatedAdapter.getTemperatureRecipeData().test(temperature) == temperature >= 1, "Heated mixing threshold changed");
            helper.assertTrue(superheatedAdapter.getTemperatureRecipeData().test(temperature) == temperature >= 3, "Superheated mixing threshold changed");
            helper.assertTrue(chilledAdapter.getTemperatureRecipeData().test(temperature) == temperature <= -1, "Chilled mixing threshold changed");
        }
        helper.assertValueEqual(ambientAdapter.getProcessingDuration(), 0, "adapted mixing duration");
        helper.assertValueEqual(ambient.getProcessingDuration(), 400, "unchanged source duration");
        PressingRecipe pressing = new Builder<>(PressingRecipe::new, CCBAPI.asResource("test/pressing")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
        helper.assertTrue(!ReactorKettleMixingRecipe.isSupported(new RecipeHolder<>(CCBAPI.asResource("test/pressing"), pressing)), "Pressing was treated as mixing");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void mixingPreservesFluidsComponentsRemaindersAndProbability(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            ItemStack result = new ItemStack(Items.GOLD_INGOT, 2);
            result.set(DataComponents.CUSTOM_NAME, Component.literal("Named mixing result"));
            FluidStack fluidResult = new FluidStack(Fluids.LAVA, 250);
            fluidResult.set(DataComponents.CUSTOM_NAME, Component.literal("Named fluid result"));
            MixingRecipe source = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/mixing_resources")).require(Items.MILK_BUCKET).require(Fluids.WATER, 500).output(result).output(0.5F, Items.DIAMOND).output(fluidResult).duration(500).build();
            int[] rolls = {0};
            source.getRollableResults().set(1, new ProcessingOutput(new ItemStack(Items.DIAMOND), 0.5F) {
                @Override
                public ItemStack rollOutput(RandomSource random) {
                    rolls[0]++;
                    return ItemStack.EMPTY;
                }
            });
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(source))) {
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.MILK_BUCKET));
                kettle.getInputFluidTank().getCapability().fill(new FluidStack(Fluids.WATER, 750), FluidAction.EXECUTE);
                ReactorKettleRecipe recipe = AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a matching item and fluid mixing recipe."));
                helper.assertValueEqual(recipe.getRollableResults().get(1).getChance(), 0.5F, "adapted output chance");
                helper.assertTrue(new ReactorKettleCraftPlanner(kettle, recipe).matches(), "Repeated mixing preview failed");
                helper.assertValueEqual(rolls[0], 0, "rolls during matching");
                helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Mixing craft failed");
                helper.assertValueEqual(rolls[0], 1, "rolls during execution");
                helper.assertTrue(ItemStack.matches(kettle.getOutputInventory().getStackInSlot(0), result), "Item output lost its count or components");
                helper.assertTrue(kettle.getOutputInventory().getStackInSlot(1).is(Items.BUCKET), "Container remainder was lost");
                helper.assertTrue(kettle.getOutputInventory().getStackInSlot(2).isEmpty(), "Empty probability roll produced an item");
                helper.assertValueEqual(kettle.getInputFluidTank().getCapability().getFluidInTank(0).getAmount(), 250, "remaining input fluid");
                FluidStack output = kettle.getOutputFluidTank().getCapability().getFluidInTank(0);
                helper.assertTrue(FluidStack.isSameFluidSameComponents(output, fluidResult), "Fluid output lost its components");
                helper.assertValueEqual(output.getAmount(), 250, "output fluid amount");
                helper.assertValueEqual(source.getFluidResults().getFirst().getAmount(), 250, "source fluid after execution");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void mixingHonorsFiltersCapacityAndColdPriority(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            MixingRecipe ordinary = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/mixing_ordinary")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            ChilledMixingRecipe chilled = new Builder<>(ChilledMixingRecipe::new, CCBAPI.asResource("test/mixing_cold")).require(Items.IRON_INGOT).output(Items.DIAMOND).build();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(ordinary, chilled))) {
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
                temperature[0] = -3;
                ReactorKettleRecipe match = AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a chilled mixing recipe."));
                helper.assertTrue(match.getResultItem(helper.getLevel().registryAccess()).is(Items.DIAMOND), "Cold mixing lost priority to unheated mixing");
                kettle.setRecipeFilter(new ItemStack(Items.GOLD_INGOT));
                match = AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a filtered mixing recipe."));
                helper.assertTrue(match.getResultItem(helper.getLevel().registryAccess()).is(Items.GOLD_INGOT), "Mixing ignored the primary output filter");
                for (int slot = 0; slot < kettle.getOutputInventory().getSlots(); slot++) {
                    kettle.getOutputInventory().setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
                }
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).isEmpty(), "Mixing accepted blocked item output");
                helper.assertTrue(!AirtightReactorKettleCrafting.applyRecipe(kettle, match), "Mixing committed into blocked output");
                helper.assertValueEqual(kettle.getInputInventory().getStackInSlot(0).getCount(), 2, "input after blocked output");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void disablingAndReloadingCancelUncommittedMixing(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            MixingRecipe source = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/mixing_reload")).require(Items.IRON_INGOT).output(Items.GOLD_INGOT).build();
            ConfigBool enabled = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticMixingRecipes;
            boolean previous = enabled.get();
            try (RecipeIndexTestScope scope = new RecipeIndexTestScope(helper.getLevel(), List.of(source))) {
                enabled.set(true);
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
                AirtightReactorKettleController controller = new AirtightReactorKettleController(kettle, new AirtightReactorKettleAnimationState(kettle));
                controller.updateReactorKettle();
                helper.assertTrue(controller.isOperating(), "Mixing did not start");
                enabled.set(false);
                controller.tick();
                helper.assertTrue(!controller.isOperating(), "Disabling mixing did not stop the pending cycle");
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).isEmpty(), "Disabled mixing remained selectable");
                enabled.set(true);
                controller.lazyTick();
                controller.updateReactorKettle();
                helper.assertTrue(controller.isOperating(), "Reenabled mixing did not restart");
                for (int tick = 0; tick < 22; tick++) {
                    controller.tick();
                }
                helper.assertValueEqual(kettle.getInputInventory().getStackInSlot(0).getCount(), 3, "input after one committed mixing craft");
                scope.reload(List.of());
                controller.tick();
                helper.assertTrue(!controller.isOperating(), "Recipe removal did not cancel continuous mixing");
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).isEmpty(), "Reload retained a removed mixing recipe");
                helper.assertValueEqual(kettle.getOutputInventory().getStackInSlot(0).getCount(), 1, "committed output retained after reload");
                MixingRecipe replacement = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/mixing_replacement")).require(Items.IRON_INGOT).output(Items.DIAMOND).build();
                scope.reload(List.of(replacement));
                ReactorKettleRecipe match = AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a reloaded mixing recipe."));
                helper.assertTrue(match.getResultItem(helper.getLevel().registryAccess()).is(Items.DIAMOND), "Reload reused the old conversion");
            }
            finally {
                enabled.set(previous);
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void mixingIndexFailureFallsBackUntilReload(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            TrackingIngredient broken = new TrackingIngredient(Items.IRON_INGOT, true);
            MixingRecipe source = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/mixing_broken_index")).require(broken.toVanilla()).output(Items.GOLD_INGOT).build();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(source))) {
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                broken.expectFailure(helper, () -> helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).isPresent(), "Mixing index failure did not fall back"));
                int expansions = broken.expansions();
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).isPresent(), "Mixing fallback stopped working");
                helper.assertValueEqual(broken.expansions(), expansions, "failed mixing index rebuilt without reload");
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void registeredCreateBrassMixingRunsInTheKettle(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            List<RecipeHolder<MixingRecipe>> recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(AllRecipeTypes.MIXING.getType());
            RecipeHolder<MixingRecipe> source = recipes.stream().filter(holder -> "create:mixing/brass_ingot".equals(holder.id().toString())).findFirst().orElseThrow(() -> new NoSuchElementException("Expected the registered Create brass mixing recipe."));
            ReactorKettleRecipe recipe = ReactorKettleMixingRecipe.convert(source).value();
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT));
            kettle.getInputInventory().setStackInSlot(1, new ItemStack(AllItems.ZINC_INGOT.asItem()));
            helper.assertTrue(!new ReactorKettleCraftPlanner(kettle, recipe).matches(), "Brass mixing accepted ambient temperature");
            temperature[0] = 3;
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Registered brass mixing failed at superheated temperature");
            helper.assertTrue(ItemStack.matches(kettle.getOutputInventory().getStackInSlot(0), AllItems.BRASS_INGOT.asStack(2)), "Registered brass output changed");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void nativeRecipesReturnEachConsumedContainer(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            ReactorKettleRecipe recipe = new ReactorKettleRecipe.Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/native_containers")).require(Items.MILK_BUCKET).require(Items.MILK_BUCKET).output(Items.GOLD_INGOT).build();
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.MILK_BUCKET));
            kettle.getInputInventory().setStackInSlot(1, new ItemStack(Items.MILK_BUCKET));
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Native recipe with two containers failed");
            helper.assertTrue(ItemStack.matches(kettle.getOutputInventory().getStackInSlot(1), new ItemStack(Items.BUCKET, 2)), "Native recipe did not return each consumed container");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void fluidOnlyMixingHonorsFluidFiltersAndBlockedOutput(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            MixingRecipe source = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/fluid_only_mixing")).require(Fluids.WATER, 500).output(Fluids.LAVA, 250).build();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(source))) {
                kettle.getInputFluidTank().getCapability().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
                kettle.setRecipeFilter(new ItemStack(Items.WATER_BUCKET));
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).isEmpty(), "Mixing ignored the fluid output filter");
                kettle.setRecipeFilter(new ItemStack(Items.LAVA_BUCKET));
                ReactorKettleRecipe recipe = AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected a fluid-only mixing recipe."));
                helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Fluid-only mixing failed");
                SmartFluidTankBehaviour outputTank = kettle.getOutputFluidTank();
                outputTank.allowInsertion();
                outputTank.getCapability().fill(new FluidStack(Fluids.LAVA, AirtightReactorKettleBlockEntity.getFluidCapacity()), FluidAction.EXECUTE);
                outputTank.getCapability().fill(new FluidStack(Fluids.WATER, AirtightReactorKettleBlockEntity.getFluidCapacity()), FluidAction.EXECUTE);
                outputTank.forbidInsertion();
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingMixingRecipe(kettle).isEmpty(), "Mixing accepted full fluid output");
                helper.assertTrue(!AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Mixing consumed fluid with full output");
                helper.assertValueEqual(kettle.getInputFluidTank().getCapability().getFluidInTank(0).getAmount(), 500, "fluid retained with blocked output");
            }
        });
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
            BlockPos cogPos = pos.above();
            AirtightReactorKettleStructuralCogBlockEntity cog = helper.getBlockEntity(cogPos);
            cog.setSpeed(SpeedLevel.FAST.getSpeedValue());
            kettle.getCore().getStructureManager().tick();
            test.accept(kettle, temperature);
            helper.succeed();
        });
    }
}
