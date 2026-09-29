package net.ty.createcraftedbeginning.gametests.content.airtights.gascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CanisterMiningResetGameTests {
    private CanisterMiningResetGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void damageableCanistersIgnoreOnlyStoredGas(GameTestHelper helper) {
        for (Item item : List.of(CCBItems.GAS_CANISTER.get(), CCBItems.CREATIVE_GAS_CANISTER.get())) {
            ItemStack before = new ItemStack(item);
            before.set(DataComponents.MAX_DAMAGE, 100);
            before.set(DataComponents.DAMAGE, 0);
            ItemStack after = before.copy();
            after.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 100));
            helper.assertTrue(!item.shouldCauseBlockBreakReset(before, after), "Stored gas alone reset damageable canister mining");
            after.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed"));
            helper.assertTrue(item.shouldCauseBlockBreakReset(before, after), "A meaningful component change did not reset mining");
            helper.assertTrue(item.shouldCauseBlockBreakReset(before, new ItemStack(Items.STICK)), "Changing the held item did not reset mining");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void damageablePacksIgnoreContentsAndFlags(GameTestHelper helper) {
        Item item = CCBItems.GAS_CANISTER_PACK.get();
        ItemStack before = new ItemStack(item);
        before.set(DataComponents.MAX_DAMAGE, 100);
        before.set(DataComponents.DAMAGE, 0);
        ItemStack after = before.copy();
        after.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, 1);
        after.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(new ItemStack(CCBItems.GAS_CANISTER.get()))));
        helper.assertTrue(!item.shouldCauseBlockBreakReset(before, after), "Pack storage changes reset damageable pack mining");
        after.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 100));
        helper.assertTrue(item.shouldCauseBlockBreakReset(before, after), "Pack inherited the individual canister exclusion");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void nonDamageableItemsStillCompareAllComponents(GameTestHelper helper) {
        for (Item item : List.of(CCBItems.GAS_CANISTER.get(), CCBItems.CREATIVE_GAS_CANISTER.get(), CCBItems.GAS_CANISTER_PACK.get())) {
            ItemStack before = new ItemStack(item);
            ItemStack after = before.copy();
            helper.assertTrue(!before.isDamageableItem(), "Unexpected default durability");
            helper.assertTrue(!item.shouldCauseBlockBreakReset(before, after), "Equal items reset mining");
            if (item == CCBItems.GAS_CANISTER_PACK.get()) {
                after.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, 1);
            }
            else {
                after.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 100));
            }
            helper.assertTrue(item.shouldCauseBlockBreakReset(before, after), "Non-damageable component comparison changed");
        }
        helper.succeed();
    }
}
