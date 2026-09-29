package net.ty.createcraftedbeginning.gametests.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasCraftableBigItemStack;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasCraftingOrders;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasCraftingOrders.TransferResult;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe.Builder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasCraftingOrdersGameTests {
    private GasCraftingOrdersGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void previewDoesNotChangeOrdersOrReadCommitStock(GameTestHelper helper) {
        Fixture fixture = new Fixture();
        TransferResult result = fixture.book.transfer(fixture.recipe, fixture.output, 4, fixture.inputs, fixture.stock, true, false, () -> {
            throw new AssertionError("Preview requested commit-time stock.");
        });
        helper.assertTrue(result == TransferResult.SUCCESS, "Available preview was rejected");
        helper.assertTrue(fixture.orders.isEmpty() && fixture.recipes.isEmpty(), "Preview modified order lists");
        helper.assertValueEqual(fixture.inputs.getFirst().count, 3, "Input requirement after preview");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void maximumTransferRespectsReservationsAndExistingEntry(GameTestHelper helper) {
        Fixture fixture = new Fixture();
        fixture.orders.add(new BigItemStack(new ItemStack(Items.IRON_INGOT), 2));
        helper.assertTrue(fixture.book.transfer(fixture.recipe, fixture.output, 4, fixture.inputs, fixture.stock, true, true, () -> fixture.stock) == TransferResult.SUCCESS, "Maximum transfer failed");
        GasCraftableBigItemStack entry = (GasCraftableBigItemStack) fixture.recipes.getFirst();
        helper.assertValueEqual(entry.count, 24, "Output from six available sets");
        helper.assertValueEqual(fixture.orders.getFirst().count, 20, "Reserved input total");
        helper.assertTrue(fixture.book.remove(entry, 2), "Removing sets failed");
        helper.assertValueEqual(fixture.orders.getFirst().count, 14, "Inputs after removing two sets");
        helper.assertTrue(fixture.book.transfer(fixture.recipe, fixture.output, 4, fixture.inputs, fixture.stock, false, true, () -> fixture.stock) == TransferResult.SUCCESS, "Existing recipe could not be incremented");
        helper.assertValueEqual(fixture.recipes.size(), 1, "Recipe entries after increment");
        helper.assertValueEqual(entry.count, 20, "Output after increment");
        helper.assertValueEqual(fixture.orders.getFirst().count, 17, "Inputs after increment");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void changedStockRejectsCommitWithoutLeavingAnEntry(GameTestHelper helper) {
        Fixture fixture = new Fixture();
        helper.assertTrue(fixture.book.transfer(fixture.recipe, fixture.output, 4, fixture.inputs, fixture.stock, true, true, InventorySummary::new) == TransferResult.UNAVAILABLE, "Commit ignored stock change");
        helper.assertTrue(fixture.orders.isEmpty() && fixture.recipes.isEmpty(), "Failed commit left an order");
        helper.assertTrue(fixture.book.transfer(fixture.recipe, fixture.output, 4, fixture.inputs, fixture.stock, false, true, () -> null) == TransferResult.UNAVAILABLE, "Missing commit snapshot was accepted");
        helper.assertTrue(fixture.orders.isEmpty() && fixture.recipes.isEmpty(), "Missing snapshot left an order");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void bothRecipeAndMaterialSlotLimitsRejectNewEntries(GameTestHelper helper) {
        Fixture fixture = new Fixture();
        for (int index = 0; index < 9; index++) {
            fixture.recipes.add(new CraftableBigItemStack(new ItemStack(Items.DIRT), fixture.recipe));
        }
        helper.assertTrue(fixture.book.transfer(fixture.recipe, fixture.output, 4, fixture.inputs, fixture.stock, false, false, () -> fixture.stock) == TransferResult.FULL, "Recipe slot limit was bypassed");
        fixture.recipes.clear();
        for (int index = 0; index < 9; index++) {
            ItemStack item = new ItemStack(Items.IRON_INGOT);
            item.set(DataComponents.CUSTOM_NAME, Component.literal("order_" + index));
            fixture.orders.add(new BigItemStack(item, 1));
        }
        helper.assertTrue(fixture.book.transfer(fixture.recipe, fixture.output, 4, fixture.inputs, fixture.stock, false, true, () -> fixture.stock) == TransferResult.FULL, "Material slot limit was bypassed");
        helper.assertValueEqual(fixture.orders.size(), 9, "Existing material entries");
        helper.assertTrue(fixture.recipes.isEmpty(), "Full order list left a recipe entry");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void existingRecipePreviewChecksItsActualRequirements(GameTestHelper helper) {
        Fixture fixture = new Fixture();
        GasCraftableBigItemStack existing = new GasCraftableBigItemStack(fixture.output, fixture.recipe, 4, List.of(new BigItemStack(new ItemStack(Items.GOLD_INGOT), 5)));
        existing.count = 4;
        fixture.recipes.add(existing);
        helper.assertTrue(fixture.book.transfer(fixture.recipe, fixture.output, 4, fixture.inputs, fixture.stock, false, false, () -> fixture.stock) == TransferResult.UNAVAILABLE, "Preview accepted an existing recipe without its selected materials");
        helper.assertValueEqual(existing.count, 4, "Existing recipe after failed preview");
        helper.assertTrue(fixture.orders.isEmpty(), "Failed preview added materials");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void largeOutputTransferDoesNotOverflowIntoRemoval(GameTestHelper helper) {
        Fixture fixture = new Fixture();
        fixture.stock.add(new ItemStack(Items.IRON_INGOT), 10000);
        List<BigItemStack> inputs = List.of(new BigItemStack(new ItemStack(Items.IRON_INGOT), 1));
        helper.assertTrue(fixture.book.transfer(fixture.recipe, fixture.output, 1000000, inputs, fixture.stock, true, true, () -> fixture.stock) == TransferResult.SUCCESS, "Large output transfer overflowed");
        helper.assertValueEqual(fixture.recipes.getFirst().count, BigItemStack.INF, "Saturated recipe output");
        helper.assertValueEqual(fixture.orders.getFirst().count, 10020, "Input reservation after large output transfer");
        helper.succeed();
    }

    private static final class Fixture {
        private final List<BigItemStack> orders = new ArrayList<>();
        private final List<CraftableBigItemStack> recipes = new ArrayList<>();
        private final GasCraftingOrders book = new GasCraftingOrders(orders, recipes);
        private final ReactorKettleRecipe recipe = new Builder<>(ReactorKettleRecipe::new, CCBAPI.asResource("test/transfer")).require(Items.IRON_INGOT).output(Items.DIAMOND).build();
        private final ItemStack output = new ItemStack(Items.DIAMOND);
        private final List<BigItemStack> inputs = List.of(new BigItemStack(new ItemStack(Items.IRON_INGOT), 3));
        private final InventorySummary stock = new InventorySummary();

        private Fixture() {
            stock.add(new ItemStack(Items.IRON_INGOT), 20);
        }
    }
}
