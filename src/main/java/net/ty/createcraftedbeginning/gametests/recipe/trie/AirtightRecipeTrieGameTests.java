package net.ty.createcraftedbeginning.gametests.recipe.trie;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipeLookup;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.GasIngredient;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.GasIngredientType;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.SizedGasIngredient;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe.Serializer;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractFluid;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractGas;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractItem;
import net.ty.createcraftedbeginning.recipe.trie.AirtightRecipeTrie;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBGasTags;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightRecipeTrieGameTests {
    private AirtightRecipeTrieGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void customFluidNeedNotDisplayEveryAcceptedFluid(GameTestHelper helper) {
        FluidIngredient ingredient = new PartialFluidIngredient();
        ReactorKettleRecipe recipe = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/partial_fluid")).require(new SizedFluidIngredient(ingredient, 100)).output(Items.GOLD_INGOT).build();
        AirtightRecipeTrie.Builder<ReactorKettleRecipe> builder = AirtightRecipeTrie.builder();
        builder.insert(recipe);

        helper.assertTrue(ingredient.test(new FluidStack(Fluids.LAVA, 100)), "Fixture must accept the undisplayed fluid");
        helper.assertTrue(builder.build().lookup(new HashSet<>(Set.of(new AbstractFluid(Fluids.LAVA)))).contains(recipe), "Index omitted a recipe whose custom fluid predicate accepts the input");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void customGasNeedNotDisplayEveryAcceptedGas(GameTestHelper helper) {
        GasIngredient ingredient = new PartialGasIngredient();
        ReactorKettleRecipe recipe = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/partial_gas")).require(new SizedGasIngredient(ingredient, 100)).output(Items.GOLD_INGOT).build();
        AirtightRecipeTrie.Builder<ReactorKettleRecipe> builder = AirtightRecipeTrie.builder();
        builder.insert(recipe);
        GasStack input = new GasStack(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100);

        helper.assertTrue(ingredient.test(input), "Fixture must accept the undisplayed gas");
        helper.assertTrue(builder.build().lookup(new HashSet<>(Set.of(new AbstractGas(input.getGasType())))).contains(recipe), "Index omitted a recipe whose custom gas predicate accepts the input");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void customItemNeedNotDisplayEveryAcceptedItem(GameTestHelper helper) {
        Ingredient ingredient = new PartialItemIngredient().toVanilla();
        ReactorKettleRecipe recipe = itemRecipe("partial_item", ingredient);
        AirtightRecipeTrie.Builder<ReactorKettleRecipe> builder = AirtightRecipeTrie.builder();
        builder.insert(recipe);

        helper.assertTrue(ingredient.test(new ItemStack(Items.GOLD_INGOT)), "Fixture must accept the undisplayed item");
        helper.assertTrue(builder.build().lookup(new HashSet<>(Set.of(new AbstractItem(Items.GOLD_INGOT)))).contains(recipe), "Index omitted a custom item recipe");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void candidateOrderMatchesLinearRecipeOrder(GameTestHelper helper) {
        List<Ingredient> ingredients = List.of(Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.COPPER_INGOT), Ingredient.of(Items.IRON_INGOT, Items.COPPER_INGOT), Ingredient.of(Items.GOLD_INGOT), Ingredient.of(Items.IRON_INGOT, Items.GOLD_INGOT));
        List<ReactorKettleRecipe> recipes = new ArrayList<>();
        AirtightRecipeTrie.Builder<ReactorKettleRecipe> builder = AirtightRecipeTrie.builder();
        for (int index = 0; index < ingredients.size(); index++) {
            ReactorKettleRecipe recipe = itemRecipe("order_" + index, ingredients.get(index));
            recipes.add(recipe);
            builder.insert(recipe);
        }
        List<ReactorKettleRecipe> candidates = builder.build().lookup(new HashSet<>(Set.of(new AbstractItem(Items.IRON_INGOT), new AbstractItem(Items.COPPER_INGOT), new AbstractItem(Items.GOLD_INGOT))));

        helper.assertTrue(candidates.equals(recipes), "Trie traversal changed the selection order among eligible recipes");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void lookupAcceptsImmutableInputAndKeepsUnknownVariants(GameTestHelper helper) {
        ReactorKettleRecipe recipe = itemRecipe("immutable_input", Ingredient.of(Items.IRON_INGOT));
        AirtightRecipeTrie.Builder<ReactorKettleRecipe> builder = AirtightRecipeTrie.builder();
        builder.insert(recipe);
        AirtightRecipeTrie<ReactorKettleRecipe> trie = builder.build();
        Set<AbstractVariant> variants = Set.of(new AbstractItem(Items.IRON_INGOT), new AbstractItem(Items.DIRT));

        helper.assertTrue(trie.lookup(variants).contains(recipe), "Immutable query did not return the matching recipe");
        Set<AbstractVariant> mutable = new HashSet<>(variants);
        trie.lookup(mutable);
        helper.assertTrue(mutable.equals(variants), "Query removed the caller's unknown variants");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void tagsComponentsAndRepeatedIngredientsStayInCandidates(GameTestHelper helper) {
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("indexed"));
        Ingredient component = DataComponentIngredient.of(false, namedIron);
        ReactorKettleRecipe recipe = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/tags_components")).require(component).require(Items.IRON_INGOT).require(Ingredient.of(ItemTags.LOGS)).require(new SizedFluidIngredient(FluidIngredient.tag(FluidTags.WATER), 100)).require(CCBGasTags.NATURAL.tag, 100).output(Items.GOLD_INGOT).build();
        AirtightRecipeTrie.Builder<ReactorKettleRecipe> builder = AirtightRecipeTrie.builder();
        builder.insert(recipe);
        Set<AbstractVariant> variants = new HashSet<>(Set.of(new AbstractItem(Items.IRON_INGOT), new AbstractItem(Items.OAK_LOG), new AbstractFluid(Fluids.WATER), new AbstractGas(CCBGases.NATURAL_AIR.get())));

        helper.assertTrue(component.test(namedIron) && !component.test(new ItemStack(Items.IRON_INGOT)), "Component predicate fixture is invalid");
        helper.assertTrue(builder.build().lookup(variants).contains(recipe), "Tags, component predicates or repeated ingredients caused a false negative");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void mixedInputsMatchLinearPredicates(GameTestHelper helper) {
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT, 64);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("indexed"));
        List<ReactorKettleRecipe> recipes = List.of(
                itemRecipe("matrix_iron", Ingredient.of(Items.IRON_INGOT)),
                itemRecipe("matrix_overlap", Ingredient.of(Items.IRON_INGOT, Items.COPPER_INGOT)),
                itemRecipe("matrix_custom_item", new PartialItemIngredient().toVanilla()),
                itemRecipe("matrix_component", DataComponentIngredient.of(false, namedIron)),
                new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/matrix_water")).require(Fluids.WATER, 100).output(Items.DIAMOND).build(),
                new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/matrix_custom_fluid")).require(new SizedFluidIngredient(new PartialFluidIngredient(), 100)).output(Items.DIAMOND).build(),
                new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/matrix_gas")).require(CCBGases.NATURAL_AIR.get(), 100).output(Items.DIAMOND).build(),
                new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/matrix_custom_gas")).require(new SizedGasIngredient(new PartialGasIngredient(), 100)).output(Items.DIAMOND).build(),
                new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/matrix_mixed")).require(Items.IRON_INGOT).require(Items.IRON_INGOT).require(new SizedFluidIngredient(new PartialFluidIngredient(), 100)).require(new SizedGasIngredient(new PartialGasIngredient(), 100)).output(Items.DIAMOND).build());
        AirtightRecipeTrie.Builder<ReactorKettleRecipe> builder = AirtightRecipeTrie.builder();
        recipes.forEach(builder::insert);
        AirtightRecipeTrie<ReactorKettleRecipe> trie = builder.build();
        List<ItemStack> itemChoices = List.of(new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.COPPER_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64));
        List<FluidStack> fluidChoices = List.of(new FluidStack(Fluids.WATER, 1000), new FluidStack(Fluids.LAVA, 1000));
        List<GasStack> gasChoices = List.of(new GasStack(CCBGases.NATURAL_AIR.get(), 1000), new GasStack(CCBGases.ENERGIZED_NATURAL_AIR.get(), 1000));
        for (int mask = 0; mask < 256; mask++) {
            List<ItemStack> items = new ArrayList<>();
            List<FluidStack> fluids = new ArrayList<>();
            List<GasStack> gases = new ArrayList<>();
            Set<AbstractVariant> variants = new HashSet<>();
            for (int index = 0; index < itemChoices.size(); index++) {
                if ((mask & 1 << index) == 0) {
                    continue;
                }

                ItemStack item = index == 0 && (mask & 128) != 0 ? namedIron : itemChoices.get(index);
                items.add(item);
                variants.add(new AbstractItem(item.getItem()));
            }
            for (int index = 0; index < fluidChoices.size(); index++) {
                if ((mask & 1 << index + 3) == 0) {
                    continue;
                }

                FluidStack fluid = fluidChoices.get(index);
                fluids.add(fluid);
                variants.add(new AbstractFluid(fluid.getFluid()));
            }
            for (int index = 0; index < gasChoices.size(); index++) {
                if ((mask & 1 << index + 5) == 0) {
                    continue;
                }

                GasStack gas = gasChoices.get(index);
                gases.add(gas);
                variants.add(new AbstractGas(gas.getGasType()));
            }
            Inputs input = new Inputs(items, fluids, gases);
            List<ReactorKettleRecipe> expected = recipes.stream().filter(input::matches).toList();
            List<ReactorKettleRecipe> actual = trie.lookup(variants).stream().filter(input::matches).toList();
            helper.assertTrue(actual.equals(expected), "Candidate completeness or order differed for input mask " + mask);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void injectionQueriesAcceptUndisplayedMaterials(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Serializer<GasInjectionRecipe> serializer = CCBRecipeTypes.GAS_INJECTION.getSerializer();
        SizedGasIngredient gasIngredient = new SizedGasIngredient(new PartialGasIngredient(), 100);
        GasInjectionRecipe itemRecipe = new StandardGasProcessingRecipe.Builder<>(serializer.factory(), CCBAPI.asResource("test/injection_item")).require(new PartialItemIngredient().toVanilla()).require(gasIngredient).output(Items.DIAMOND).build();
        GasInjectionRecipe fluidRecipe = new StandardGasProcessingRecipe.Builder<>(serializer.factory(), CCBAPI.asResource("test/injection_fluid")).require(new SizedFluidIngredient(new PartialFluidIngredient(), 100)).require(gasIngredient).output(Fluids.WATER, 100).build();
        GasTank gas = new GasTank(1000);
        gas.tryReplaceContents(new GasStack(CCBGases.ENERGIZED_NATURAL_AIR.get(), 1000));
        GasInjectionRecipeLookup lookup = new GasInjectionRecipeLookup(level, gas);
        ItemStackHandler items = new ItemStackHandler(1);
        items.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        FluidTank fluids = new FluidTank(1000);
        fluids.setFluid(new FluidStack(Fluids.LAVA, 1000));
        try (RecipeIndexTestScope scope = new RecipeIndexTestScope(level, List.of(itemRecipe))) {
            helper.assertTrue(lookup.findRecipeMatch(items.getStackInSlot(0)).orElseThrow(() -> new NoSuchElementException("Expected a matching belt recipe in test 'injectionQueriesAcceptUndisplayedMaterials'.")).recipe() == itemRecipe, "Belt query omitted custom item or gas");
            helper.assertTrue(lookup.findBasinRecipeMatch(items, fluids).orElseThrow(() -> new NoSuchElementException("Expected a matching basin recipe in test 'injectionQueriesAcceptUndisplayedMaterials'.")).recipe() == itemRecipe, "Basin query omitted custom item or gas");
            scope.reload(List.of(fluidRecipe));
            helper.assertTrue(lookup.findFluidRecipeMatch(fluids).orElseThrow(() -> new NoSuchElementException("Expected a matching fluid recipe in test 'injectionQueriesAcceptUndisplayedMaterials'.")).recipe() == fluidRecipe, "Fluid query omitted custom fluid or gas after reload");
            helper.assertTrue(lookup.findBasinRecipeMatch(items, fluids).orElseThrow(() -> new NoSuchElementException("Expected a matching basin recipe in test 'injectionQueriesAcceptUndisplayedMaterials'.")).recipe() == fluidRecipe, "Basin query omitted custom fluid or gas after reload");
            helper.assertValueEqual(gas.getGasStack().getAmount(), 1000L, "Gas after queries");
            helper.assertValueEqual(fluids.getFluidAmount(), 1000, "Fluid after queries");
            helper.assertValueEqual(items.getStackInSlot(0).getCount(), 1, "Items after queries");
        }
        helper.succeed();
    }

    private static ReactorKettleRecipe itemRecipe(String name, Ingredient ingredient) {
        return new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/" + name)).require(ingredient).output(Items.DIAMOND).build();
    }

    private record Inputs(List<ItemStack> items, List<FluidStack> fluids, List<GasStack> gases) {
        private boolean matches(ReactorKettleRecipe recipe) {
            return recipe.getIngredients().stream().allMatch(ingredient -> ingredient.isEmpty() || items.stream().anyMatch(ingredient))
                    && recipe.getFluidIngredients().stream().allMatch(ingredient -> fluids.stream().anyMatch(ingredient.ingredient()))
                    && recipe.getGasRequirements().stream().allMatch(requirement -> gases.stream().anyMatch(requirement.sizedIngredient().ingredient()));
        }
    }

    private static final class PartialItemIngredient implements ICustomIngredient {
        @Override
        public boolean test(ItemStack stack) {
            return stack.is(Items.IRON_INGOT) || stack.is(Items.GOLD_INGOT);
        }

        @Override
        public Stream<ItemStack> getItems() {
            return Stream.of(new ItemStack(Items.IRON_INGOT));
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public IngredientType<?> getType() {
            throw new UnsupportedOperationException("Test predicate is not serialized.");
        }
    }

    private static final class PartialFluidIngredient extends FluidIngredient {
        @Override
        public boolean test(FluidStack stack) {
            return stack.is(Fluids.WATER) || stack.is(Fluids.LAVA);
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public FluidIngredientType<?> getType() {
            throw new UnsupportedOperationException("Test predicate is not serialized.");
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        protected Stream<FluidStack> generateStacks() {
            return Stream.of(new FluidStack(Fluids.WATER, 100));
        }
    }

    private static final class PartialGasIngredient extends GasIngredient {
        @Override
        public boolean test(GasStack stack) {
            return stack.getGasType() == CCBGases.NATURAL_AIR.get() || stack.getGasType() == CCBGases.ENERGIZED_NATURAL_AIR.get();
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public GasIngredientType<?> getType() {
            throw new UnsupportedOperationException("Test predicate is not serialized.");
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        protected Stream<GasStack> generateStacks() {
            return Stream.of(new GasStack(CCBGases.NATURAL_AIR.get(), 100));
        }
    }
}
