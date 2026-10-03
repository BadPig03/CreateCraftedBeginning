package net.ty.createcraftedbeginning.gametests.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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
import net.ty.createcraftedbeginning.recipe.ReactorKettleBrewingRecipes;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ReactorKettleMixingRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReactorKettleBrewingGameTests {
    private ReactorKettleBrewingGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void brewingRequiresHeatAndPreservesPotionComponents(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.NETHER_WART));
            kettle.getInputFluidTank().getCapability().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
            temperature[0] = 0;
            helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).isEmpty(), "Brewing accepted ambient temperature");
            temperature[0] = -3;
            helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).isEmpty(), "Brewing accepted superchilled temperature");
            temperature[0] = 1;
            ReactorKettleRecipe recipe = AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected water and nether wart brewing."));
            helper.assertTrue(recipe instanceof ReactorKettleMixingRecipe mixing && mixing.isBrewing(), "Brewing lost its independent source marker");
            helper.assertValueEqual(recipe.getProcessingDuration(), 0, "brewing duration");
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Water brewing failed");
            FluidStack awkward = PotionFluidHandler.getFluidFromPotion(new PotionContents(Potions.AWKWARD), BottleType.REGULAR, 1000);
            FluidStack output = kettle.getOutputFluidTank().getCapability().getFluidInTank(0);
            helper.assertTrue(FluidStack.isSameFluidSameComponents(output, awkward), "Brewing changed the potion or bottle type");
            helper.assertValueEqual(output.getAmount(), 1000, "brewed fluid amount");
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.NETHER_WART));
            helper.assertTrue(!new ReactorKettleCraftPlanner(kettle, recipe).matches(), "Water brewing accepted awkward potion as water");
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.SUGAR));
            temperature[0] = 3;
            ReactorKettleRecipe swiftness = AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected awkward potion and sugar brewing."));
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, swiftness), "Chained brewing failed at superheated temperature");
            FluidStack expected = PotionFluidHandler.getFluidFromPotion(new PotionContents(Potions.SWIFTNESS), BottleType.REGULAR, 1000);
            helper.assertTrue(FluidStack.isSameFluidSameComponents(kettle.getOutputFluidTank().getCapability().getFluidInTank(0), expected), "Chained brewing produced the wrong potion");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void brewingConvertsBottleTypesAndReturnsContainers(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            FluidStack regular = PotionFluidHandler.getFluidFromPotion(new PotionContents(Potions.SWIFTNESS), BottleType.REGULAR, 1000);
            kettle.getInputFluidTank().getCapability().fill(regular, FluidAction.EXECUTE);
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.GUNPOWDER));
            ReactorKettleRecipe splash = AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected splash potion brewing."));
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, splash), "Splash brewing failed");
            FluidStack expectedSplash = PotionFluidHandler.getFluidFromPotion(new PotionContents(Potions.SWIFTNESS), BottleType.SPLASH, 1000);
            helper.assertTrue(FluidStack.isSameFluidSameComponents(kettle.getOutputFluidTank().getCapability().getFluidInTank(0), expectedSplash), "Splash brewing lost potion components");
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.GUNPOWDER));
            helper.assertTrue(!new ReactorKettleCraftPlanner(kettle, splash).matches(), "Regular potion input accepted splash potion");
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.DRAGON_BREATH));
            ReactorKettleRecipe lingering = AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected lingering potion brewing."));
            helper.assertTrue(AirtightReactorKettleCrafting.applyRecipe(kettle, lingering), "Lingering brewing failed");
            FluidStack expectedLingering = PotionFluidHandler.getFluidFromPotion(new PotionContents(Potions.SWIFTNESS), BottleType.LINGERING, 1000);
            helper.assertTrue(FluidStack.isSameFluidSameComponents(kettle.getOutputFluidTank().getCapability().getFluidInTank(0), expectedLingering), "Lingering brewing lost potion components");
            helper.assertTrue(kettle.getOutputInventory().getStackInSlot(0).is(Items.GLASS_BOTTLE), "Dragon breath did not return a glass bottle");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void brewingChecksAmountsFiltersAndOutputSpace(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.REDSTONE));
            FluidStack input = PotionFluidHandler.getFluidFromPotion(new PotionContents(Potions.SWIFTNESS), BottleType.REGULAR, 999);
            kettle.getInputFluidTank().getCapability().fill(input, FluidAction.EXECUTE);
            helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).isEmpty(), "Brewing accepted insufficient fluid");
            input.setAmount(1);
            kettle.getInputFluidTank().getCapability().fill(input, FluidAction.EXECUTE);
            kettle.setRecipeFilter(PotionContents.createItemStack(Items.POTION, Potions.POISON));
            helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).isEmpty(), "Brewing ignored potion contents in the output filter");
            kettle.setRecipeFilter(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.LONG_SWIFTNESS));
            helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).isEmpty(), "Brewing ignored bottle type in the output filter");
            kettle.setRecipeFilter(PotionContents.createItemStack(Items.POTION, Potions.LONG_SWIFTNESS));
            ReactorKettleRecipe recipe = AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).orElseThrow(() -> new NoSuchElementException("Expected filtered long swiftness brewing."));
            SmartFluidTankBehaviour output = kettle.getOutputFluidTank();
            output.allowInsertion();
            output.getCapability().fill(new FluidStack(Fluids.WATER, AirtightReactorKettleBlockEntity.getFluidCapacity()), FluidAction.EXECUTE);
            output.getCapability().fill(new FluidStack(Fluids.LAVA, AirtightReactorKettleBlockEntity.getFluidCapacity()), FluidAction.EXECUTE);
            output.forbidInsertion();
            helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).isEmpty(), "Brewing accepted blocked fluid output");
            helper.assertTrue(!AirtightReactorKettleCrafting.applyRecipe(kettle, recipe), "Brewing committed with blocked fluid output");
            helper.assertValueEqual(kettle.getInputFluidTank().getCapability().getFluidInTank(0).getAmount(), 1000, "fluid retained after blocked brewing");
            helper.assertValueEqual(kettle.getInputInventory().getStackInSlot(0).getCount(), 1, "reagent retained after blocked brewing");
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void brewingSwitchAndReloadCancelOnlyPendingBrewing(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            ConfigBool brewing = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticBrewingRecipes;
            ConfigBool mixing = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticMixingRecipes;
            boolean previousBrewing = brewing.get();
            boolean previousMixing = mixing.get();
            try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of())) {
                brewing.set(true);
                mixing.set(false);
                kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.NETHER_WART, 3));
                kettle.getInputFluidTank().getCapability().fill(new FluidStack(Fluids.WATER, 3000), FluidAction.EXECUTE);
                AirtightReactorKettleController controller = new AirtightReactorKettleController(kettle, new AirtightReactorKettleAnimationState(kettle));
                controller.updateReactorKettle();
                helper.assertTrue(controller.isOperating(), "Disabling automatic mixing also disabled brewing");
                brewing.set(false);
                controller.tick();
                helper.assertTrue(!controller.isOperating(), "Disabling brewing retained the pending cycle");
                helper.assertTrue(AirtightReactorKettleRecipeLookup.getMatchingBrewingRecipe(kettle).isEmpty(), "Disabled brewing remained selectable");
                helper.assertValueEqual(kettle.getInputInventory().getStackInSlot(0).getCount(), 3, "reagents after cancellation");
                brewing.set(true);
                controller.lazyTick();
                controller.updateReactorKettle();
                for (int tick = 0; tick < 22; tick++) {
                    controller.tick();
                }
                helper.assertValueEqual(kettle.getInputInventory().getStackInSlot(0).getCount(), 2, "reagents after zero-duration brewing");
                AirtightReactorKettleRecipeLookup.invalidateRecipeCaches();
                controller.tick();
                helper.assertTrue(!controller.isOperating(), "Reload retained an old brewing operation");
                helper.assertValueEqual(kettle.getOutputFluidTank().getCapability().getFluidInTank(0).getAmount(), 1000, "committed brewing output after reload");
                helper.assertValueEqual(kettle.getInputFluidTank().getCapability().getFluidInTank(0).getAmount(), 2000, "uncommitted brewing input after reload");
            }
            finally {
                brewing.set(previousBrewing);
                mixing.set(previousMixing);
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void nativeThenMixingThenBrewingThenShapelessTakePriority(GameTestHelper helper) {
        withKettle(helper, (kettle, temperature) -> {
            ReactorKettleRecipe nativeRecipe = new ReactorKettleRecipe.Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/brewing_native_priority")).require(Items.NETHER_WART).require(Items.SUGAR).require(Fluids.WATER, 1000).temperatureMatching(TemperatureMatching.COMPATIBLE).output(Items.EMERALD).build();
            MixingRecipe mixingRecipe = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/brewing_mixing_priority")).require(Items.NETHER_WART).require(Items.SUGAR).require(Fluids.WATER, 1000).duration(1000).output(Items.DIAMOND).build();
            ShapelessRecipe shapeless = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.GOLD_INGOT), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.NETHER_WART), Ingredient.of(Items.SUGAR)));
            ConfigBool mixing = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticMixingRecipes;
            ConfigBool brewing = CCBConfig.server().machines.airtightReactorKettle.enableAutomaticBrewingRecipes;
            boolean previousMixing = mixing.get();
            boolean previousBrewing = brewing.get();
            List<String> stages = List.of("native", "mixing", "brewing after mixing removal", "shapeless after mixing removal", "brewing with mixing disabled", "shapeless with mixing disabled");
            try (RecipeIndexTestScope scope = new RecipeIndexTestScope(helper.getLevel(), List.of())) {
                for (int order = 0; order < 2; order++) {
                    for (int stage = 0; stage < stages.size(); stage++) {
                        String scenario = stages.get(stage) + ", registration order " + order;
                        List<Recipe<?>> recipes = switch (stage) {
                            case 0 -> List.of(nativeRecipe, mixingRecipe, shapeless);
                            case 1, 4, 5 -> List.of(mixingRecipe, shapeless);
                            default -> List.of(shapeless);
                        };
                        scope.reload(order == 0 ? recipes : recipes.reversed());
                        temperature[0] = 3;
                        mixing.set(stage < 4);
                        brewing.set(stage != 3 && stage != 5);
                        SmartInventory inputInventory = kettle.getInputInventory();
                        SmartInventory outputInventory = kettle.getOutputInventory();
                        IFluidHandler inputFluids = kettle.getInputFluidTank().getCapability();
                        IFluidHandler outputFluids = kettle.getOutputFluidTank().getCapability();
                        inputInventory.setStackInSlot(0, new ItemStack(Items.NETHER_WART));
                        inputInventory.setStackInSlot(1, new ItemStack(Items.SUGAR));
                        inputFluids.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
                        AirtightReactorKettleController controller = new AirtightReactorKettleController(kettle, new AirtightReactorKettleAnimationState(kettle));
                        controller.updateReactorKettle();
                        for (int tick = 0; tick < 22; tick++) {
                            controller.tick();
                        }
                        boolean brewed = stage == 2 || stage == 4;
                        boolean crafted = stage == 3 || stage == 5;
                        FluidStack outputFluid = outputFluids.getFluidInTank(0);
                        if (brewed) {
                            FluidStack expected = PotionFluidHandler.getFluidFromPotion(new PotionContents(Potions.AWKWARD), BottleType.REGULAR, 1000);
                            helper.assertTrue(FluidStack.isSameFluidSameComponents(outputFluid, expected), "Wrong brewing output: " + scenario);
                            helper.assertValueEqual(outputFluid.getAmount(), 1000, "brewing output amount: " + scenario);
                            helper.assertTrue(outputInventory.isEmpty(), "Brewing unexpectedly produced items: " + scenario);
                            helper.assertTrue(inputInventory.getStackInSlot(0).isEmpty(), "Brewing did not consume nether wart: " + scenario);
                            helper.assertTrue(ItemStack.matches(inputInventory.getStackInSlot(1), new ItemStack(Items.SUGAR)), "Brewing consumed unrelated sugar: " + scenario);
                        }
                        else {
                            ItemStack expected = switch (stage) {
                                case 0 -> new ItemStack(Items.EMERALD);
                                case 1 -> new ItemStack(Items.DIAMOND);
                                default -> new ItemStack(Items.GOLD_INGOT);
                            };
                            helper.assertTrue(ItemStack.matches(outputInventory.getStackInSlot(0), expected), "Recipe priority or zero-duration processing changed: " + scenario);
                            helper.assertTrue(outputFluid.isEmpty(), "Item recipe unexpectedly produced fluid: " + scenario);
                            helper.assertTrue(inputInventory.isEmpty(), "Selected recipe did not consume its inputs: " + scenario);
                        }
                        helper.assertValueEqual(inputFluids.getFluidInTank(0).getAmount(), crafted ? 1000 : 0, "remaining input fluid: " + scenario);
                        inputInventory.setStackInSlot(1, ItemStack.EMPTY);
                        inputFluids.drain(1000, FluidAction.EXECUTE);
                        outputInventory.setStackInSlot(0, ItemStack.EMPTY);
                        outputFluids.drain(1000, FluidAction.EXECUTE);
                    }
                }
            }
            finally {
                mixing.set(previousMixing);
                brewing.set(previousBrewing);
            }
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void brewingCatalogIsScopedToLevelAndInvalidatedOnReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            throw new NullPointerException("Expected the Nether level for brewing cache isolation.");
        }

        List<RecipeHolder<ReactorKettleRecipe>> recipes = ReactorKettleBrewingRecipes.getRecipes(level);
        helper.assertTrue(!recipes.isEmpty(), "Create generated no brewing recipes");
        helper.assertTrue(recipes == ReactorKettleBrewingRecipes.getRecipes(level), "Brewing recipes were regenerated for each lookup");
        List<RecipeHolder<ReactorKettleRecipe>> netherRecipes = ReactorKettleBrewingRecipes.getRecipes(nether);
        helper.assertTrue(recipes != netherRecipes && recipes.getFirst().value() != netherRecipes.getFirst().value(), "Different levels shared mutable brewing recipes");
        AirtightReactorKettleRecipeLookup.invalidateRecipeCaches();
        List<RecipeHolder<ReactorKettleRecipe>> reloaded = ReactorKettleBrewingRecipes.getRecipes(level);
        helper.assertTrue(recipes != reloaded && recipes.getFirst().value() != reloaded.getFirst().value(), "Reload reused old generated brewing recipes");
        helper.assertValueEqual(reloaded.size(), recipes.size(), "recipe count after unchanged reload");
        helper.succeed();
    }

    private static void withKettle(GameTestHelper helper, BiConsumer<AirtightReactorKettleBlockEntity, float[]> test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.getDefaultState());
        BlockEntity original = helper.getBlockEntity(pos);
        float[] temperature = {3};
        AirtightReactorKettleBlockEntity kettle = new AirtightReactorKettleBlockEntity(original.getType(), helper.absolutePos(pos), original.getBlockState()) {
            @Override
            public float getRecipeTemperature() {
                return temperature[0];
            }
        };
        helper.getLevel().setBlockEntity(kettle);
        helper.runAfterDelay(2, () -> {
            AirtightReactorKettleStructuralCogBlockEntity cog = helper.getBlockEntity(pos.above());
            cog.setSpeed(SpeedLevel.FAST.getSpeedValue());
            kettle.getCore().getStructureManager().tick();
            test.accept(kettle, temperature);
            helper.succeed();
        });
    }
}
