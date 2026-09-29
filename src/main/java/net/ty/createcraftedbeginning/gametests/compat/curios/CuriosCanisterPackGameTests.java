package net.ty.createcraftedbeginning.gametests.compat.curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.curios.CuriosCompat;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@PrefixGameTestTemplate(false)
public final class CuriosCanisterPackGameTests {
    private CuriosCanisterPackGameTests() {
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_3x3")
    public static void extraSlotsDrainInIndexOrderBeforeInventory(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ICurioStacksHandler handler = slots(player, 2);
        IDynamicStackHandler stacks = handler.getStacks();
        helper.assertValueEqual(stacks.getSlots(), 3, "Two additional slots must extend the default slot");
        Gas gas = CCBGases.NATURAL_AIR.get();
        ItemStack first = pack(gas, 30);
        ItemStack second = pack(gas, 60);
        ItemStack third = pack(gas, 40);
        ItemStack backpack = pack(gas, 500);
        stacks.setStackInSlot(0, first);
        stacks.setStackInSlot(1, second);
        stacks.setStackInSlot(2, third);
        player.getInventory().setItem(0, backpack);
        List<GasCanisterContainer> suppliers = CanisterContainerSuppliers.getAllSuppliers(player);
        helper.assertTrue(suppliers.size() == 4 && suppliers.get(0).getContainer() == first && suppliers.get(1).getContainer() == second && suppliers.get(2).getContainer() == third && suppliers.get(3).getContainer() == backpack, "Equipped packs must retain ascending slot order ahead of inventory packs");
        consume(helper, player, gas, 80);
        helper.assertTrue(contents(first).isEmpty(), "The first slot must be drained first");
        helper.assertValueEqual(contents(second).getGasInTank(0).getAmount(), 10L, "The second slot must cover the remainder");
        helper.assertValueEqual(contents(third).getGasInTank(0).getAmount(), 40L, "Later slots must remain untouched until needed");
        helper.assertValueEqual(contents(backpack).getGasInTank(0).getAmount(), 500L, "Inventory must not precede equipped packs");
        consume(helper, player, gas, 200);
        helper.assertTrue(contents(second).isEmpty() && contents(third).isEmpty(), "Remaining equipped packs must drain before inventory");
        helper.assertValueEqual(contents(backpack).getGasInTank(0).getAmount(), 350L, "Inventory must supply only the unpaid remainder");
        helper.succeed();
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_3x3")
    public static void slotValidationAndFallbackPreserveGasTypes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ICurioStacksHandler handler = slots(player, 2);
        IDynamicStackHandler stacks = handler.getStacks();
        SlotContext context = new SlotContext(CuriosCompat.GAS_CANISTER_PACK_SLOT, player, 0, false, false);
        helper.assertTrue(CuriosApi.isStackValid(context, new ItemStack(CCBItems.GAS_CANISTER_PACK.asItem())), "The dedicated slot must accept a gas canister pack");
        Gas natural = CCBGases.NATURAL_AIR.get();
        Gas other = CCBGases.ULTRAWARM_AIR.get();
        ItemStack otherPack = pack(other, 70);
        ItemStack matchingPack = pack(natural, 20);
        ItemStack backpack = pack(natural, 80);
        stacks.setStackInSlot(1, otherPack);
        stacks.setStackInSlot(2, matchingPack);
        handler.getCosmeticStacks().setStackInSlot(0, pack(natural, 500));
        player.getInventory().setItem(0, backpack);
        helper.assertTrue(CanisterContainerSuppliers.getFirstAvailableGasContent(player).is(other), "The first occupied functional slot must select the displayed gas");
        helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, natural, usage -> 101).isEmpty(), "Other gases and cosmetic packs must not cover an unaffordable request");
        consume(helper, player, natural, 50);
        helper.assertValueEqual(contents(otherPack).getGasInTank(0).getAmount(), 70L, "A request must not consume another gas type");
        helper.assertTrue(contents(matchingPack).isEmpty(), "The matching equipped pack must precede inventory");
        helper.assertValueEqual(contents(backpack).getGasInTank(0).getAmount(), 50L, "Inventory must cover the remainder after matching equipped gas");
        helper.succeed();
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_3x3")
    public static void changesInvalidateCacheAndDuplicateStacksDoNotDoubleSupply(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ICurioStacksHandler handler = slots(player, 0);
        IDynamicStackHandler stacks = handler.getStacks();
        helper.assertValueEqual(stacks.getSlots(), 1, "Players must start with one dedicated slot");
        helper.assertTrue(CanisterContainerSuppliers.getAllSuppliers(player).isEmpty(), "An empty dedicated slot must not provide gas");
        Gas gas = CCBGases.NATURAL_AIR.get();
        ItemStack pack = pack(gas, 40);
        stacks.setStackInSlot(0, pack);
        NeoForge.EVENT_BUS.post(new CurioChangeEvent(player, CuriosCompat.GAS_CANISTER_PACK_SLOT, 0, ItemStack.EMPTY, pack));
        player.getInventory().setItem(0, pack);
        List<GasCanisterContainer> suppliers = CanisterContainerSuppliers.getAllSuppliers(player);
        helper.assertTrue(suppliers.size() == 1 && suppliers.getFirst().getContainer() == pack, "Equipment changes must invalidate cached suppliers and the same stack must not be counted twice");
        helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, gas, usage -> 41).isEmpty(), "Duplicate stack references must not double available gas");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        stacks.setStackInSlot(0, ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new CurioChangeEvent(player, CuriosCompat.GAS_CANISTER_PACK_SLOT, 0, pack, ItemStack.EMPTY));
        helper.assertTrue(CanisterContainerSuppliers.getAllSuppliers(player).isEmpty(), "Unequipping must remove the cached source in the same tick");
        helper.succeed();
    }

    private static ICurioStacksHandler slots(Player player, int additionalSlots) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) {
            throw new NullPointerException("Expected a Curios inventory for the test player.");
        }

        if (additionalSlots > 0) {
            Multimap<String, AttributeModifier> modifiers = HashMultimap.create();
            modifiers.put(CuriosCompat.GAS_CANISTER_PACK_SLOT, new AttributeModifier(CCBAPI.asResource("test_extra_canister_slots"), additionalSlots, Operation.ADD_VALUE));
            inventory.addTransientSlotModifiers(modifiers);
        }
        ICurioStacksHandler handler = inventory.getStacksHandler(CuriosCompat.GAS_CANISTER_PACK_SLOT).orElse(null);
        if (handler == null) {
            throw new NullPointerException("Expected the dedicated gas canister pack Curios slot.");
        }

        return handler;
    }

    private static ItemStack pack(Gas gas, long amount) {
        ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        contents(canister).fill(0, new GasStack(gas, amount), GasAction.EXECUTE);
        ItemStack pack = new ItemStack(CCBItems.GAS_CANISTER_PACK.asItem());
        pack.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(canister)));
        return pack;
    }

    private static GasCanisterContainer contents(ItemStack stack) {
        GasCanisterContainer container = stack.getCapability(CanisterCapabilities.ITEM);
        if (container == null) {
            throw new NullPointerException("Expected a gas canister capability for '" + stack.getItem() + "'.");
        }

        return container;
    }

    private static void consume(GameTestHelper helper, Player player, Gas gas, long amount) {
        AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, gas, usage -> amount).orElse(null);
        if (fuel == null) {
            throw new NullPointerException("Expected affordable gas for a request of " + amount + " GU.");
        }

        helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, true), "Equipped gas simulation must succeed");
        helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), "Equipped gas consumption must succeed");
    }
}
