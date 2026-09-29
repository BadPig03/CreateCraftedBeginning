package net.ty.createcraftedbeginning.gametests.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
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
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasOrderChanges;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasOrderChanges.Change;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasOrderChangesGameTests {
    private GasOrderChangesGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void orderCreationRespectsStockAndKeepsItemComponents(GameTestHelper helper) {
        List<BigItemStack> orders = new ArrayList<>();
        ItemStack item = new ItemStack(Items.IRON_INGOT, 20);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("request"));
        helper.assertTrue(GasOrderChanges.apply(orders, null, item, 7, false, 10, false) == Change.ADDED, "New order feedback");
        BigItemStack order = orders.getFirst();
        helper.assertValueEqual(order.count, 7, "Order capped by stock");
        helper.assertValueEqual(order.stack.getCount(), 1, "Order display item count");
        helper.assertTrue(ItemStack.isSameItemSameComponents(item, order.stack), "Order lost item components");
        helper.assertValueEqual(item.getCount(), 20, "Source item count");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void orderLimitOnlyBlocksNewEntries(GameTestHelper helper) {
        List<BigItemStack> orders = new ArrayList<>();
        ItemStack item = new ItemStack(Items.IRON_INGOT);
        for (int index = 0; index < 9; index++) {
            orders.add(new BigItemStack(item.copy(), 10));
        }
        helper.assertTrue(GasOrderChanges.apply(orders, null, item, 100, false, 20, false) == Change.NONE, "Tenth order was added");
        BigItemStack existing = orders.getFirst();
        helper.assertTrue(GasOrderChanges.apply(orders, existing, item, 100, false, 20, false) == Change.UPDATED, "Existing order was blocked by entry limit");
        helper.assertValueEqual(existing.count, 30, "Updated existing order");
        helper.assertValueEqual(orders.size(), 9, "Order entry count");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void modifiersAndReducedStockKeepOriginalQuantityRules(GameTestHelper helper) {
        ItemStack item = new ItemStack(Items.IRON_INGOT);
        BigItemStack order = new BigItemStack(item, 1);
        List<BigItemStack> orders = new ArrayList<>(List.of(order));
        GasOrderChanges.apply(orders, order, item, 100, false, 10, false);
        helper.assertValueEqual(order.count, 10, "First increment without control");
        order.count = 1;
        GasOrderChanges.apply(orders, order, item, 100, false, 10, true);
        helper.assertValueEqual(order.count, 11, "First increment with control");
        order.count = 90;
        GasOrderChanges.apply(orders, order, item, 100, false, 20, false);
        helper.assertValueEqual(order.count, 100, "Increment capped at current stock");
        GasOrderChanges.apply(orders, order, item, 50, false, 20, false);
        helper.assertValueEqual(order.count, 100, "Reduced stock must not silently shrink an existing order");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void removalAndIgnoredActionsKeepOrderListConsistent(GameTestHelper helper) {
        ItemStack item = new ItemStack(Items.IRON_INGOT);
        BigItemStack order = new BigItemStack(item, 10);
        List<BigItemStack> orders = new ArrayList<>(List.of(order));
        helper.assertTrue(GasOrderChanges.apply(orders, order, item, 100, false, 0, false) == Change.NONE, "Zero transfer changed order");
        helper.assertTrue(GasOrderChanges.apply(orders, null, item, 100, true, 5, false) == Change.NONE, "Removal created an order");
        helper.assertTrue(GasOrderChanges.apply(orders, null, item, 0, false, 5, false) == Change.NONE, "Empty stock created an order");
        helper.assertTrue(GasOrderChanges.apply(orders, order, item, 100, true, 4, false) == Change.UPDATED, "Partial removal feedback");
        helper.assertValueEqual(order.count, 6, "Partial removal amount");
        helper.assertTrue(GasOrderChanges.apply(orders, order, item, 100, true, 10, false) == Change.REMOVED, "Full removal feedback");
        helper.assertTrue(orders.isEmpty(), "Removed order remained in list");
        helper.succeed();
    }
}
